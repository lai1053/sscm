package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.admin.oceanengine.client.OceanEngineOpenApiClient;
import com.kakarote.admin.oceanengine.entity.WkQcAppToken;
import com.kakarote.admin.oceanengine.entity.WkQcMediaAccount;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.mapper.WkQcAppTokenMapper;
import com.kakarote.admin.oceanengine.mapper.WkQcMediaAccountMapper;
import com.kakarote.admin.oceanengine.service.MediaAccountSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
public class MediaAccountSyncServiceImpl implements MediaAccountSyncService {

    private static final Logger log = LoggerFactory.getLogger(MediaAccountSyncServiceImpl.class);

    @Resource
    private OceanEngineOpenApiClient openApiClient;

    @Resource
    private WkQcMediaAccountMapper mediaAccountMapper;

    @Resource
    private WkQcAppTokenMapper appTokenMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void syncCurrentAuthMediaAccounts() throws UnsupportedEncodingException {
        // 1. 巨量广告
        syncChannelMediaAccounts(OceanChannelCode.OCEANENGINE_ADS);

        // 2. 巨量千川
        syncChannelMediaAccounts(OceanChannelCode.OCEANENGINE_QIANCHUAN);
    }

    @Override
    public List<WkQcMediaAccount> listMediaAccounts(OceanChannelCode channelCode, Integer status) {
        LambdaQueryWrapper<WkQcMediaAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WkQcMediaAccount::getChannelId, channelCode.getChannelId());
        if (status != null) {
            wrapper.eq(WkQcMediaAccount::getStatus, status);
        }
        wrapper.orderByAsc(WkQcMediaAccount::getName);
        return mediaAccountMapper.selectList(wrapper);
    }

    private void syncChannelMediaAccounts(OceanChannelCode channelCode) throws UnsupportedEncodingException {
        log.info("开始同步媒体账户, channel={}", channelCode.getCode());

        JsonNode resp = openApiClient.getAdvertiserByToken(channelCode);
        int code = resp.path("code").asInt(-1);
        if (code != 0) {
            log.error("调用 oauth2/advertiser/get 失败, channel={}, body={}", channelCode.getCode(), resp.toString());
            return;
        }

        JsonNode listNode = resp.path("data").path("list");
        if (!listNode.isArray()) {
            log.warn("媒体账户 list 不是数组, channel={}, data={}", channelCode.getCode(), resp.path("data").toString());
            return;
        }

        // 找到当前 channel 对应的 app_auth_id（用于回写 appAuthId 字段）
        Long appAuthId = findAppAuthIdByChannel(channelCode);
        LocalDateTime now = LocalDateTime.now();

        Iterator<JsonNode> it = listNode.elements();
        while (it.hasNext()) {
            JsonNode item = it.next();

            long accountId = item.path("account_id").asLong();
            String accountName = item.path("account_name").asText("");
            String accountRole = item.path("account_role").asText("");   // 平台角色
            String accountType = item.path("account_type").asText("");   // 基本等于 role
            String advertiserName = item.path("advertiser_name").asText("");
            boolean isValid = item.path("is_valid").asBoolean(true);

            String externalAccountId = String.valueOf(accountId);
            Long channelId = channelCode.getChannelId();

            // upsert: 先查是否已存在
            LambdaQueryWrapper<WkQcMediaAccount> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WkQcMediaAccount::getChannelId, channelId)
                    .eq(WkQcMediaAccount::getExternalAccountId, externalAccountId)
                    .last("LIMIT 1");

            WkQcMediaAccount exist = mediaAccountMapper.selectOne(wrapper);
            if (exist == null) {
                exist = new WkQcMediaAccount();
                exist.setChannelId(channelId);
                exist.setExternalAccountId(externalAccountId);
                exist.setCreatedAt(now);
            }

            exist.setName(accountName);
            exist.setCompanyName(advertiserName); // 这里先简单用 advertiser_name，当成公司名
            exist.setAccountType(accountType);
            exist.setAppAuthId(appAuthId);
            exist.setStatus(isValid ? 1 : 0);
            exist.setExtra(item.toString());
            exist.setUpdatedAt(now);

            if (exist.getId() == null) {
                mediaAccountMapper.insert(exist);
                log.info("新增媒体账户, channel={}, accountId={}, name={}", channelCode.getCode(), externalAccountId, accountName);
            } else {
                mediaAccountMapper.updateById(exist);
                log.info("更新媒体账户, channel={}, accountId={}, name={}", channelCode.getCode(), externalAccountId, accountName);
            }
        }

        log.info("同步媒体账户完成, channel={}", channelCode.getCode());
    }

    /**
     * 简单版：取当前 channel 下 status=1 的第一条 token 的 appAuthId
     * 你目前一条 channel 只有一个 token，这样就够用了。
     */
    private Long findAppAuthIdByChannel(OceanChannelCode channelCode) {
        LambdaQueryWrapper<WkQcAppToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WkQcAppToken::getChannelId, channelCode.getChannelId())
                .eq(WkQcAppToken::getStatus, 1)
                .orderByDesc(WkQcAppToken::getExpiresAt)
                .last("LIMIT 1");

        WkQcAppToken token = appTokenMapper.selectOne(wrapper);
        if (token == null) {
            log.warn("未找到 channel={} 的 app_token 记录", channelCode.getCode());
            return null;
        }
        return token.getAppAuthId();
    }
}