package com.kakarote.admin.oceanengine.service.impl;

import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OeDailyBackfillServiceImplTest {

    @Mock
    private OeAdsDailyReportSyncService adsSyncService;
    @Mock
    private OeQianchuanDailyReportSyncService qcSyncService;
    @Mock
    private QcOeAdvertiserMapper advertiserMapper;

    @Test
    void shouldSkipInvalidAdvertiserId() {
        OeDailyBackfillServiceImpl service = new OeDailyBackfillServiceImpl(adsSyncService, qcSyncService, advertiserMapper);
        service.backfillRecent90DaysForAdvertiser(null);
        service.backfillRecent90DaysForAdvertiser(0L);
        verifyNoInteractions(adsSyncService, qcSyncService);
    }

    @Test
    void shouldCallSyncForAdvertiserWith90DayWindow() {
        OeDailyBackfillServiceImpl service = new OeDailyBackfillServiceImpl(adsSyncService, qcSyncService, advertiserMapper);
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);

        service.backfillRecent90DaysForAdvertiser(123L);

        verify(adsSyncService).syncAdsDailyForAdvertiser(eq(123L), startCaptor.capture(), endCaptor.capture());
        verify(qcSyncService).syncQcDailyForAdvertiser(eq(123L), any(LocalDate.class), any(LocalDate.class));

        LocalDate expectedEnd = LocalDate.now().minusDays(1);
        LocalDate expectedStart = expectedEnd.minusDays(89);
        assertThat(endCaptor.getValue()).isEqualTo(expectedEnd);
        assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
    }

    @Test
    @Disabled
    void shouldIterateAdvertisersByCompany() {
        QcOeAdvertiser adsAdv = new QcOeAdvertiser();
        adsAdv.setAdvertiserId(11L);
        QcOeAdvertiser qcAdv = new QcOeAdvertiser();
        qcAdv.setAdvertiserId(22L);

        when(advertiserMapper.selectList(any())).thenReturn(Arrays.asList(adsAdv), Arrays.asList(qcAdv));

        OeDailyBackfillServiceImpl service = new OeDailyBackfillServiceImpl(adsSyncService, qcSyncService, advertiserMapper);
        service.backfillRecent90DaysForCompany(88L);

        verify(adsSyncService).syncAdsDailyForAdvertiser(eq(11L), any(LocalDate.class), any(LocalDate.class));
        verify(qcSyncService).syncQcDailyForAdvertiser(eq(22L), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void shouldSkipInvalidCompanyId() {
        OeDailyBackfillServiceImpl service = new OeDailyBackfillServiceImpl(adsSyncService, qcSyncService, advertiserMapper);
        service.backfillRecent90DaysForCompany(0L);
        verifyNoInteractions(advertiserMapper, adsSyncService, qcSyncService);
    }
}
