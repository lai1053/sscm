package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.admin.oceanengine.config.OceanEngineConstants;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeDailyBackfillService;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日报历史回补：统一调用 ADS/QC 同步服务，以本地日报表为准，幂等 upsert。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeDailyBackfillServiceImpl implements OeDailyBackfillService {

    private static final int WINDOW_DAYS = 90;

    private final OeAdsDailyReportSyncService adsSyncService;
    private final OeQianchuanDailyReportSyncService qcSyncService;
    private final QcOeAdvertiserMapper advertiserMapper;

    @Override
    public void backfillRecent90DaysForAdvertiser(Long advertiserId) {
        if (advertiserId == null || advertiserId <= 0) {
            log.warn("[BACKFILL] advertiserId is null/invalid");
            return;
        }
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(WINDOW_DAYS - 1L);
        log.info("[BACKFILL] advertiserId={} range={}~{}", advertiserId, startDate, endDate);
        adsSyncService.syncAdsDailyForAdvertiser(advertiserId, startDate, endDate);
        qcSyncService.syncQcDailyForAdvertiser(advertiserId, startDate, endDate);
    }

    @Override
    public void backfillRecent90DaysForCompany(Long advCompanyId) {
        if (advCompanyId == null || advCompanyId <= 0) {
            log.warn("[BACKFILL] advCompanyId is null/invalid");
            return;
        }
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(WINDOW_DAYS - 1L);

        List<QcOeAdvertiser> adsList = listByChannelAndCompany(OceanEngineConstants.CHANNEL_ADS, advCompanyId);
        List<QcOeAdvertiser> qcList = listByChannelAndCompany(OceanEngineConstants.CHANNEL_QIANCHUAN, advCompanyId);

        log.info("[BACKFILL] company={} adsAdvertisers={} qcAdvertisers={} range={}~{}",
                advCompanyId, adsList.size(), qcList.size(), startDate, endDate);

        for (Long advertiserId : adsList.stream().map(QcOeAdvertiser::getAdvertiserId).collect(Collectors.toList())) {
            adsSyncService.syncAdsDailyForAdvertiser(advertiserId, startDate, endDate);
        }
        for (Long advertiserId : qcList.stream().map(QcOeAdvertiser::getAdvertiserId).collect(Collectors.toList())) {
            qcSyncService.syncQcDailyForAdvertiser(advertiserId, startDate, endDate);
        }
    }

    @Override
    public DailyRangeSyncResult syncRangeForAll(LocalDate startDate, LocalDate endDate) {
        DailyRangeSyncResult result = new DailyRangeSyncResult();
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            log.warn("[BACKFILL_RANGE] invalid range, startDate={}, endDate={}", startDate, endDate);
            return result;
        }
        List<Long> adsAdvertisers = listByChannel(OceanEngineConstants.CHANNEL_ADS);
        List<Long> qcAdvertisers = listByChannel(OceanEngineConstants.CHANNEL_QIANCHUAN);
        result.setAdsAdvertiserCount(adsAdvertisers.size());
        result.setQcAdvertiserCount(qcAdvertisers.size());
        if (!CollectionUtils.isEmpty(adsAdvertisers)) {
            result.setAdsResult(adsSyncService.syncAdsDaily(startDate, endDate, adsAdvertisers));
        }
        if (!CollectionUtils.isEmpty(qcAdvertisers)) {
            result.setQcResult(qcSyncService.syncQcDaily(startDate, endDate, qcAdvertisers));
        }
        return result;
    }

    private List<QcOeAdvertiser> listByChannelAndCompany(String channel, Long advCompanyId) {
        List<QcOeAdvertiser> list = advertiserMapper.selectList(
                new LambdaQueryWrapper<QcOeAdvertiser>()
                        .eq(QcOeAdvertiser::getChannel, channel)
                        .eq(QcOeAdvertiser::getAdvCompanyId, advCompanyId)
                        .eq(QcOeAdvertiser::getIsDeleted, 0)
                        .select(QcOeAdvertiser::getAdvertiserId)
        );
        return CollectionUtils.isEmpty(list) ? java.util.Collections.emptyList() : list;
    }

    private List<Long> listByChannel(String channel) {
        List<QcOeAdvertiser> list = advertiserMapper.selectList(
                new LambdaQueryWrapper<QcOeAdvertiser>()
                        .eq(QcOeAdvertiser::getChannel, channel)
                        .eq(QcOeAdvertiser::getIsDeleted, 0)
                        .select(QcOeAdvertiser::getAdvertiserId)
        );
        if (CollectionUtils.isEmpty(list)) {
            return java.util.Collections.emptyList();
        }
        return list.stream()
                .map(QcOeAdvertiser::getAdvertiserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
    }
}
