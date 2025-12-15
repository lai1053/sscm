package com.kakarote.admin.oceanengine.service;

import java.time.LocalDate;

import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService.AdsDailySyncResult;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService.QcDailySyncResult;

/**
 * 日报历史回补服务（ADS + 千川）。
 */
public interface OeDailyBackfillService {

    /**
     * 回补指定广告主最近90天（ADS+千川）。
     */
    void backfillRecent90DaysForAdvertiser(Long advertiserId);

    /**
     * 回补指定公司名下全部广告主最近90天（ADS+千川）。
     */
    void backfillRecent90DaysForCompany(Long advCompanyId);

    /**
     * 同步全量广告主的日报消耗数据（ADS+千川），支持指定日期范围。
     */
    DailyRangeSyncResult syncRangeForAll(LocalDate startDate, LocalDate endDate);

    class DailyRangeSyncResult {
        private LocalDate startDate;
        private LocalDate endDate;
        private AdsDailySyncResult adsResult;
        private QcDailySyncResult qcResult;
        private int adsAdvertiserCount;
        private int qcAdvertiserCount;

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public AdsDailySyncResult getAdsResult() {
            return adsResult;
        }

        public void setAdsResult(AdsDailySyncResult adsResult) {
            this.adsResult = adsResult;
        }

        public QcDailySyncResult getQcResult() {
            return qcResult;
        }

        public void setQcResult(QcDailySyncResult qcResult) {
            this.qcResult = qcResult;
        }

        public int getAdsAdvertiserCount() {
            return adsAdvertiserCount;
        }

        public void setAdsAdvertiserCount(int adsAdvertiserCount) {
            this.adsAdvertiserCount = adsAdvertiserCount;
        }

        public int getQcAdvertiserCount() {
            return qcAdvertiserCount;
        }

        public void setQcAdvertiserCount(int qcAdvertiserCount) {
            this.qcAdvertiserCount = qcAdvertiserCount;
        }
    }
}
