package com.kakarote.admin.oceanengine.job;

import com.kakarote.admin.oceanengine.service.OceanTokenRefreshService;
import com.kakarote.admin.oceanengine.service.OceanTokenRefreshService.RefreshResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时刷新 OceanEngine access_token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OceanEngineTokenRefreshJob {

    private final OceanTokenRefreshService tokenRefreshService;

    /**
     * 每 12 小时刷新一次即将过期的 token
     */
    @Scheduled(cron = "0 0 */12 * * ?")
    public void refreshTokens() {
        log.info("[OE_TOKEN_REFRESH] start");
        RefreshResult result = tokenRefreshService.refreshExpiringTokens();
        log.info("[OE_TOKEN_REFRESH] end, checked={}, refreshed={}, skipped={}, failed={}",
                result.getChecked(), result.getRefreshed(),
                result.getSkipped(), result.getFailed());
    }
}
