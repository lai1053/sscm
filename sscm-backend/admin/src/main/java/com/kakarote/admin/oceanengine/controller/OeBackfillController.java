package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.auth.OeAuthChecker;
import com.kakarote.admin.oceanengine.auth.OeAuthConst;
import com.kakarote.admin.oceanengine.service.OeDailyBackfillService;
import com.kakarote.core.common.Result;
import com.kakarote.core.common.SystemCodeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OceanEngine 日报回填管理入口
 */
@Slf4j
@RestController
@RequestMapping("/adminApi/qc/oe/tools/backfill")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeBackfillController {

    private final OeDailyBackfillService backfillService;

    @PostMapping("/advertiser")
    @ApiOperation(value = "回填单个广告主的近期日报", notes = "默认回填最近 90 天 ADS 与千川日报")
    public Result<Map<String, Object>> backfillAdvertiser(@ApiParam(value = "广告主ID请求体", required = true) @RequestBody AdvertiserBackfillRequest request) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BACKFILL_DAILY);
        if (request == null || request.getAdvertiserId() == null || request.getAdvertiserId() <= 0) {
            log.warn("[OE_BACKFILL_ADVERTISER] invalid advertiserId, request={}", request);
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "invalid advertiserId");
        }
        String status = "OK";
        String message = "已触发最近历史窗口回填";
        try {
            backfillService.backfillRecent90DaysForAdvertiser(request.getAdvertiserId());
            log.info("[OE_BACKFILL_ADVERTISER] triggered, advertiserId={}", request.getAdvertiserId());
        } catch (Exception e) {
            status = "SKIPPED";
            message = "回填触发失败: " + e.getMessage();
            log.warn("[OE_BACKFILL_ADVERTISER] failed, advertiserId={}, msg={}", request.getAdvertiserId(), e.getMessage(), e);
        }
        Map<String, Object> data = new HashMap<>(4);
        data.put("advertiserId", request.getAdvertiserId());
        data.put("status", status);
        data.put("message", message);
        return Result.ok(data);
    }

    @PostMapping("/company")
    @ApiOperation(value = "回填客户公司下全部广告主的近期日报", notes = "ADS 约回填 90 天，千川约回填 30 天")
    public Result<Map<String, Object>> backfillCompany(@ApiParam(value = "客户公司ID请求体", required = true) @RequestBody CompanyBackfillRequest request) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BACKFILL_DAILY);
        if (request == null || request.getAdvCompanyId() == null || request.getAdvCompanyId() <= 0) {
            log.warn("[OE_BACKFILL_COMPANY] invalid advCompanyId, request={}", request);
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "invalid advCompanyId");
        }
        String status = "OK";
        String message = "已触发最近历史窗口回填（ADS 约 90 天，千川约 30 天）";
        try {
            backfillService.backfillRecent90DaysForCompany(request.getAdvCompanyId());
            log.info("[OE_BACKFILL_COMPANY] triggered, advCompanyId={}", request.getAdvCompanyId());
        } catch (Exception e) {
            status = "SKIPPED";
            message = "回填触发失败: " + e.getMessage();
            log.warn("[OE_BACKFILL_COMPANY] failed, advCompanyId={}, msg={}", request.getAdvCompanyId(), e.getMessage(), e);
        }
        Map<String, Object> data = new HashMap<>(4);
        data.put("advCompanyId", request.getAdvCompanyId());
        data.put("status", status);
        data.put("message", message);
        return Result.ok(data);
    }

    @Data
    @ApiModel("广告主回填请求")
    public static class AdvertiserBackfillRequest {
        @ApiModelProperty(value = "巨量广告主ID", required = true, example = "123456789")
        private Long advertiserId;
    }

    @Data
    @ApiModel("客户公司回填请求")
    public static class CompanyBackfillRequest {
        @ApiModelProperty(value = "巨量客户公司ID（adv_company_id）", required = true, example = "10001")
        private Long advCompanyId;
    }
}
