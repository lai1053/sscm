package com.kakarote.admin.oceanengine.service;

import java.time.LocalDate;

/**
 * 销售归属计算服务
 */
public interface OeSaleAssignmentService {

    /**
     * 根据广告主和统计日期，计算该日应归属的销售（sale_user_id）。
     * 规则：按 advertiser 当前 sale_user_id + sale_history 变更时间，找到该日期时刻生效的销售。
     */
    Long resolveSaleUserIdForDate(Long advertiserId, LocalDate statDate);
}
