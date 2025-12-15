package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.entity.WkQcAppAuth;
import com.kakarote.admin.oceanengine.entity.WkQcAppToken;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.mapper.WkQcAppAuthMapper;
import com.kakarote.admin.oceanengine.mapper.WkQcAppTokenMapper;
import com.kakarote.admin.oceanengine.service.OceanTokenRefreshService;
import com.kakarote.admin.oceanengine.service.OceanTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OceanTokenRefreshServiceImpl implements OceanTokenRefreshService {

    private static final int DEFAULT_THRESHOLD_MINUTES = 60;

    private final WkQcAppAuthMapper appAuthMapper;
    private final WkQcAppTokenMapper appTokenMapper;
    private final OceanTokenService oceanTokenService;

    private final Map<Long, String> channelCodeMap = Arrays.stream(OceanChannelCode.values())
            .collect(Collectors.toMap(OceanChannelCode::getChannelId, OceanChannelCode::getCode));

    @Override
    public RefreshResult refreshExpiringTokens() {
        return refreshExpiringTokens(DEFAULT_THRESHOLD_MINUTES);
    }

    @Override
    public RefreshResult refreshExpiringTokens(int thresholdMinutes) {
        RefreshResult result = new RefreshResult();
        int safeMinutes = thresholdMinutes > 0 ? thresholdMinutes : DEFAULT_THRESHOLD_MINUTES;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(safeMinutes);

        List<WkQcAppAuth> authList = appAuthMapper.selectList(
                new LambdaQueryWrapper<WkQcAppAuth>()
                        .eq(WkQcAppAuth::getStatus, 1)
        );
        if (CollectionUtils.isEmpty(authList)) {
            log.info("[OE_TOKEN_REFRESH] 无启用的授权记录，跳过刷新");
            return result;
        }

        for (WkQcAppAuth auth : authList) {
            if (auth == null || auth.getId() == null) {
                continue;
            }
            result.setChecked(result.getChecked() + 1);

            WkQcAppToken latest = appTokenMapper.selectOne(
                    new LambdaQueryWrapper<WkQcAppToken>()
                            .eq(WkQcAppToken::getAppAuthId, auth.getId())
                            .eq(WkQcAppToken::getStatus, 1)
                            .orderByDesc(WkQcAppToken::getId)
                            .last("LIMIT 1")
            );

            if (latest == null) {
                log.warn("[OE_TOKEN_REFRESH] 未找到 token，跳过刷新 appAuthId={}, channelId={}",
                        auth.getId(), auth.getChannelId());
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }

            LocalDateTime expiresAt = latest.getExpiresAt();
            boolean needRefresh = expiresAt == null || !expiresAt.isAfter(threshold);
            if (!needRefresh) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }

            try {
                oceanTokenService.refreshTokenByAppAuth(auth.getId());
                result.setRefreshed(result.getRefreshed() + 1);
                log.info("[OE_TOKEN_REFRESH] 刷新 token 成功, appAuthId={}, channel={}, expiresAt={}",
                        auth.getId(), resolveChannelCode(auth.getChannelId()), expiresAt);
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                log.warn("[OE_TOKEN_REFRESH] 刷新 token 失败, appAuthId={}, channel={}, msg={}",
                        auth.getId(), resolveChannelCode(auth.getChannelId()), e.getMessage(), e);
            }
        }

        return result;
    }

    private String resolveChannelCode(Long channelId) {
        String code = channelCodeMap.get(channelId);
        if (StringUtils.hasText(code)) {
            return code;
        }
        return channelId == null ? "UNKNOWN" : String.valueOf(channelId);
    }
}
