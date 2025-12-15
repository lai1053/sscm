package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.auth.OeAuthChecker;
import com.kakarote.admin.oceanengine.auth.OeAuthConst;
import com.kakarote.admin.oceanengine.enums.CustomerHealthStatus;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeAccountDiscoveryService;
import com.kakarote.admin.oceanengine.service.OeAdvertiserSyncService;
import com.kakarote.admin.oceanengine.service.OeCustomerHealthService;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.impl.OeAccountDiscoveryServiceImpl;
import com.kakarote.admin.oceanengine.service.impl.OeAdvertiserSyncServiceImpl;
import com.kakarote.core.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * Debug 调试接口，用于手动执行 OceanEngine 相关维护动作，如查看客户健康度、按公司触发广告/千川同步。
 */
@RestController
@RequestMapping("/adminApi/qc/oe/debug")
@Api(tags = "巨量对接接口")
public class OeDebugController {

    @Resource
    private OeCustomerHealthService oeCustomerHealthService;

    @Resource
    private OeAdsDailyReportSyncService oeAdsDailyReportSyncService;

    @Resource
    private OeAccountDiscoveryService oeAccountDiscoveryService;

    @Resource
    private OeAdvertiserSyncService oeAdvertiserSyncService;

    @Resource
    private OeQianchuanDailyReportSyncService oeQianchuanDailyReportSyncService;

    /**
     * 计算指定广告主公司的最新健康度。
     *
     * @param advCompanyId 广告主公司 ID
     * @return 健康度结果
     */
    @GetMapping("/customer/health")
    @ApiOperation(value = "查询客户健康度")
    public Result<CustomerHealthStatus> getCustomerHealth(@ApiParam(value = "客户公司ID", required = true) @RequestParam Long advCompanyId) {
        LocalDate today = LocalDate.now();
        CustomerHealthStatus status = oeCustomerHealthService.calculateHealth(advCompanyId, today);
        return Result.ok(status);
    }

    /**
     * 手动按公司同步 OceanEngine ADS 日报，覆盖该公司下所有广告主。
     * 未传或非法的 {@code days} 默认使用最近 7 天。
     */
    @GetMapping("/ads/daily/syncByCompany")
    @ApiOperation(value = "按公司同步 ADS 日报", notes = "默认回填最近7天，可调整 days 范围")
    public Result<String> syncAdsByCompany(
            @ApiParam(value = "客户公司ID", required = true) @RequestParam Long advCompanyId,
            @ApiParam(value = "回填天数，默认7天") @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BACKFILL_DAILY);
        int period = days == null || days <= 0 ? 7 : days;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period - 1L);
        List<Long> advertiserIds = oeAdsDailyReportSyncService.listAdsAdvertiserIdsByCompany(advCompanyId);
        if (advertiserIds == null || advertiserIds.isEmpty()) {
            return Result.ok("no advertisers for company " + advCompanyId);
        }
        oeAdsDailyReportSyncService.syncAdsDaily(startDate, endDate, advertiserIds);
        String msg = "companyId=" + advCompanyId + ", advertisers=" + advertiserIds.size()
                + ", range=" + startDate + " ~ " + endDate;
        return Result.ok(msg);
    }

    /**
     * 手动按公司同步千川日报，覆盖该公司下所有广告主。
     * 未传或非法的 {@code days} 默认使用最近 7 天。
     */
    @GetMapping("/qc/daily/syncByCompany")
    @ApiOperation(value = "按公司同步千川日报", notes = "默认回填最近7天，可调整 days 范围")
    public Result<String> syncQcByCompany(
            @ApiParam(value = "客户公司ID", required = true) @RequestParam Long advCompanyId,
            @ApiParam(value = "回填天数，默认7天") @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_BACKFILL_DAILY);
        int period = days == null || days <= 0 ? 7 : days;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period - 1L);
        List<Long> advertiserIds = oeQianchuanDailyReportSyncService.listQcAdvertiserIdsByCompany(advCompanyId);
        if (advertiserIds == null || advertiserIds.isEmpty()) {
            return Result.ok("no advertisers for company " + advCompanyId);
        }
        oeQianchuanDailyReportSyncService.syncQcDaily(startDate, endDate, advertiserIds);
        String msg = "qc companyId=" + advCompanyId + ", advertisers=" + advertiserIds.size()
                + ", range=" + startDate + " ~ " + endDate;
        return Result.ok(msg);
    }

    /**
     * ADS 渠道广告主全量发现并同步。
     *
     * @return 同步结果摘要
     */
    @PostMapping("/ads/advertisers/fullResync")
    @ApiOperation(value = "ADS 渠道广告主全量发现并同步")
    public Result<OeAdvertiserSyncServiceImpl.OeAdvertiserSyncResult> fullResyncAds() {
        List<OeAccountDiscoveryServiceImpl.AdvertiserDetail> details =
                oeAccountDiscoveryService.discoverAdvertisers(OceanChannelCode.OCEANENGINE_ADS.getCode());
        OeAdvertiserSyncServiceImpl.OeAdvertiserSyncResult result = oeAdvertiserSyncService.syncAdvertisers(details);
        return Result.ok(result);
    }

    /**
     * 千川渠道广告主全量发现并同步。
     *
     * @return 同步结果摘要
     */
    @PostMapping("/qc/advertisers/fullResync")
    @ApiOperation(value = "千川渠道广告主全量发现并同步")
    public Result<OeAdvertiserSyncServiceImpl.OeAdvertiserSyncResult> fullResyncQc() {
        List<OeAccountDiscoveryServiceImpl.AdvertiserDetail> details =
                oeAccountDiscoveryService.discoverAdvertisers(OceanChannelCode.OCEANENGINE_QIANCHUAN.getCode());
        OeAdvertiserSyncServiceImpl.OeAdvertiserSyncResult result = oeAdvertiserSyncService.syncAdvertisers(details);
        return Result.ok(result);
    }
}
