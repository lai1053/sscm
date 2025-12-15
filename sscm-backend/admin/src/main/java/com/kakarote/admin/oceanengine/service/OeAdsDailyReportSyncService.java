package com.kakarote.admin.oceanengine.service;

import java.time.LocalDate;

/**
 * ADS日报同步服务
 */
public interface OeAdsDailyReportSyncService {

    /**
     * 同步指定统计日期的 ADS 广告主日报数据到 wk_qc_oe_ads_advertiser_daily。
     * statDate 为自然日，按天粒度。
     */
    AdsDailySyncResult syncAdsDaily(LocalDate statDate);

    /**
     * 同步指定广告主在日期区间内的 ADS 日报数据
     */
    AdsDailySyncResult syncAdsDaily(LocalDate startDate, LocalDate endDate, java.util.List<Long> advertiserIds);

    /**
     * 同步单个广告主在日期区间内的 ADS 日报数据
     */
    AdsDailySyncResult syncAdsDailyForAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询某客户公司下的ADS广告主ID列表
     */
    java.util.List<Long> listAdsAdvertiserIdsByCompany(Long advCompanyId);

    class AdsDailySyncResult {
        private int total;
        private int success;
        private int fail;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFail() {
            return fail;
        }

        public void setFail(int fail) {
            this.fail = fail;
        }
    }
}
