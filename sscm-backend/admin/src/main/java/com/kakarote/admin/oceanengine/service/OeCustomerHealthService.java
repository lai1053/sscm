package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.enums.CustomerHealthStatus;

import java.time.LocalDate;

/**
 * 客户健康度服务
 */
public interface OeCustomerHealthService {

    /**
     * 计算指定客户在给定基准日的健康状态。
     *
     * @param advCompanyId 客户公司ID，对应广告主上的 adv_company_id
     * @param baseDate     统计基准日（通常是 LocalDate.now()）
     * @return 客户健康状态枚举
     */
    CustomerHealthStatus calculateHealth(Long advCompanyId, LocalDate baseDate);
}
