package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.dto.DailyCostRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 统一封装 ADS/千川 日报查询，所有业务方应从此处取数，避免直接调用开放平台或手写 SQL。
 */
public interface OeDailyQueryService {

    // --- 广告主维度 ---

    /**
     * ADS 广告主在日期区间的总消耗。
     */
    BigDecimal sumAdsCostByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate);

    /**
     * 千川广告主在日期区间的总消耗。
     */
    BigDecimal sumQcCostByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate);

    // --- 公司维度 ---

    /**
     * ADS 公司维度总消耗（公司下所有 ADS advertiser 汇总）。
     */
    BigDecimal sumAdsCostByCompany(Long advCompanyId, LocalDate startDate, LocalDate endDate);

    /**
     * 千川 公司维度总消耗（公司下所有 QC advertiser 汇总）。
     */
    BigDecimal sumQcCostByCompany(Long advCompanyId, LocalDate startDate, LocalDate endDate);

    // --- 明细列表（按天） ---

    /**
     * 千川：按广告主 + 日期区间，返回按天的明细列表。
     */
    List<DailyCostRow> listQcDailyByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate);

    /**
     * ADS：按广告主 + 日期区间，返回按天的明细列表。
     */
    List<DailyCostRow> listAdsDailyByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate);
}
