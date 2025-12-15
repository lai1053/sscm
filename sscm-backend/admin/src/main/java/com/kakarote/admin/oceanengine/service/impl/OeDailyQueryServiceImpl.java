package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.dto.DailyCostRow;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.mapper.QcOeAdsAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeQcAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.service.OeDailyQueryService;
import com.kakarote.admin.oceanengine.config.OceanEngineConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一封装 ADS/QC 日报查询入口，所有业务侧应通过本 Service 获取消耗/明细，避免散落的 SQL 或直连开放平台。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeDailyQueryServiceImpl implements OeDailyQueryService {

    private final QcOeAdsAdvertiserDailyMapper adsDailyMapper;
    private final QcOeQcAdvertiserDailyMapper qcDailyMapper;
    private final QcOeAdvertiserMapper advertiserMapper;

    @Override
    public BigDecimal sumAdsCostByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate) {
        if (invalidRange(advertiserId, startDate, endDate)) {
            return BigDecimal.ZERO;
        }
        BigDecimal val = adsDailyMapper.sumCostByAdvertiserAndDateRange(
                advertiserId, Date.valueOf(startDate), Date.valueOf(endDate));
        return val == null ? BigDecimal.ZERO : val;
    }

    @Override
    public BigDecimal sumQcCostByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate) {
        if (invalidRange(advertiserId, startDate, endDate)) {
            return BigDecimal.ZERO;
        }
        BigDecimal val = qcDailyMapper.sumCostByAdvertiserAndDateRange(
                advertiserId, Date.valueOf(startDate), Date.valueOf(endDate));
        return val == null ? BigDecimal.ZERO : val;
    }

    @Override
    public BigDecimal sumAdsCostByCompany(Long advCompanyId, LocalDate startDate, LocalDate endDate) {
        if (invalidRange(advCompanyId, startDate, endDate)) {
            return BigDecimal.ZERO;
        }
        List<Long> advertiserIds = listAdvertiserIdsByChannelAndCompany(OceanEngineConstants.CHANNEL_ADS, advCompanyId);
        if (CollectionUtils.isEmpty(advertiserIds)) {
            return BigDecimal.ZERO;
        }
        BigDecimal val = adsDailyMapper.sumCostByAdvertisersAndDateRange(
                advertiserIds, Date.valueOf(startDate), Date.valueOf(endDate));
        return val == null ? BigDecimal.ZERO : val;
    }

    @Override
    public BigDecimal sumQcCostByCompany(Long advCompanyId, LocalDate startDate, LocalDate endDate) {
        if (invalidRange(advCompanyId, startDate, endDate)) {
            return BigDecimal.ZERO;
        }
        List<Long> advertiserIds = listAdvertiserIdsByChannelAndCompany(OceanEngineConstants.CHANNEL_QIANCHUAN, advCompanyId);
        if (CollectionUtils.isEmpty(advertiserIds)) {
            return BigDecimal.ZERO;
        }
        BigDecimal val = qcDailyMapper.sumCostByAdvertisersAndDateRange(
                advertiserIds, Date.valueOf(startDate), Date.valueOf(endDate));
        return val == null ? BigDecimal.ZERO : val;
    }

    @Override
    public List<DailyCostRow> listQcDailyByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate) {
        if (invalidRange(advertiserId, startDate, endDate)) {
            return Collections.emptyList();
        }
        List<DailyCostRow> rows = qcDailyMapper.listDailyByAdvertiserAndDateRange(
                advertiserId, Date.valueOf(startDate), Date.valueOf(endDate));
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public List<DailyCostRow> listAdsDailyByAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate) {
        if (invalidRange(advertiserId, startDate, endDate)) {
            return Collections.emptyList();
        }
        List<DailyCostRow> rows = adsDailyMapper.listDailyByAdvertiserAndDateRange(
                advertiserId, Date.valueOf(startDate), Date.valueOf(endDate));
        return rows == null ? Collections.emptyList() : rows;
    }

    private boolean invalidRange(Long id, LocalDate startDate, LocalDate endDate) {
        return id == null || startDate == null || endDate == null;
    }

    private List<Long> listAdvertiserIdsByChannelAndCompany(String channel, Long advCompanyId) {
        List<QcOeAdvertiser> advertisers = advertiserMapper.selectList(
                new LambdaQueryWrapper<QcOeAdvertiser>()
                        .eq(QcOeAdvertiser::getChannel, channel)
                        .eq(QcOeAdvertiser::getIsDeleted, 0)
                        .eq(QcOeAdvertiser::getAdvCompanyId, advCompanyId)
                        .select(QcOeAdvertiser::getAdvertiserId)
        );
        if (CollectionUtils.isEmpty(advertisers)) {
            return Collections.emptyList();
        }
        return advertisers.stream()
                .map(QcOeAdvertiser::getAdvertiserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
    }
}
