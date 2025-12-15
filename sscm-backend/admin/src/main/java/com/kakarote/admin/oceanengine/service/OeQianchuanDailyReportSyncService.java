package com.kakarote.admin.oceanengine.service;

import java.time.LocalDate;

/**
 * 千川日报同步服务
 */
public interface OeQianchuanDailyReportSyncService {

    /**
     * 同步指定日期的千川广告主日报数据
     */
    QcDailySyncResult syncQcDaily(LocalDate statDate);

    /**
     * 同步单个广告主在日期区间内的千川日报数据
     */
    QcDailySyncResult syncQcDailyForAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate);
    /**
     * 同步指定广告主在日期区间内的千川日报数据
     */
    QcDailySyncResult syncQcDaily(LocalDate startDate, LocalDate endDate, java.util.List<Long> advertiserIds);

    /**
     * 查询某客户公司下的千川广告主ID列表
     */
    java.util.List<Long> listQcAdvertiserIdsByCompany(Long advCompanyId);

    /**
     * 按公司+天数范围同步千川日报
     */
    QcDailySyncResult syncByCompany(Long advCompanyId, int days);

    class QcDailySyncResult {
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
