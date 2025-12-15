package com.kakarote.admin.oceanengine.job;

import com.kakarote.admin.oceanengine.service.OceanEngineSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OceanEngineSyncJob {

    private final OceanEngineSyncService syncService; // OceanEngineSynService

    /**
     * 每天凌晨3点全量同步一次
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void syncJob() {
        log.info("[OE_SYNC_JOB] start");
        syncService.syncAll();
        log.info("[OE_SYNC_JOB] end");
    }
}
