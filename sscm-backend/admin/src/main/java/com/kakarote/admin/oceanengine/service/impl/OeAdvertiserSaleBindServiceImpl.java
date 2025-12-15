package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.entity.QcOeSaleUser;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeSaleUserMapper;
import com.kakarote.admin.oceanengine.service.OeAdvertiserSaleBindService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OeAdvertiserSaleBindServiceImpl implements OeAdvertiserSaleBindService {

    private final QcOeAdvertiserMapper advertiserMapper;
    private final QcOeSaleUserMapper saleUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void backfillSaleUserId() {
        int batchSize = 1000;
        int offset = 0;
        int totalHandled = 0;
        int bound = 0;
        List<QcOeAdvertiser> batch;
        do {
            batch = advertiserMapper.selectList(new LambdaQueryWrapper<QcOeAdvertiser>()
                    .isNull(QcOeAdvertiser::getSaleUserId)
                    .isNotNull(QcOeAdvertiser::getSaleId)
                    .gt(QcOeAdvertiser::getSaleId, 0)
                    .eq(QcOeAdvertiser::getIsDeleted, 0)
                    .last("limit " + batchSize + " offset " + offset));
            if (batch == null || batch.isEmpty()) {
                break;
            }
            totalHandled += batch.size();
            for (QcOeAdvertiser advertiser : batch) {
                boolean changed = bindSaleUser(advertiser);
                if (changed) {
                    bound++;
                    advertiser.setGmtModified(new Date());
                    advertiserMapper.updateById(advertiser);
                }
            }
            offset += batchSize;
        } while (batch.size() == batchSize);

        log.info("[OE_BIND] backfill finished, handled={}, bound={}", totalHandled, bound);
    }

    @Override
    public void bindSaleUserForAdvertiser(QcOeAdvertiser advertiser) {
        if (advertiser == null) {
            return;
        }
        if (advertiser.getSaleUserId() != null) {
            return;
        }
        boolean changed = bindSaleUser(advertiser);
        if (changed) {
            advertiser.setGmtModified(new Date());
        }
    }

    private boolean bindSaleUser(QcOeAdvertiser advertiser) {
        if (advertiser.getSaleId() == null || advertiser.getSaleId() <= 0) {
            return false;
        }
        QcOeSaleUser saleUser = saleUserMapper.selectOne(
                new LambdaQueryWrapper<QcOeSaleUser>()
                        .eq(QcOeSaleUser::getSaleId, advertiser.getSaleId())
                        .eq(QcOeSaleUser::getIsDeleted, 0)
                        .last("limit 1")
        );
        if (saleUser == null) {
            return false;
        }
        advertiser.setSaleUserId(saleUser.getId());
        return true;
    }
}
