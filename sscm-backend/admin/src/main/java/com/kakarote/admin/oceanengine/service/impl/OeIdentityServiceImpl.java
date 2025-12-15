package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.entity.PO.OeUserMapping;
import com.kakarote.admin.oceanengine.mapper.OeUserMappingMapper;
import com.kakarote.admin.oceanengine.service.OeIdentityService;
import com.kakarote.core.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OceanEngine 身份绑定服务实现。
 */
@Service
@RequiredArgsConstructor
public class OeIdentityServiceImpl implements OeIdentityService {

    private final OeUserMappingMapper userMappingMapper;

    @Override
    public Long getCurrentOeSaleUserId() {
        Long adminUserId = UserUtil.getUserId();
        return getOeSaleUserIdByAdminUserId(adminUserId);
    }

    @Override
    public Long getOeSaleUserIdByAdminUserId(Long adminUserId) {
        if (adminUserId == null) {
            return null;
        }
        OeUserMapping mapping = userMappingMapper.selectOne(new LambdaQueryWrapper<OeUserMapping>()
                .eq(OeUserMapping::getAdminUserId, adminUserId)
                .last("limit 1"));
        return mapping == null ? null : mapping.getOeSaleUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindUser(Long adminUserId, Long oeSaleUserId) {
        if (adminUserId == null || oeSaleUserId == null) {
            return;
        }
        OeUserMapping exist = userMappingMapper.selectOne(new LambdaQueryWrapper<OeUserMapping>()
                .eq(OeUserMapping::getAdminUserId, adminUserId)
                .last("limit 1"));
        Long operatorId = UserUtil.getUserId();
        if (exist == null) {
            OeUserMapping mapping = new OeUserMapping();
            mapping.setAdminUserId(adminUserId);
            mapping.setOeSaleUserId(oeSaleUserId);
            mapping.setOperatorId(operatorId);
            userMappingMapper.insert(mapping);
            return;
        }
        exist.setOeSaleUserId(oeSaleUserId);
        exist.setOperatorId(operatorId);
        userMappingMapper.updateById(exist);
    }
}
