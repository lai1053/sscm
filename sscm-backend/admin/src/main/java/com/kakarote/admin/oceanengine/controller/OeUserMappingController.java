package com.kakarote.admin.oceanengine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.auth.OeAuthChecker;
import com.kakarote.admin.oceanengine.auth.OeAuthConst;
import com.kakarote.admin.oceanengine.entity.PO.OeUserMapping;
import com.kakarote.admin.oceanengine.mapper.OeUserMappingMapper;
import com.kakarote.admin.oceanengine.model.OeUserBindRequest;
import com.kakarote.admin.oceanengine.model.OeUserMappingVO;
import com.kakarote.admin.oceanengine.service.OeIdentityService;
import com.kakarote.core.common.Result;
import com.kakarote.core.common.SystemCodeEnum;
import com.kakarote.core.exception.CrmException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户与巨量销售用户绑定管理。
 */
@RestController
@RequestMapping("/adminApi/qc/oe/user-mapping")
@Api(tags = "巨量对接接口-用户绑定")
@RequiredArgsConstructor
public class OeUserMappingController {

    private final OeUserMappingMapper userMappingMapper;
    private final OeIdentityService oeIdentityService;

    @GetMapping("/list")
    @ApiOperation("查询绑定列表")
    public Result<List<OeUserMappingVO>> list(
            @ApiParam("WuKong用户ID") @RequestParam(value = "adminUserId", required = false) Long adminUserId,
            @ApiParam("巨量销售用户ID") @RequestParam(value = "oeSaleUserId", required = false) Long oeSaleUserId) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_USER_BIND);
        LambdaQueryWrapper<OeUserMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(adminUserId != null, OeUserMapping::getAdminUserId, adminUserId);
        wrapper.eq(oeSaleUserId != null, OeUserMapping::getOeSaleUserId, oeSaleUserId);
        List<OeUserMapping> mappings = userMappingMapper.selectList(wrapper);
        if (mappings == null) {
            mappings = new ArrayList<>();
        }
        List<OeUserMappingVO> voList = mappings.stream().map(this::toVO).collect(Collectors.toList());
        return Result.ok(voList);
    }

    @PostMapping("/bind")
    @ApiOperation("绑定或更新用户与巨量销售用户关系")
    public Result<Void> bind(@RequestBody OeUserBindRequest request) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_USER_BIND);
        if (request == null || request.getAdminUserId() == null || request.getAdminUserId() <= 0
                || request.getOeSaleUserId() == null || request.getOeSaleUserId() <= 0) {
            throw new CrmException(SystemCodeEnum.SYSTEM_NO_VALID, "adminUserId 或 oeSaleUserId 无效");
        }
        oeIdentityService.bindUser(request.getAdminUserId(), request.getOeSaleUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("解除绑定关系")
    public Result<Void> delete(@ApiParam(value = "绑定关系ID", required = true) @PathVariable("id") Long id) {
        OeAuthChecker.checkPermission(OeAuthConst.OE_USER_BIND);
        if (id == null || id <= 0) {
            throw new CrmException(SystemCodeEnum.SYSTEM_NO_VALID, "id 无效");
        }
        userMappingMapper.deleteById(id);
        return Result.ok();
    }

    private OeUserMappingVO toVO(OeUserMapping mapping) {
        OeUserMappingVO vo = new OeUserMappingVO();
        vo.setId(mapping.getId());
        vo.setAdminUserId(mapping.getAdminUserId());
        vo.setOeSaleUserId(mapping.getOeSaleUserId());
        vo.setOperatorId(mapping.getOperatorId());
        vo.setCreateTime(mapping.getCreateTime());
        return vo;
    }
}
