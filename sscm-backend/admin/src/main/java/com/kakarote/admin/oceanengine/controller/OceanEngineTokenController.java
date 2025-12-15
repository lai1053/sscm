package com.kakarote.admin.oceanengine.controller;

import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.service.OceanTokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/oceanengine/token")
@Api(tags = "巨量对接接口")
public class OceanEngineTokenController {

    @Resource
    private OceanTokenService oceanTokenService;

    /**
     * 手动刷新指定 app_auth 的 token
     * 示例：
     *   GET /admin/oceanengine/token/refresh?appAuthId=3
     */
    @GetMapping("/refresh")
    @ApiOperation(value = "刷新指定 appAuth 的巨量 token")
    public Map<String, Object> refreshByAppAuth(@ApiParam(value = "app_auth 主键 ID", required = true) @RequestParam("appAuthId") Long appAuthId) {
        oceanTokenService.refreshTokenByAppAuth(appAuthId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("appAuthId", appAuthId);
        return resp;
    }

    /**
     * 手动刷新某个渠道下所有 app_auth 的 token
     * 示例：
     *   POST /admin/oceanengine/token/refreshAll?channelCode=OCEANENGINE_ADS
     */
    @PostMapping("/refreshAll")
    @ApiOperation(value = "按渠道刷新全部 appAuth 的巨量 token", notes = "channelCode 取值：OCEANENGINE_ADS 或 OCEANENGINE_QIANCHUAN")
    public Map<String, Object> refreshAllByChannel(@ApiParam(value = "渠道编码", required = true) @RequestParam("channelCode") String channelCode) {
        OceanChannelCode codeEnum = OceanChannelCode.fromCode(channelCode);
        oceanTokenService.refreshAllTokensForChannel(codeEnum);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("channelCode", codeEnum.getCode());
        return resp;
    }


    @GetMapping("/test")
    @ApiOperation(value = "联通性测试", notes = "开发联调自测使用")
    public Map<String, Object> testGet()  {
        Map<String, Object> result = new HashMap<>();
        result.put("res", "yes");
        result.put("code", "ok");

        return result;
    }
}
