package com.kakarote.admin.oceanengine.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.entity.WkQcAppAuth;
import com.kakarote.admin.oceanengine.entity.WkQcAppToken;
import com.kakarote.admin.oceanengine.entity.WkQcChannel;
import com.kakarote.admin.oceanengine.entity.WkQcIntegrationApp;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.mapper.WkQcAppAuthMapper;
import com.kakarote.admin.oceanengine.mapper.WkQcAppTokenMapper;
import com.kakarote.admin.oceanengine.mapper.WkQcChannelMapper;
import com.kakarote.admin.oceanengine.mapper.WkQcIntegrationAppMapper;
import com.kakarote.admin.oceanengine.service.OceanTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OceanTokenServiceImpl implements OceanTokenService {

    private static final Logger log = LoggerFactory.getLogger(OceanTokenServiceImpl.class);

    /**
     * 广告（巨量广告）刷新 token URL
     * 文档示例：https://api.oceanengine.com/open_api/oauth2/refresh_token/
     */
    private static final String ADS_REFRESH_TOKEN_URL =
            "https://api.oceanengine.com/open_api/oauth2/refresh_token/";

    /**
     * 千川刷新 token URL
     * 文档示例（页面左上切换到“巨量千川 / 全部接口”）:
     * https://ad.oceanengine.com/open_api/oauth2/refresh_token/
     */
    private static final String QIANCHUAN_REFRESH_TOKEN_URL =
            "https://ad.oceanengine.com/open_api/oauth2/refresh_token/";

    @Resource
    private WkQcAppTokenMapper appTokenMapper;

    @Resource
    private WkQcAppAuthMapper appAuthMapper;

    @Resource
    private WkQcIntegrationAppMapper integrationAppMapper;

    @Resource
    private WkQcChannelMapper channelMapper;

    @Resource
    private RestTemplate restTemplate;

    // ==================== 对外接口 ====================

    /** 提供给业务方用的：按渠道拿可用 access_token */
    @Override
    public String getAccessToken(OceanChannelCode channelCode) {
        // 当前每个 channel 只有一个 app_auth，可以简单按 channel 找默认 app_auth：
        WkQcAppAuth defaultAuth = findDefaultAppAuthByChannel(channelCode);
        if (defaultAuth == null) {
            throw new IllegalStateException("未找到 channel=" + channelCode.getCode() + " 的 app 授权记录");
        }
        return getAccessTokenByAppAuth(defaultAuth.getId());
    }

    @Override
    public String getAccessTokenByAppAuth(Long appAuthId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime safeTime = now.plusMinutes(10);

        // 1. 先找这个 app_auth 下“未过期”（带缓冲）的 token
        LambdaQueryWrapper<WkQcAppToken> validWrapper = new LambdaQueryWrapper<>();
        validWrapper.eq(WkQcAppToken::getAppAuthId, appAuthId)
                .eq(WkQcAppToken::getStatus, 1)
                .gt(WkQcAppToken::getExpiresAt, safeTime)
                .orderByAsc(WkQcAppToken::getExpiresAt)
                .last("LIMIT 1");

        WkQcAppToken token = appTokenMapper.selectOne(validWrapper);
        if (token != null) {
            log.debug("使用未过期 token, appAuthId={}, tokenId={}, expiresAt={}",
                    appAuthId, token.getId(), token.getExpiresAt());
            return token.getAccessToken();
        }

        // 2. 没有未过期 token，尝试刷新
        return refreshAccessTokenForAppAuth(appAuthId);
    }

    @Override
    public void refreshTokenByAppAuth(Long appAuthId) {
        refreshAccessTokenForAppAuth(appAuthId);
    }

    @Override
    public void refreshAllTokensForChannel(OceanChannelCode channelCode) {
        List<WkQcAppAuth> authList = appAuthMapper.selectList(
                new LambdaQueryWrapper<WkQcAppAuth>()
                        .eq(WkQcAppAuth::getChannelId, channelCode.getChannelId())
                        .eq(WkQcAppAuth::getStatus, 1)
        );
        for (WkQcAppAuth auth : authList) {
            try {
                refreshAccessTokenForAppAuth(auth.getId());
            } catch (Exception e) {
                log.error("刷新 channel={} 下 appAuthId={} 的 token 失败",
                        channelCode.getCode(), auth.getId(), e);
            }
        }
    }

    // ==================== 内部方法 ====================

    /** 找某个渠道的默认 app_auth（目前假设一个 channel 一个授权） */
    private WkQcAppAuth findDefaultAppAuthByChannel(OceanChannelCode channelCode) {
        LambdaQueryWrapper<WkQcAppAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WkQcAppAuth::getChannelId, channelCode.getChannelId())
                .eq(WkQcAppAuth::getStatus, 1)
                .orderByAsc(WkQcAppAuth::getId)
                .last("LIMIT 1");
        return appAuthMapper.selectOne(wrapper);
    }

    /** 通用的 JSON POST 调用，带日志和错误处理 */
    private JSONObject doPostJson(String url, Map<String, Object> body, Long appAuthId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            String respStr = resp.getBody();

            log.info("调用 OceanEngine 刷新 token 接口完成, appAuthId={}, url={}, httpStatus={}, resp={}",
                    appAuthId, url, resp.getStatusCodeValue(), respStr);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("调用刷新 token 接口 HTTP 状态异常, status="
                        + resp.getStatusCodeValue() + ", resp=" + respStr);
            }

            try {
                return JSON.parseObject(respStr);
            } catch (Exception e) {
                log.error("刷新 token 返回内容不是合法 JSON, appAuthId={}, resp={}",
                        appAuthId, respStr, e);
                throw new IllegalStateException("刷新失败，需要重新授权, appAuthId=" + appAuthId
                        + ", 返回内容不是 JSON");
            }
        } catch (RuntimeException e) {
            // 保留原始异常方便排查
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("调用刷新 token 接口异常, appAuthId=" + appAuthId, e);
        }
    }

    /** 广告（ADS）刷新 token */
    private JSONObject refreshAdsToken(Long appAuthId, WkQcIntegrationApp app, String refreshToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", app.getAppId());
        body.put("secret", app.getAppSecret());
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", refreshToken);
        return doPostJson(ADS_REFRESH_TOKEN_URL, body, appAuthId);
    }

    /** 千川刷新 token */
    private JSONObject refreshQianchuanToken(Long appAuthId, WkQcIntegrationApp app,
                                             WkQcAppAuth appAuth, String refreshToken) {
        if (!StringUtils.hasText(appAuth.getAuthCode())) {
            throw new IllegalStateException("千川刷新 token 需要 auth_code，请在 wk_qc_app_auth 表补上");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", app.getAppId());
        body.put("secret", app.getAppSecret());
        body.put("refresh_token", refreshToken);
        body.put("grant_type", appAuth.getAuthCode());
        return doPostJson(QIANCHUAN_REFRESH_TOKEN_URL, body, appAuthId);
    }

    /**
     * 核心刷新逻辑：按 app_auth_id 刷新 token
     */
    private String refreshAccessTokenForAppAuth(Long appAuthId) {
        // 1. 查授权记录
        WkQcAppAuth appAuth = appAuthMapper.selectById(appAuthId);
        if (appAuth == null) {
            throw new IllegalStateException("未找到授权记录, appAuthId=" + appAuthId);
        }

        // 2. 查渠道、应用配置
        WkQcIntegrationApp app = integrationAppMapper.selectById(appAuth.getIntegrationAppId());
        if (app == null) {
            throw new IllegalStateException("未找到集成应用配置, integrationAppId=" + appAuth.getIntegrationAppId());
        }

        WkQcChannel channel = channelMapper.selectById(appAuth.getChannelId());
        if (channel == null || !StringUtils.hasText(channel.getCode())) {
            throw new IllegalStateException("未找到渠道配置或渠道编码为空, channelId=" + appAuth.getChannelId());
        }

        // 3. 查最新 refresh_token
        WkQcAppToken latest = appTokenMapper.selectOne(
                new LambdaQueryWrapper<WkQcAppToken>()
                        .eq(WkQcAppToken::getAppAuthId, appAuthId)
                        .eq(WkQcAppToken::getStatus, 1)
                        .orderByDesc(WkQcAppToken::getId)
                        .last("LIMIT 1")
        );
        if (latest == null || !StringUtils.hasText(latest.getRefreshToken())) {
            throw new IllegalStateException("没有可用 refresh_token，请重新授权, appAuthId=" + appAuthId);
        }
        String refreshToken = latest.getRefreshToken();

        // 4. 按渠道决定走哪套刷新逻辑
        JSONObject respJson;
        String channelCode = channel.getCode();
        if ("OCEANENGINE_ADS".equals(channelCode)) {
            respJson = refreshAdsToken(appAuthId, app, refreshToken);
        } else if ("OCEANENGINE_QIANCHUAN".equals(channelCode)) {
            respJson = refreshQianchuanToken(appAuthId, app, appAuth, refreshToken);
        } else {
            throw new IllegalStateException("暂不支持该渠道刷新 token, channelCode=" + channelCode);
        }

        // 5. 统一处理返回并写入新 token
        int code = respJson.getIntValue("code");
        String msg = respJson.getString("message");
        if (code != 0) {
            log.error("刷新 OceanEngine token 失败, appAuthId={}, code={}, message={}, resp={}",
                    appAuthId, code, msg, respJson.toJSONString());

            if (code == 40107) {
                // refresh_token 无效，需要前端重新走授权
                throw new IllegalStateException("刷新 OceanEngine token 失败，需要重新走授权流程, appAuthId="
                        + appAuthId + ", code=" + code + ", message=" + msg);
            }

            throw new IllegalStateException("刷新失败，需要重新授权, appAuthId="
                    + appAuthId + ", code=" + code + ", message=" + msg);
        }

        JSONObject data = respJson.getJSONObject("data");
        String newAccessToken = data.getString("access_token");
        String newRefreshToken = data.getString("refresh_token");
        long expiresIn = data.getLongValue("expires_in");

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);

        // 旧 token 失效，新 token 入库
        latest.setStatus(0);
        appTokenMapper.updateById(latest);

        WkQcAppToken newToken = new WkQcAppToken();
        newToken.setAppAuthId(appAuthId);
        newToken.setChannelId(appAuth.getChannelId());
        newToken.setAccessToken(newAccessToken);
        newToken.setRefreshToken(newRefreshToken);
        newToken.setExpiresAt(expiresAt);
        newToken.setRawResponse(respJson.toJSONString());
        newToken.setStatus(1);
        newToken.setCreatedAt(LocalDateTime.now());
        newToken.setUpdatedAt(LocalDateTime.now());
        appTokenMapper.insert(newToken);

        log.info("刷新 OceanEngine token 成功, appAuthId={}, tokenId={}, expiresAt={}",
                appAuthId, newToken.getId(), expiresAt);

        return newAccessToken;
    }
}