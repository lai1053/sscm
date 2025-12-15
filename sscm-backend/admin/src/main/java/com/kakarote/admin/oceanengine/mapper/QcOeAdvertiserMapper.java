package com.kakarote.admin.oceanengine.mapper;

import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.model.AccountSourceSummaryVO;
import com.kakarote.admin.oceanengine.model.SalesAdvertiserVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardAdvertiserVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardCompanyVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardSaleVO;
import com.kakarote.core.entity.BasePage;
import com.kakarote.core.servlet.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 巨量广告主账户表 Mapper
 */
public interface QcOeAdvertiserMapper extends BaseMapper<QcOeAdvertiser> {

    java.util.List<SalesAdvertiserVO> listAdvertisersBySaleUser(BasePage<SalesAdvertiserVO> page,
                                                                @Param("saleUserIds") java.util.List<Long> saleUserIds,
                                                                @Param("channel") String channel);

    java.util.List<SalesDashboardSaleVO> listSalesDashboard(@Param("startDate") java.sql.Date startDate,
                                                            @Param("endDate") java.sql.Date endDate,
                                                            @Param("channel") String channel,
                                                            @Param("accountSource") String accountSource);

    java.util.List<SalesDashboardCompanyVO> listCompanyDashboard(@Param("saleUserIds") java.util.List<Long> saleUserIds,
                                                                 @Param("startDate") java.sql.Date startDate,
                                                                 @Param("endDate") java.sql.Date endDate,
                                                                 @Param("channel") String channel,
                                                                 @Param("accountSource") String accountSource);

    java.util.List<SalesDashboardAdvertiserVO> listAdvertiserDashboard(@Param("saleUserIds") java.util.List<Long> saleUserIds,
                                                                      @Param("advCompanyId") Long advCompanyId,
                                                                      @Param("startDate") java.sql.Date startDate,
                                                                      @Param("endDate") java.sql.Date endDate,
                                                                      @Param("channel") String channel,
                                                                      @Param("accountSource") String accountSource);

    java.util.List<AccountSourceSummaryVO> listAccountSourceSummary();

    int updateLastCostDateIfNewer(@Param("advertiserId") Long advertiserId, @Param("statDate") java.sql.Date statDate);

    java.util.List<com.kakarote.admin.oceanengine.model.SalesOverviewCompanyRow> listCompanyCostForOverview(
            @Param("startDate") java.sql.Date startDate,
            @Param("endDate") java.sql.Date endDate,
            @Param("channel") String channel,
            @Param("accountSource") String accountSource,
            @Param("saleUserIds") java.util.List<Long> saleUserIds);

    java.util.List<com.kakarote.admin.oceanengine.model.CompanyLastCostRow> listCompanyLastCostDate(
            @Param("channel") String channel,
            @Param("accountSource") String accountSource,
            @Param("saleUserIds") java.util.List<Long> saleUserIds);

    java.util.List<com.kakarote.admin.oceanengine.model.CustomerTrendPointVO> listCustomerTrend(
            @Param("advCompanyId") Long advCompanyId,
            @Param("startDate") java.sql.Date startDate,
            @Param("endDate") java.sql.Date endDate,
            @Param("channel") String channel,
            @Param("accountSource") String accountSource);
}
