package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kakarote.admin.oceanengine.config.OceanEngineConstants;
import com.kakarote.admin.oceanengine.entity.QcOeAgentAdvertiserRel;
import com.kakarote.admin.oceanengine.mapper.QcOeAgentAdvertiserRelMapper;
import com.kakarote.admin.oceanengine.service.OeAgentAdvertiserRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代理-广告主关系维护：持久化 agent/advertiser/select 结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeAgentAdvertiserRelationServiceImpl implements OeAgentAdvertiserRelationService {

    private final QcOeAgentAdvertiserRelMapper relMapper;

    @Override
    public void syncRelations(Long rootAdvertiserId,
                              String rootAccountType,
                              String rootAccountRole,
                              String accountSource,
                              List<Long> advertiserIds,
                              String channel) {
        if (rootAdvertiserId == null || !StringUtils.hasText(accountSource)) {
            log.warn("[OE_REL] skip sync, rootAdvertiserId or accountSource missing, root={}, source={}",
                    rootAdvertiserId, accountSource);
            return;
        }
        String safeChannel = StringUtils.hasText(channel) ? channel : OceanEngineConstants.CHANNEL_ADS;
        String safeAccountType = rootAccountType == null ? "" : rootAccountType;
        String safeAccountRole = rootAccountRole == null ? "" : rootAccountRole;
        List<Long> currentIds = CollectionUtils.isEmpty(advertiserIds)
                ? Collections.emptyList()
                : advertiserIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Date now = new Date();
        List<QcOeAgentAdvertiserRel> existing = relMapper.selectList(
                new LambdaQueryWrapper<QcOeAgentAdvertiserRel>()
                        .eq(QcOeAgentAdvertiserRel::getRootAdvertiserId, rootAdvertiserId)
                        .eq(QcOeAgentAdvertiserRel::getAccountSource, accountSource)
                        .eq(QcOeAgentAdvertiserRel::getChannel, safeChannel)
                        .eq(QcOeAgentAdvertiserRel::getIsDeleted, 0)
        );
        Map<Long, QcOeAgentAdvertiserRel> existingMap = existing.stream()
                .collect(Collectors.toMap(QcOeAgentAdvertiserRel::getAdvertiserId, r -> r, (a, b) -> a));

        // Upsert current list
        for (Long advertiserId : currentIds) {
            QcOeAgentAdvertiserRel rel = existingMap.get(advertiserId);
            if (rel == null) {
                QcOeAgentAdvertiserRel insert = new QcOeAgentAdvertiserRel();
                insert.setRootAdvertiserId(rootAdvertiserId);
                insert.setRootAccountType(safeAccountType);
                insert.setRootAccountRole(safeAccountRole);
                insert.setAdvertiserId(advertiserId);
                insert.setAccountSource(accountSource);
                insert.setChannel(safeChannel);
                insert.setIsActive(1);
                insert.setFirstSeen(now);
                insert.setLastSeen(now);
                insert.setLastSyncTime(now);
                insert.setGmtCreate(now);
                insert.setGmtModified(now);
                insert.setIsDeleted(0);
                relMapper.insert(insert);
            } else {
                QcOeAgentAdvertiserRel update = new QcOeAgentAdvertiserRel();
                update.setId(rel.getId());
                update.setRootAccountType(safeAccountType);
                update.setRootAccountRole(safeAccountRole);
                update.setIsActive(1);
                update.setLastSeen(now);
                update.setLastSyncTime(now);
                update.setGmtModified(now);
                relMapper.updateById(update);
            }
        }

        // Mark missing as inactive
        if (!CollectionUtils.isEmpty(existing)) {
            Set<Long> currentSet = new HashSet<>(currentIds);
            List<Long> toDeactivate = existing.stream()
                    .map(QcOeAgentAdvertiserRel::getAdvertiserId)
                    .filter(id -> !currentSet.contains(id))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toDeactivate)) {
                relMapper.update(null, new LambdaUpdateWrapper<QcOeAgentAdvertiserRel>()
                        .set(QcOeAgentAdvertiserRel::getIsActive, 0)
                        .set(QcOeAgentAdvertiserRel::getLastSyncTime, now)
                        .set(QcOeAgentAdvertiserRel::getGmtModified, now)
                        .eq(QcOeAgentAdvertiserRel::getRootAdvertiserId, rootAdvertiserId)
                        .eq(QcOeAgentAdvertiserRel::getAccountSource, accountSource)
                        .eq(QcOeAgentAdvertiserRel::getChannel, safeChannel)
                        .eq(QcOeAgentAdvertiserRel::getIsDeleted, 0)
                        .in(QcOeAgentAdvertiserRel::getAdvertiserId, toDeactivate));
            }
        }
    }
}
