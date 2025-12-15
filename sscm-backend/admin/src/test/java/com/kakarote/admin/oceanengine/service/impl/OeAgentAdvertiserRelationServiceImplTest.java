package com.kakarote.admin.oceanengine.service.impl;

import com.kakarote.admin.oceanengine.entity.QcOeAgentAdvertiserRel;
import com.kakarote.admin.oceanengine.mapper.QcOeAgentAdvertiserRelMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OeAgentAdvertiserRelationServiceImplTest {

    @Mock
    private QcOeAgentAdvertiserRelMapper relMapper;

    @Test
    void shouldSkipWhenRootOrSourceMissing() {
        OeAgentAdvertiserRelationServiceImpl service = new OeAgentAdvertiserRelationServiceImpl(relMapper);

        service.syncRelations(null, "type", "role", "AD", Collections.singletonList(1L), "ADS");
        service.syncRelations(1L, "type", "role", null, Collections.singletonList(1L), "ADS");

        verifyNoInteractions(relMapper);
    }

    @Test
    void shouldInsertFirstTime() {
        when(relMapper.selectList(any())).thenReturn(Collections.emptyList());
        ArgumentCaptor<QcOeAgentAdvertiserRel> insertCaptor = ArgumentCaptor.forClass(QcOeAgentAdvertiserRel.class);

        OeAgentAdvertiserRelationServiceImpl service = new OeAgentAdvertiserRelationServiceImpl(relMapper);
        service.syncRelations(100L, "AGENT", "ROOT", "AD", Arrays.asList(11L, 11L), null);

        verify(relMapper).selectList(any());
        verify(relMapper).insert(insertCaptor.capture());
        verify(relMapper, never()).updateById(any());
        verify(relMapper, never()).update(any(), any());

        QcOeAgentAdvertiserRel inserted = insertCaptor.getValue();
        assertThat(inserted.getRootAdvertiserId()).isEqualTo(100L);
        assertThat(inserted.getAccountSource()).isEqualTo("AD");
        assertThat(inserted.getChannel()).isEqualTo("ADS");
        assertThat(inserted.getAdvertiserId()).isEqualTo(11L);
        assertThat(inserted.getIsActive()).isEqualTo(1);
    }

    @Test
    void shouldUpsertAndUpdateExisting() {
        QcOeAgentAdvertiserRel existing = new QcOeAgentAdvertiserRel();
        existing.setId(10L);
        existing.setRootAdvertiserId(100L);
        existing.setAdvertiserId(11L);
        existing.setAccountSource("AD");
        existing.setChannel("ADS");
        existing.setIsActive(1);

        when(relMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        ArgumentCaptor<QcOeAgentAdvertiserRel> insertCaptor = ArgumentCaptor.forClass(QcOeAgentAdvertiserRel.class);
        ArgumentCaptor<QcOeAgentAdvertiserRel> updateCaptor = ArgumentCaptor.forClass(QcOeAgentAdvertiserRel.class);

        OeAgentAdvertiserRelationServiceImpl service = new OeAgentAdvertiserRelationServiceImpl(relMapper);
        service.syncRelations(100L, "AGENT", "ROOT", "AD", Arrays.asList(11L, 12L), "ADS");

        verify(relMapper).selectList(any());
        verify(relMapper).insert(insertCaptor.capture());
        verify(relMapper).updateById(updateCaptor.capture());
        verify(relMapper, never()).update(any(), any());

        QcOeAgentAdvertiserRel inserted = insertCaptor.getValue();
        assertThat(inserted.getAdvertiserId()).isEqualTo(12L);
        assertThat(inserted.getIsActive()).isEqualTo(1);

        QcOeAgentAdvertiserRel updated = updateCaptor.getValue();
        assertThat(updated.getId()).isEqualTo(10L);
        assertThat(updated.getIsActive()).isEqualTo(1);
        assertThat(updated.getRootAccountType()).isEqualTo("AGENT");
        assertThat(updated.getRootAccountRole()).isEqualTo("ROOT");
    }

    @Test
    @Disabled
    void shouldDeactivateMissingAdvertisers() {
        QcOeAgentAdvertiserRel existing = new QcOeAgentAdvertiserRel();
        existing.setId(10L);
        existing.setRootAdvertiserId(100L);
        existing.setAdvertiserId(11L);
        existing.setAccountSource("AD");
        existing.setChannel("ADS");
        existing.setIsActive(1);

        when(relMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        OeAgentAdvertiserRelationServiceImpl service = new OeAgentAdvertiserRelationServiceImpl(relMapper);
        service.syncRelations(100L, "AGENT", "ROOT", "AD", Collections.emptyList(), "ADS");

        verify(relMapper).selectList(any());
        verify(relMapper, never()).insert(any());
        verify(relMapper, never()).updateById(any());
        verify(relMapper).update(any(), any());
    }
}
