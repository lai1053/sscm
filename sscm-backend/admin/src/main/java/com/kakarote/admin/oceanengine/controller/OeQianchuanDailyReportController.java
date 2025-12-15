package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.auth.OeAuthChecker;
import com.kakarote.admin.oceanengine.auth.OeAuthConst;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService.QcDailySyncResult;
import com.kakarote.core.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 千川日报同步入口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/adminApi/qc/oe/report/qc/daily")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeQianchuanDailyReportController {

    private final OeQianchuanDailyReportSyncService syncService;

    @PostMapping("/sync")
    @ApiOperation(value = "同步指定日期千川日报")
    public Result<QcDailySyncResult> sync(@ApiParam(value = "统计日期，格式yyyy-MM-dd", required = true)
                                          @RequestParam("statDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_SYNC_DAILY);
        QcDailySyncResult result = syncService.syncQcDaily(statDate);
        return Result.ok(result);
    }
}
