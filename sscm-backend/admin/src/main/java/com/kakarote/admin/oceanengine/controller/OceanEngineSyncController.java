package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.service.OeAccountDiscoveryService;
import com.kakarote.admin.oceanengine.service.OeAdvertiserSyncService;
import com.kakarote.admin.oceanengine.service.impl.OeAccountDiscoveryServiceImpl.AdvertiserDetail;
import com.kakarote.admin.oceanengine.service.impl.OeAdvertiserSyncServiceImpl.OeAdvertiserSyncResult;
import com.kakarote.core.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 手动触发巨量账户发现与广告主同步
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/adminApi/qc/oe/advertisers")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OceanEngineSyncController {

    private final OeAccountDiscoveryService accountDiscoveryService;
    private final OeAdvertiserSyncService advertiserSyncService;

    @PostMapping("/sync")
    @ApiOperation(value = "手动发现并同步巨量广告主", notes = "channelCode 取值：OCEANENGINE_ADS 或 OCEANENGINE_QIANCHUAN")
    public Result<OeAdvertiserSyncResult> sync(@ApiParam(value = "渠道编码", required = true) @RequestParam("channelCode") String channelCode) {
        try {
            OceanChannelCode channel = OceanChannelCode.fromCode(channelCode);
            log.info("[OE_SYNC] 手动同步触发, channel={}", channel.getCode());
            List<AdvertiserDetail> details = accountDiscoveryService.discoverAdvertisers(channel.getCode());
            OeAdvertiserSyncResult syncResult = advertiserSyncService.syncAdvertisers(details);
            return Result.ok(syncResult);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            log.error("[OE_SYNC] 手动同步异常, channelCode={}, msg={}", channelCode, ex.getMessage(), ex);
            return Result.error(500, "同步失败：" + ex.getMessage());
        }
    }
}
