package com.kakarote.admin.oceanengine.job;

import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService.AdsDailySyncResult;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService.QcDailySyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 同步 ADS + 千川 昨日消耗日报
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OceanEngineDailyCostSyncJob {

    private final OeAdsDailyReportSyncService adsDailyReportSyncService;
    private final OeQianchuanDailyReportSyncService qianchuanDailyReportSyncService;

    /**
     * 每天 04:30 同步昨日消耗
     */
    @Scheduled(cron = "0 30 4 * * ?")
    public void syncYesterdayCost() {
        LocalDate statDate = LocalDate.now().minusDays(1);
        long start = System.currentTimeMillis();
        log.info("[OE_DAILY_COST] start, statDate={}", statDate);

        AdsDailySyncResult adsResult = null;
        QcDailySyncResult qcResult = null;

        try {
            adsResult = adsDailyReportSyncService.syncAdsDaily(statDate);
        } catch (Exception e) {
            log.warn("[OE_DAILY_COST] ADS 日报同步异常, statDate={}, msg={}", statDate, e.getMessage(), e);
        }

        try {
            qcResult = qianchuanDailyReportSyncService.syncQcDaily(statDate);
        } catch (Exception e) {
            log.warn("[OE_DAILY_COST] 千川日报同步异常, statDate={}, msg={}", statDate, e.getMessage(), e);
        }

        long costMs = System.currentTimeMillis() - start;
        log.info("[OE_DAILY_COST] end, statDate={}, ads(total={}, success={}, fail={}), qc(total={}, success={}, fail={}), cost={}ms",
                statDate,
                adsResult == null ? 0 : adsResult.getTotal(),
                adsResult == null ? 0 : adsResult.getSuccess(),
                adsResult == null ? 0 : adsResult.getFail(),
                qcResult == null ? 0 : qcResult.getTotal(),
                qcResult == null ? 0 : qcResult.getSuccess(),
                qcResult == null ? 0 : qcResult.getFail(),
                costMs);
    }
}
