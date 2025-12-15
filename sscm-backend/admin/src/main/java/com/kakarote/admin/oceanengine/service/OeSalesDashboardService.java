package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.model.SalesAdvertiserVO;
import com.kakarote.admin.oceanengine.model.AccountSourceSummaryVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardAdvertiserVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardCompanyVO;
import com.kakarote.admin.oceanengine.model.SalesCompanySummaryVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardSummaryVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardSaleVO;
import com.kakarote.core.entity.BasePage;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;

public interface OeSalesDashboardService {

    BasePage<SalesDashboardSummaryVO> listSalesSummary(Integer page, Integer limit, @Nullable String keyword);

    BasePage<SalesAdvertiserVO> listAdvertisersBySaleUser(Long saleUserId, Integer page, Integer limit, @Nullable String channel);

    /**
     * 名下客户消耗汇总
     */
    List<SalesCompanySummaryVO> summaryBySaleUser(Long saleUserId, LocalDate startDate, LocalDate endDate);

    /**
     * 名下近 N 天无消耗客户
     */
    List<SalesCompanySummaryVO> inactiveCompaniesBySaleUser(Long saleUserId, int days);

    /**
     * 账户来源维度汇总
     */
    List<AccountSourceSummaryVO> listAccountSourceSummary();

    /**
     * 销售层级看板
     */
    List<SalesDashboardSaleVO> dashboardSales(int window, String channel, String accountSource);

    /**
     * 销售-公司层级看板
     */
    List<SalesDashboardCompanyVO> dashboardCompanies(Long saleUserId, int window, String channel, String accountSource);

    /**
     * 销售-公司-广告主层级看板
     */
    List<SalesDashboardAdvertiserVO> dashboardAdvertisers(Long saleUserId, Long advCompanyId, int window, String channel, String accountSource);

    /**
     * 销售总览
     */
    com.kakarote.admin.oceanengine.model.SalesOverviewVO overview(Integer window, String channel, String accountSource, Long saleUserId);

    /**
     * 客户消耗趋势
     */
    List<com.kakarote.admin.oceanengine.model.CustomerTrendPointVO> customerTrend(Long advCompanyId, Integer window, String channel, String accountSource);
}
