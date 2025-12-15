package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.entity.QcOeAgent;
import com.kakarote.admin.oceanengine.entity.QcOeCompany;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiserSaleHistory;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeAgentMapper;
import com.kakarote.admin.oceanengine.service.IQcOeAdvertiserService;
import com.kakarote.admin.oceanengine.service.IQcOeAgentService;
import com.kakarote.admin.oceanengine.service.IQcOeCompanyService;
import com.kakarote.admin.oceanengine.service.IQcOeAdvertiserSaleHistoryService;
import com.kakarote.admin.oceanengine.service.OeAdvertiserSyncService;
import com.kakarote.admin.oceanengine.service.OeAdvertiserSaleBindService;
import com.kakarote.admin.oceanengine.service.impl.OeAccountDiscoveryServiceImpl.AdvertiserDetail;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 将发现到的代理商/广告主信息落库，保持幂等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeAdvertiserSyncServiceImpl implements OeAdvertiserSyncService {

    private final IQcOeAgentService agentService;
    private final IQcOeAdvertiserService advertiserService;
    private final IQcOeCompanyService companyService;
    private final IQcOeAdvertiserSaleHistoryService advertiserSaleHistoryService;
    private final QcOeAgentMapper agentMapper;
    private final QcOeAdvertiserMapper advertiserMapper;
    private final OeAdvertiserSaleBindService advertiserSaleBindService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OeAdvertiserSyncResult syncAdvertisers(List<AdvertiserDetail> details) {
        OeAdvertiserSyncResult result = new OeAdvertiserSyncResult();
        if (details == null || details.isEmpty()) {
            return result;
        }
        Date now = new Date();
        result.setTotalCount(details.size());

        for (AdvertiserDetail detail : details) {
            // upsert agent
            if (detail.getAgentId() != null) {
                QcOeAgent agent = agentMapper.selectOne(
                        new LambdaQueryWrapper<QcOeAgent>().eq(QcOeAgent::getAgentId, detail.getAgentId())
                );
                if (agent == null) {
                    agent = buildAgent(detail, now);
                    if (agentMapper.insert(agent) > 0) {
                        result.setAgentInsertCount(result.getAgentInsertCount() + 1);
                    }
                } else {
                    updateAgent(agent, detail, now);
                    agentMapper.updateById(agent);
                    result.setAgentUpdateCount(result.getAgentUpdateCount() + 1);
                }
            }

            // upsert advertiser
            if (detail.getAdvertiserId() == null) {
                continue;
            }
            Long companyId = upsertCompany(detail, now);
            QcOeAdvertiser advertiser = advertiserMapper.selectOne(
                    new LambdaQueryWrapper<QcOeAdvertiser>().eq(QcOeAdvertiser::getAdvertiserId, detail.getAdvertiserId())
            );
            if (advertiser == null) {
                advertiser = buildAdvertiser(detail, now, companyId);
                advertiserSaleBindService.bindSaleUserForAdvertiser(advertiser);
                if (advertiserMapper.insert(advertiser) > 0) {
                    result.setInsertCount(result.getInsertCount() + 1);
                }
            } else {
                Long prevSaleId = advertiser.getSaleId();
                String prevSaleName = advertiser.getSaleName();
                boolean saleChanged = isSaleChanged(prevSaleId, prevSaleName, detail);
                updateAdvertiser(advertiser, detail, now, companyId);
                if (saleChanged) {
                    recordSaleChange(detail, prevSaleId, prevSaleName, now);
                    advertiser.setSaleUserId(null);
                }
                advertiserSaleBindService.bindSaleUserForAdvertiser(advertiser);
                advertiserMapper.updateById(advertiser);
                result.setUpdateCount(result.getUpdateCount() + 1);
            }
        }
        log.info("[OE_SYNC] upsert advertisers total={}, insert={}, update={}, agentInsert={}, agentUpdate={}",
                result.getTotalCount(), result.getInsertCount(), result.getUpdateCount(),
                result.getAgentInsertCount(), result.getAgentUpdateCount());
        return result;
    }

    private QcOeAgent buildAgent(AdvertiserDetail detail, Date now) {
        QcOeAgent agent = new QcOeAgent();
        agent.setAgentId(detail.getAgentId());
        agent.setAgentName(detail.getAgentName());
        agent.setCompanyId(detail.getAgentCompanyId());
        agent.setCompanyName(detail.getAgentCompanyName());
        agent.setGmtCreate(now);
        agent.setGmtModified(now);
        agent.setIsDeleted(0);
        return agent;
    }

    private void updateAgent(QcOeAgent agent, AdvertiserDetail detail, Date now) {
        agent.setAgentName(detail.getAgentName());
        agent.setCompanyId(detail.getAgentCompanyId());
        agent.setCompanyName(detail.getAgentCompanyName());
        agent.setGmtModified(now);
    }

    private QcOeAdvertiser buildAdvertiser(AdvertiserDetail detail, Date now) {
        QcOeAdvertiser advertiser = new QcOeAdvertiser();
        applyAdvertiser(advertiser, detail, now);
        advertiser.setGmtCreate(now);
        advertiser.setIsDeleted(0);
        return advertiser;
    }

    private QcOeAdvertiser buildAdvertiser(AdvertiserDetail detail, Date now, Long companyId) {
        QcOeAdvertiser advertiser = buildAdvertiser(detail, now);
        advertiser.setCompanyId(companyId);
        return advertiser;
    }

    private void updateAdvertiser(QcOeAdvertiser advertiser, AdvertiserDetail detail, Date now, Long companyId) {
        applyAdvertiser(advertiser, detail, now);
        advertiser.setCompanyId(companyId);
    }

    private void applyAdvertiser(QcOeAdvertiser advertiser, AdvertiserDetail detail, Date now) {
        advertiser.setAdvertiserId(detail.getAdvertiserId());
        advertiser.setChannel(detail.getChannel());
        advertiser.setAdvertiserName(detail.getAdvertiserName());
        advertiser.setAdvertiserStatus(detail.getAdvertiserStatus());
        advertiser.setAdvCompanyId(detail.getAdvCompanyId());
        advertiser.setAdvCompanyName(detail.getAdvCompanyName());
        advertiser.setFirstAgentId(detail.getAgentId());
        advertiser.setFirstAgentName(detail.getAgentName());
        advertiser.setFirstAgentCompanyId(detail.getAgentCompanyId());
        advertiser.setFirstAgentCompanyName(detail.getAgentCompanyName());
        advertiser.setFirstIndustryName(detail.getFirstIndustryName());
        advertiser.setSecondIndustryName(detail.getSecondIndustryName());
        advertiser.setAccountSource(detail.getAccountSource());
        advertiser.setSaleId(detail.getSaleId());
        advertiser.setSaleName(detail.getSaleName());
        advertiser.setLastSyncTime(now);
        advertiser.setGmtModified(now);
    }

    private boolean isSaleChanged(Long prevSaleId, String prevSaleName, AdvertiserDetail detail) {
        Long newSaleId = detail.getSaleId();
        if (newSaleId == null || newSaleId <= 0) {
            return false;
        }
        return !Objects.equals(prevSaleId, newSaleId) || !Objects.equals(prevSaleName, detail.getSaleName());
    }

    private void recordSaleChange(AdvertiserDetail detail, Long prevSaleId, String prevSaleName, Date now) {
        QcOeAdvertiserSaleHistory history = new QcOeAdvertiserSaleHistory();
        history.setAdvertiserId(detail.getAdvertiserId());
        history.setPrevSaleId(prevSaleId);
        history.setPrevSaleName(prevSaleName);
        history.setNewSaleId(detail.getSaleId());
        history.setNewSaleName(detail.getSaleName());
        history.setChangeTime(now);
        history.setChangeSource("SYNC");
        history.setGmtCreate(now);
        history.setGmtModified(now);
        history.setIsDeleted(0);
        advertiserSaleHistoryService.save(history);
    }

    private Long upsertCompany(AdvertiserDetail detail, Date now) {
        if (detail.getAdvCompanyId() == null) {
            return null;
        }
        QcOeCompany company = companyService.lambdaQuery()
                .eq(QcOeCompany::getAdvCompanyId, detail.getAdvCompanyId())
                .one();
        if (company == null) {
            company = new QcOeCompany();
            company.setAdvCompanyId(detail.getAdvCompanyId());
            company.setAdvCompanyName(detail.getAdvCompanyName());
            company.setFirstIndustryName(detail.getFirstIndustryName());
            company.setSecondIndustryName(detail.getSecondIndustryName());
            company.setGmtCreate(now);
            company.setGmtModified(now);
            company.setIsDeleted(0);
            companyService.save(company);
            return company.getId();
        }

        boolean needUpdate = false;
        if (detail.getAdvCompanyName() != null && !detail.getAdvCompanyName().equals(company.getAdvCompanyName())) {
            company.setAdvCompanyName(detail.getAdvCompanyName());
            needUpdate = true;
        }
        if (detail.getFirstIndustryName() != null && !detail.getFirstIndustryName().equals(company.getFirstIndustryName())) {
            company.setFirstIndustryName(detail.getFirstIndustryName());
            needUpdate = true;
        }
        if (detail.getSecondIndustryName() != null && !detail.getSecondIndustryName().equals(company.getSecondIndustryName())) {
            company.setSecondIndustryName(detail.getSecondIndustryName());
            needUpdate = true;
        }
        if (needUpdate) {
            company.setGmtModified(now);
            companyService.updateById(company);
        }
        return company.getId();
    }

    @Data
    public static class OeAdvertiserSyncResult {
        private int totalCount;
        private int insertCount;
        private int updateCount;
        private int agentInsertCount;
        private int agentUpdateCount;
    }
}
