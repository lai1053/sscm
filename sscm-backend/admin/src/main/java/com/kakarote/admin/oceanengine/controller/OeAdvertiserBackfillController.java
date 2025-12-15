package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.service.OeAdvertiserSaleBindService;
import com.kakarote.core.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手动触发广告主与销售绑定回填
 */
@Slf4j
@RestController
@RequestMapping("/adminApi/qc/oe/advertisers")
@Api(tags = "巨量对接接口")
@RequiredArgsConstructor
public class OeAdvertiserBackfillController {

    private final OeAdvertiserSaleBindService bindService;

    @PostMapping("/backfillSaleUser")
    @ApiOperation(value = "回填巨量广告主的销售绑定关系")
    public Result<Void> backfillSaleUser() {
        bindService.backfillSaleUserId();
        return Result.ok();
    }
}
