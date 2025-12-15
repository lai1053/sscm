package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.auth.OeAuthChecker;
import com.kakarote.admin.oceanengine.auth.OeAuthConst;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.service.OeAccountDiscoveryService;
import com.kakarote.admin.oceanengine.service.OeAdvertiserSyncService;
import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService.AdsDailySyncResult;
import com.kakarote.admin.oceanengine.service.OeDailyBackfillService;
import com.kakarote.admin.oceanengine.service.OeDailyBackfillService.DailyRangeSyncResult;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService.QcDailySyncResult;
import com.kakarote.admin.oceanengine.service.impl.OeAdvertiserSyncServiceImpl.OeAdvertiserSyncResult;
import com.kakarote.core.common.Result;
import com.kakarote.core.common.SystemCodeEnum;
import com.kakarote.core.utils.UserUtil;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 老板一键同步入口：复用现有广告主同步与日报回填逻辑。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/adminApi/qc/oe/boss")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeBossSyncController {

    private final OeAccountDiscoveryService accountDiscoveryService;
    private final OeAdvertiserSyncService advertiserSyncService;
    private final OeDailyBackfillService dailyBackfillService;

    @PostMapping("/sync/advertisers")
    @ApiOperation(value = "一键同步全部广告账户（ADS+千川）")
    public Result<Map<String, Object>> syncAdvertisers() {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BOSS_SYNC);
        Long userId = UserUtil.getUserId();
        long start = System.currentTimeMillis();
        log.info("[OE_BOSS_ADVERTISER] trigger by userId={}", userId);

        OeAdvertiserSyncResult adsResult = syncAdvertisersByChannel(OceanChannelCode.OCEANENGINE_ADS);
        OeAdvertiserSyncResult qcResult = syncAdvertisersByChannel(OceanChannelCode.OCEANENGINE_QIANCHUAN);

        Map<String, Object> detail = new HashMap<>(4);
        detail.put("ads", buildAdvertiserDetail(adsResult));
        detail.put("qc", buildAdvertiserDetail(qcResult));
        detail.put("total", (adsResult == null ? 0 : adsResult.getTotalCount())
                + (qcResult == null ? 0 : qcResult.getTotalCount()));

        Map<String, Object> resp = new HashMap<>(8);
        resp.put("ok", true);
        resp.put("action", "bossSyncAdvertisers");
        resp.put("detail", detail);
        resp.put("durationMs", System.currentTimeMillis() - start);

        log.info("[OE_BOSS_ADVERTISER] done userId={}, total={}, durationMs={}",
                userId, detail.get("total"), resp.get("durationMs"));
        return Result.ok(resp);
    }

    @PostMapping("/sync/daily")
    @ApiOperation(value = "一键同步全部广告账户消耗（ADS+千川）")
    public Result<Map<String, Object>> syncDaily(
            @ApiParam(value = "开始日期，格式yyyy-MM-dd") @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @ApiParam(value = "结束日期，格式yyyy-MM-dd") @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BOSS_SYNC);
        LocalDate resolvedStart = startDate;
        LocalDate resolvedEnd = endDate;
        if (resolvedStart == null && resolvedEnd == null) {
            resolvedEnd = LocalDate.now().minusDays(1);
            resolvedStart = resolvedEnd;
        } else if (resolvedStart == null || resolvedEnd == null) {
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "startDate and endDate must both be provided or both omitted");
        }
        if (resolvedStart.isAfter(resolvedEnd)) {
            return Result.error(SystemCodeEnum.SYSTEM_NO_VALID, "startDate cannot be after endDate");
        }
        Long userId = UserUtil.getUserId();
        long start = System.currentTimeMillis();
        log.info("[OE_BOSS_DAILY] trigger by userId={} range={}~{}", userId, resolvedStart, resolvedEnd);

        DailyRangeSyncResult result = dailyBackfillService.syncRangeForAll(resolvedStart, resolvedEnd);
        Map<String, Object> resp = new HashMap<>(8);
        Map<String, Object> detail = new HashMap<>(4);
        detail.put("ads", buildAdsDailyDetail(result.getAdsAdvertiserCount(), result.getAdsResult()));
        detail.put("qc", buildQcDailyDetail(result.getQcAdvertiserCount(), result.getQcResult()));

        resp.put("ok", true);
        resp.put("action", "bossSyncDaily");
        resp.put("startDate", resolvedStart.toString());
        resp.put("endDate", resolvedEnd.toString());
        resp.put("detail", detail);
        resp.put("durationMs", System.currentTimeMillis() - start);

        log.info("[OE_BOSS_DAILY] done userId={} range={}~{} durationMs={}",
                userId, resolvedStart, resolvedEnd, resp.get("durationMs"));
        return Result.ok(resp);
    }

    private OeAdvertiserSyncResult syncAdvertisersByChannel(OceanChannelCode channel) {
        try {
            return advertiserSyncService.syncAdvertisers(accountDiscoveryService.discoverAdvertisers(channel.getCode()));
        } catch (Exception ex) {
            log.warn("[OE_BOSS_ADVERTISER] sync channel={} failed, msg={}", channel.getCode(), ex.getMessage(), ex);
            return null;
        }
    }

    private Map<String, Object> buildAdvertiserDetail(OeAdvertiserSyncResult result) {
        Map<String, Object> detail = new HashMap<>(8);
        if (result == null) {
            detail.put("status", "SKIPPED");
            return detail;
        }
        detail.put("total", result.getTotalCount());
        detail.put("insert", result.getInsertCount());
        detail.put("update", result.getUpdateCount());
        detail.put("agentInsert", result.getAgentInsertCount());
        detail.put("agentUpdate", result.getAgentUpdateCount());
        return detail;
    }

    private Map<String, Object> buildAdsDailyDetail(int advertiserCount, AdsDailySyncResult syncResult) {
        Map<String, Object> detail = new HashMap<>(8);
        detail.put("advertisers", advertiserCount);
        if (syncResult != null) {
            detail.put("requests", syncResult.getTotal());
            detail.put("success", syncResult.getSuccess());
            detail.put("fail", syncResult.getFail());
            return detail;
        }
        detail.put("status", "SKIPPED");
        return detail;
    }

    private Map<String, Object> buildQcDailyDetail(int advertiserCount, QcDailySyncResult syncResult) {
        Map<String, Object> detail = new HashMap<>(8);
        detail.put("advertisers", advertiserCount);
        if (syncResult != null) {
            detail.put("requests", syncResult.getTotal());
            detail.put("success", syncResult.getSuccess());
            detail.put("fail", syncResult.getFail());
            return detail;
        }
        detail.put("status", "SKIPPED");
        return detail;
    }
}
