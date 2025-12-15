package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.auth.OeAuthChecker;
import com.kakarote.admin.oceanengine.auth.OeAuthConst;
import com.kakarote.admin.oceanengine.service.OeDailyBackfillService;
import com.kakarote.core.common.Result;
import com.kakarote.core.common.SystemCodeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日报历史回补入口（管理用途）。
 */
@Slf4j
@RestController
@RequestMapping("/adminApi/qc/oe/backfill")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeReportBackfillController {

    private final OeDailyBackfillService backfillService;

    @PostMapping("/company/{advCompanyId}/recent90")
    @ApiOperation(value = "管理端触发公司级日报回补", notes = "回补最近 90 天 ADS + 千川数据")
    public Result<String> backfillCompany(@ApiParam(value = "客户公司ID", required = true) @PathVariable Long advCompanyId) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BACKFILL_DAILY);
        if (advCompanyId == null || advCompanyId <= 0) {
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "invalid advCompanyId");
        }
        backfillService.backfillRecent90DaysForCompany(advCompanyId);
        return Result.ok("backfill company " + advCompanyId + " recent 90 days triggered");
    }

    @PostMapping("/advertiser/{advertiserId}/recent90")
    @ApiOperation(value = "管理端触发广告主日报回补", notes = "回补最近 90 天 ADS + 千川数据")
    public Result<String> backfillAdvertiser(@ApiParam(value = "广告主ID", required = true) @PathVariable Long advertiserId) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BACKFILL_DAILY);
        if (advertiserId == null || advertiserId <= 0) {
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "invalid advertiserId");
        }
        backfillService.backfillRecent90DaysForAdvertiser(advertiserId);
        return Result.ok("backfill advertiser " + advertiserId + " recent 90 days triggered");
    }
}
