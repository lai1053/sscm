package com.kakarote.admin.oceanengine.mapper;

import com.kakarote.admin.oceanengine.entity.QcOeAdsAdvertiserDaily;
import com.kakarote.core.servlet.BaseMapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 巨量广告主日报表（ADS） Mapper
 */
public interface QcOeAdsAdvertiserDailyMapper extends BaseMapper<QcOeAdsAdvertiserDaily> {

    /**
     * 按公司聚合消耗
     */
    List<Map<String, Object>> selectCostByCompany(java.sql.Date startDate, java.sql.Date endDate);

    /**
     * 按公司取最近一次有消耗日期
     */
    List<Map<String, Object>> selectLastDateByCompany(java.sql.Date startDate, java.sql.Date endDate);

    /**
     * 按公司聚合消耗（限定公司ID列表）
     */
    List<Map<String, Object>> selectCostByCompanyFiltered(java.sql.Date startDate, java.sql.Date endDate, Set<Long> companyIds);

    /**
     * 按销售聚合公司消耗
     */
    List<Map<String, Object>> selectCostByCompanyAndSaleUser(java.sql.Date startDate, java.sql.Date endDate, List<Long> saleUserIds);

    /**
     * 按公司统计区间消耗
     */
    BigDecimal sumCostByCompany(java.sql.Date startDate, java.sql.Date endDate, Long advCompanyId, Set<Long> advertiserIds);

    BigDecimal sumCostByAdvertiserAndDateRange(@Param("advertiserId") Long advertiserId,
                                               @Param("startDate") java.sql.Date startDate,
                                               @Param("endDate") java.sql.Date endDate);

    BigDecimal sumCostByAdvertisersAndDateRange(@Param("advertiserIds") java.util.List<Long> advertiserIds,
                                                @Param("startDate") java.sql.Date startDate,
                                                @Param("endDate") java.sql.Date endDate);

    java.util.List<com.kakarote.admin.oceanengine.dto.DailyCostRow> listDailyByAdvertiserAndDateRange(
            @Param("advertiserId") Long advertiserId,
            @Param("startDate") java.sql.Date startDate,
            @Param("endDate") java.sql.Date endDate);

    void upsertBatch(@Param("list") java.util.List<QcOeAdsAdvertiserDaily> list);
}
