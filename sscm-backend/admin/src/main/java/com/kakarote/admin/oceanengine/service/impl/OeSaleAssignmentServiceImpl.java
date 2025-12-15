package com.kakarote.admin.oceanengine.service.impl;

import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.service.OeSaleAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 销售归属计算服务，当前实现直接返回当前归属。
 * TODO: 后续可回放 sale history 根据变更时间获取某日归属。
 */
@Service
@RequiredArgsConstructor
public class OeSaleAssignmentServiceImpl implements OeSaleAssignmentService {

    private final QcOeAdvertiserMapper advertiserMapper;

    @Override
    public Long resolveSaleUserIdForDate(Long advertiserId, LocalDate statDate) {
        if (advertiserId == null) {
            return null;
        }
        QcOeAdvertiser advertiser = advertiserMapper.selectById(advertiserId);
        if (advertiser == null) {
            return null;
        }
        // 当前实现：返回当前 sale_user_id；后续可依据 sale history 回放到 statDate 时刻的归属。
        return advertiser.getSaleUserId();
    }
}
