package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.service.OeSaleUserProvisionService;
import com.kakarote.admin.oceanengine.service.impl.OeSaleUserProvisionServiceImpl.ProvisionResult;
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

/**
 * 销售账号自动创建与映射
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/adminApi/qc/oe/sales")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeSaleUserController {

    private final OeSaleUserProvisionService saleUserProvisionService;

    @PostMapping("/sync")
    @ApiOperation(value = "同步巨量销售账号并自动建联悟空用户", notes = "不传 channelCode 时默认同步全部渠道")
    public Result<ProvisionResult> sync(@ApiParam(value = "渠道编码，OCEANENGINE_ADS 或 OCEANENGINE_QIANCHUAN，可为空")
                                        @RequestParam(value = "channelCode", required = false) String channelCode) {
        ProvisionResult result = saleUserProvisionService.syncSaleUsersFromAdvertisers(channelCode);
        return Result.ok(result);
    }
}
