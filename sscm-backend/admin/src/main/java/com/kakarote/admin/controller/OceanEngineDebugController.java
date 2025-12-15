package com.kakarote.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kakarote.admin.oceanengine.client.OceanEngineOpenApiClient;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 纯调试用：用数据库中的 token 调一次 oauth2/advertiser/get
 * @author binlonglai
 */
@RestController
@RequestMapping("/admin/oceanengine/debug")
@Api(tags = "巨量对接接口")
public class OceanEngineDebugController {

    @Resource
    private OceanEngineOpenApiClient openApiClient;

    @GetMapping("/advertiser-get")
    @ApiOperation(value = "调试：使用数据库 token 调用巨量 advertiser/get")
    public Map<String, Object> testAdvertiserGet() throws UnsupportedEncodingException {
        Map<String, Object> result = new HashMap<>();

        JsonNode adsResp = openApiClient.getAdvertiserByToken(OceanChannelCode.OCEANENGINE_ADS);
        JsonNode qcResp  = openApiClient.getAdvertiserByToken(OceanChannelCode.OCEANENGINE_QIANCHUAN);

        result.put("ads", adsResp);
        result.put("qianchuan", qcResp);

        return result;
    }

    @GetMapping("/test")
    @ApiOperation(value = "联通性测试")
    public Map<String, Object> testGet() throws UnsupportedEncodingException {
        Map<String, Object> result = new HashMap<>();
        result.put("res", "yes");
        result.put("code", "ok");

        return result;
    }
}
