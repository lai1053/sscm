package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.model.BossCostTrendVO;
import com.kakarote.admin.oceanengine.model.InactiveCompanyVO;
import com.kakarote.admin.oceanengine.service.OeBossDashboardService;
import com.kakarote.core.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/adminApi/qc/oe/dashboard/boss")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeBossDashboardController {

    private final OeBossDashboardService bossDashboardService;

    @GetMapping("/overview")
    @ApiOperation(value = "老板看板-消耗趋势")
    public Result<List<BossCostTrendVO>> overview(
            @ApiParam(value = "开始日期，格式yyyy-MM-dd", required = true)
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @ApiParam(value = "结束日期，格式yyyy-MM-dd", required = true)
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<BossCostTrendVO> data = bossDashboardService.listCostTrend(startDate, endDate);
        return Result.ok(data);
    }

    @GetMapping("/customers/inactive")
    @ApiOperation(value = "老板看板-近期无消耗客户")
    public Result<List<InactiveCompanyVO>> inactiveCustomers(
            @ApiParam(value = "未消耗天数阈值，默认30天") @RequestParam(value = "days", required = false, defaultValue = "30") Integer days) {
        List<InactiveCompanyVO> data = bossDashboardService.listInactiveCompanies(days == null ? 30 : days);
        return Result.ok(data);
    }
}
