package com.kakarote.admin.oceanengine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.entity.PO.AdminUser;
import com.kakarote.admin.entity.PO.AdminUserRole;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.entity.QcOeSaleUser;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeSaleUserMapper;
import com.kakarote.admin.oceanengine.service.IQcOeSaleUserService;
import com.kakarote.admin.oceanengine.service.OeSaleUserProvisionService;
import com.kakarote.admin.oceanengine.util.PinyinHelper;
import com.kakarote.admin.service.IAdminUserRoleService;
import com.kakarote.admin.service.IAdminUserService;
import com.kakarote.core.utils.UserUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于巨量销售自动创建并绑定悟空用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeSaleUserProvisionServiceImpl implements OeSaleUserProvisionService {

    private static final Integer DEFAULT_DEPT_ID = 14852;
    private static final Integer DEFAULT_SALE_ROLE_ID = 180170;
    private static final String DEFAULT_PASSWORD = "a123456";

    private final QcOeAdvertiserMapper advertiserMapper;
    private final QcOeSaleUserMapper saleUserMapper;
    private final IQcOeSaleUserService saleUserService;
    private final IAdminUserService adminUserService;
    private final IAdminUserRoleService adminUserRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProvisionResult syncSaleUsersFromAdvertisers(@Nullable String channelCode) {
        String channelFilter = mapChannel(channelCode);
        LambdaQueryWrapper<QcOeAdvertiser> wrapper = new LambdaQueryWrapper<QcOeAdvertiser>()
                .isNotNull(QcOeAdvertiser::getSaleId);
        if (channelFilter != null) {
            wrapper.eq(QcOeAdvertiser::getChannel, channelFilter);
        }
        List<QcOeAdvertiser> advertisers = advertiserMapper.selectList(wrapper);
        if (CollUtil.isEmpty(advertisers)) {
            log.warn("[OE_PROVISION] 未发现含 sale_id 的广告主，channel={}", channelCode);
            return new ProvisionResult();
        }
        Map<Long, String> saleMap = advertisers.stream()
                .filter(a -> a.getSaleId() != null && a.getSaleId() > 0)
                .filter(a -> StrUtil.isNotBlank(a.getSaleName()))
                .collect(Collectors.toMap(
                        QcOeAdvertiser::getSaleId,
                        a -> StrUtil.emptyToDefault(a.getSaleName(), ""),
                        (v1, v2) -> StrUtil.isNotBlank(v1) ? v1 : v2
                ));

        ProvisionResult result = new ProvisionResult();
        result.setTotalSales(saleMap.size());

        for (Map.Entry<Long, String> entry : saleMap.entrySet()) {
            Long saleId = entry.getKey();
            String saleName = entry.getValue();
            QcOeSaleUser saleUser = saleUserMapper.selectOne(
                    new LambdaQueryWrapper<QcOeSaleUser>().eq(QcOeSaleUser::getSaleId, saleId)
            );
            Date now = new Date();
            if (saleUser == null) {
                saleUser = new QcOeSaleUser();
                saleUser.setSaleId(saleId);
                saleUser.setSaleName(saleName);
                saleUser.setSource("OCEANENGINE");
                saleUser.setStatus(1);
                saleUser.setGmtCreate(now);
                saleUser.setGmtModified(now);
                saleUser.setIsDeleted(0);
                saleUserMapper.insert(saleUser);
                result.setSaleUserInserted(result.getSaleUserInserted() + 1);
            } else {
                if (StrUtil.isNotBlank(saleName) && !StrUtil.equals(saleName, saleUser.getSaleName())) {
                    saleUser.setSaleName(saleName);
                    saleUser.setGmtModified(now);
                    saleUserMapper.updateById(saleUser);
                    result.setSaleUserUpdated(result.getSaleUserUpdated() + 1);
                }
            }

            if (saleUser.getLoginUserId() == null) {
                Long userId = ensureCrmUserForSale(saleId);
                if (userId != null) {
                    result.setCrmUserCreated(result.getCrmUserCreated() + 1);
                }
            } else {
                result.setSkippedExisting(result.getSkippedExisting() + 1);
            }
        }
        log.info("[OE_PROVISION] 同步完成 totalSales={}, newSaleUser={}, updateSaleUser={}, newCrmUser={}",
                result.getTotalSales(), result.getSaleUserInserted(), result.getSaleUserUpdated(), result.getCrmUserCreated());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long ensureCrmUserForSale(Long saleId) {
        QcOeSaleUser saleUser = saleUserMapper.selectOne(
                new LambdaQueryWrapper<QcOeSaleUser>().eq(QcOeSaleUser::getSaleId, saleId)
        );
        if (saleUser == null) {
            log.warn("[OE_PROVISION] saleId={} 未找到对应的销售档案", saleId);
            return null;
        }
        if (saleUser.getLoginUserId() != null) {
            return saleUser.getLoginUserId();
        }

        String saleName = StrUtil.emptyToDefault(saleUser.getSaleName(), "sale" + saleId);
        String baseUsername = PinyinHelper.toPinyin(saleName).replace(" ", "");
        if (StrUtil.isBlank(baseUsername)) {
            baseUsername = "sale" + saleId;
        }
        String username = nextAvailableUsername(baseUsername);

        String salt = IdUtil.fastSimpleUUID();
        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username);
        adminUser.setRealname(saleName);
        adminUser.setSalt(salt);
        adminUser.setPassword(UserUtil.sign(username + DEFAULT_PASSWORD, salt));
        adminUser.setDeptId(DEFAULT_DEPT_ID);
        adminUser.setStatus(2);
        adminUser.setIsDel(0);
        adminUser.setCreateTime(new Date());
        adminUser.setMobile(nextAvailableMobile());

        adminUserService.save(adminUser);

        AdminUserRole userRole = new AdminUserRole();
        userRole.setUserId(adminUser.getUserId());
        userRole.setRoleId(DEFAULT_SALE_ROLE_ID);
        adminUserRoleService.save(userRole);

        saleUser.setLoginUsername(username);
        saleUser.setLoginUserId(adminUser.getUserId());
        saleUser.setGmtModified(new Date());
        saleUserMapper.updateById(saleUser);

        log.info("[OE_PROVISION] 为 saleId={} 创建并绑定用户 userId={}, username={}", saleId, adminUser.getUserId(), username);
        return adminUser.getUserId();
    }

    private String nextAvailableUsername(String base) {
        String candidate = base;
        int suffix = 1;
        while (adminUserService.lambdaQuery().eq(AdminUser::getUsername, candidate).count() > 0) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String nextAvailableMobile() {
        String mobile;
        do {
            mobile = "199" + RandomUtil.randomNumbers(8);
        } while (adminUserService.lambdaQuery().eq(AdminUser::getMobile, mobile).count() > 0);
        return mobile;
    }

    private String mapChannel(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return null;
        }
        if ("OCEANENGINE_ADS".equalsIgnoreCase(channelCode)) {
            return "ADS";
        }
        if ("OCEANENGINE_QIANCHUAN".equalsIgnoreCase(channelCode)) {
            return "QIANCHUAN";
        }
        return null;
    }

    @Data
    public static class ProvisionResult {
        private int totalSales;
        private int saleUserInserted;
        private int saleUserUpdated;
        private int crmUserCreated;
        private int skippedExisting;
    }
}
