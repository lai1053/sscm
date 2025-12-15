package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.model.AccountSourceSummaryVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardAdvertiserVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardCompanyVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardSaleVO;
import com.kakarote.admin.oceanengine.model.SalesOverviewVO;
import com.kakarote.admin.oceanengine.model.CustomerTrendPointVO;
import com.kakarote.admin.oceanengine.service.OeSalesDashboardService;
import com.kakarote.core.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 销售看板基础版
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/adminApi/qc/oe/dashboard")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeSalesDashboardController {

    private final OeSalesDashboardService dashboardService;

    @GetMapping("/sales")
    @ApiOperation(value = "销售看板-销售维度列表")
    public Result<List<SalesDashboardSaleVO>> sales(
            @ApiParam(value = "统计窗口天数，默认7天") @RequestParam(value = "window", required = false) Integer window,
            @ApiParam(value = "渠道：ALL/OCEANENGINE_ADS/OCEANENGINE_QIANCHUAN", defaultValue = "ALL") @RequestParam(value = "channel", required = false, defaultValue = "ALL") String channel,
            @ApiParam(value = "账号来源过滤，默认ALL") @RequestParam(value = "accountSource", required = false, defaultValue = "ALL") String accountSource,
            @ApiParam(value = "指定销售ID，可选") @RequestParam(value = "saleUserId", required = false) Long saleUserId) {
        int win = window == null ? 7 : window;
        List<SalesDashboardSaleVO> list = dashboardService.dashboardSales(win, channel, accountSource);
        if (saleUserId == null) {
            return Result.ok(list);
        }
        return Result.ok(list.stream()
                .filter(item -> saleUserId.equals(item.getSaleUserId()))
                .collect(Collectors.toList()));
    }

    @GetMapping("/companies")
    @ApiOperation(value = "销售看板-公司维度列表")
    public Result<List<SalesDashboardCompanyVO>> companies(
            @ApiParam(value = "统计窗口天数，默认7天") @RequestParam(value = "window", required = false) Integer window,
            @ApiParam(value = "渠道：ALL/OCEANENGINE_ADS/OCEANENGINE_QIANCHUAN", defaultValue = "ALL") @RequestParam(value = "channel", required = false, defaultValue = "ALL") String channel,
            @ApiParam(value = "账号来源过滤，默认ALL") @RequestParam(value = "accountSource", required = false, defaultValue = "ALL") String accountSource,
            @ApiParam(value = "指定销售ID，可选") @RequestParam(value = "saleUserId", required = false) Long saleUserId) {
        int win = window == null ? 7 : window;
        return Result.ok(dashboardService.dashboardCompanies(saleUserId, win, channel, accountSource));
    }

    @GetMapping("/advertisers")
    @ApiOperation(value = "销售看板-广告主维度列表")
    public Result<List<SalesDashboardAdvertiserVO>> advertisers(
            @ApiParam(value = "统计窗口天数，默认7天") @RequestParam(value = "window", required = false) Integer window,
            @ApiParam(value = "渠道：ALL/OCEANENGINE_ADS/OCEANENGINE_QIANCHUAN", defaultValue = "ALL") @RequestParam(value = "channel", required = false, defaultValue = "ALL") String channel,
            @ApiParam(value = "账号来源过滤，默认ALL") @RequestParam(value = "accountSource", required = false, defaultValue = "ALL") String accountSource,
            @ApiParam(value = "指定销售ID，可选") @RequestParam(value = "saleUserId", required = false) Long saleUserId,
            @ApiParam(value = "指定客户公司ID，可选") @RequestParam(value = "advCompanyId", required = false) Long advCompanyId) {
        int win = window == null ? 7 : window;
        return Result.ok(dashboardService.dashboardAdvertisers(saleUserId, advCompanyId, win, channel, accountSource));
    }

    @GetMapping("/account-source/summary")
    @ApiOperation(value = "销售看板-账号来源分布")
    public Result<List<AccountSourceSummaryVO>> accountSourceSummary() {
        return Result.ok(dashboardService.listAccountSourceSummary());
    }

    @GetMapping("/overview")
    @ApiOperation(value = "销售看板-总览统计")
    public Result<SalesOverviewVO> overview(@RequestParam(value = "window", required = false) Integer window,
                                            @ApiParam(value = "渠道：ALL/OCEANENGINE_ADS/OCEANENGINE_QIANCHUAN", defaultValue = "ALL")
                                            @RequestParam(value = "channel", required = false, defaultValue = "ALL") String channel,
                                            @ApiParam(value = "账号来源过滤，默认ALL")
                                            @RequestParam(value = "accountSource", required = false, defaultValue = "ALL") String accountSource,
                                            @ApiParam(value = "指定销售ID，可选")
                                            @RequestParam(value = "saleUserId", required = false) Long saleUserId) {
        return Result.ok(dashboardService.overview(window, channel, accountSource, saleUserId));
    }

    @GetMapping("/customer/{advCompanyId}/trend")
    @ApiOperation(value = "销售看板-客户消耗趋势")
    public Result<List<CustomerTrendPointVO>> customerTrend(@PathVariable("advCompanyId") Long advCompanyId,
                                                            @ApiParam(value = "统计窗口天数，默认7天") @RequestParam(value = "window", required = false) Integer window,
                                                            @ApiParam(value = "渠道：ALL/OCEANENGINE_ADS/OCEANENGINE_QIANCHUAN", defaultValue = "ALL") @RequestParam(value = "channel", required = false, defaultValue = "ALL") String channel,
                                                            @ApiParam(value = "账号来源过滤，默认ALL") @RequestParam(value = "accountSource", required = false, defaultValue = "ALL") String accountSource) {
        if (advCompanyId == null || advCompanyId <= 0) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(dashboardService.customerTrend(advCompanyId, window, channel, accountSource));
    }
}
