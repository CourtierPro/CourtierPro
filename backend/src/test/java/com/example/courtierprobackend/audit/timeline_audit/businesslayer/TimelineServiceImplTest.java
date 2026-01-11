package com.example.courtierprobackend.audit.timeline_audit.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.audit.timeline_audit.datamapperlayer.TimelineEntryMapper;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceImplTest {

    @Mock
    private TimelineEntryRepository repository;
    @Mock
    private TimelineEntryMapper mapper;

    private TimelineServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TimelineServiceImpl(repository, mapper);
    }

    @Test
    void addEntry_SimpleOverload_DelegatesToMainMethod() {
        UUID txId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        TimelineEntryType type = TimelineEntryType.CREATED;
        String note = "Test note";
        String docType = "CONTRACT";

        service.addEntry(txId, actorId, type, note, docType);

        ArgumentCaptor<TimelineEntry> captor = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repository).save(captor.capture());
        
        TimelineEntry saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo(txId);
        assertThat(saved.getTransactionInfo()).isNull();
    }

    @Test
    void addEntry_WithTransactionInfo_SavesCorrectly() {
        UUID txId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        TimelineEntryType type = TimelineEntryType.NOTE;
        String note = "Test note";
        String docType = null;
        TransactionInfo txInfo = TransactionInfo.builder()
                .clientName("Client")
                .actorName("Actor")
                .build();

        service.addEntry(txId, actorId, type, note, docType, txInfo);

        ArgumentCaptor<TimelineEntry> captor = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repository).save(captor.capture());

        TimelineEntry saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo(txId);
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getType()).isEqualTo(type);
        assertThat(saved.getNote()).isEqualTo(note);
        assertThat(saved.getDocType()).isNull();
        assertThat(saved.getTransactionInfo()).isEqualTo(txInfo);
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = TimelineEntryType.class, names = {
            "CREATED", "DOCUMENT_REQUESTED", "DOCUMENT_SUBMITTED", 
            "DOCUMENT_APPROVED", "DOCUMENT_NEEDS_REVISION", "STAGE_CHANGE",
            "PROPERTY_ADDED", "PROPERTY_UPDATED", "PROPERTY_REMOVED",
            "OFFER_RECEIVED", "OFFER_UPDATED", "OFFER_REMOVED"
    })
    void addEntry_VisibleTypes_SetsSpecificVisibilityTrue(TimelineEntryType type) {
        service.addEntry(UUID.randomUUID(), UUID.randomUUID(), type, "note", null);

        ArgumentCaptor<TimelineEntry> captor = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isVisibleToClient()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TimelineEntryType.class, names = {
            "NOTE", "TRANSACTION_NOTE", "STATUS_CHANGE"
    })
    void addEntry_HiddenTypes_SetsVisibilityFalse(TimelineEntryType type) {
        service.addEntry(UUID.randomUUID(), UUID.randomUUID(), type, "note", null);

        ArgumentCaptor<TimelineEntry> captor = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isVisibleToClient()).isFalse();
    }

    @Test
    void getTimelineForTransaction_ReturnsMappedDTOs() {
        UUID txId = UUID.randomUUID();
        TimelineEntry entry = new TimelineEntry();
        TimelineEntryDTO dto = new TimelineEntryDTO();

        when(repository.findByTransactionIdOrderByTimestampAsc(txId)).thenReturn(List.of(entry));
        when(mapper.toDTO(entry)).thenReturn(dto);

        List<TimelineEntryDTO> result = service.getTimelineForTransaction(txId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(dto);
        verify(repository).findByTransactionIdOrderByTimestampAsc(txId);
    }

    @Test
    void getTimelineForClient_ReturnsMappedDTOsAndLogs() {
        UUID txId = UUID.randomUUID();
        TimelineEntry entry = new TimelineEntry();
        entry.setId(UUID.randomUUID());
        entry.setType(TimelineEntryType.CREATED);
        entry.setVisibleToClient(true);
        TimelineEntryDTO dto = new TimelineEntryDTO();

        when(repository.findByTransactionIdAndVisibleToClientTrueOrderByTimestampAsc(txId))
                .thenReturn(List.of(entry));
        when(mapper.toDTO(entry)).thenReturn(dto);

        List<TimelineEntryDTO> result = service.getTimelineForClient(txId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(dto);
        verify(repository).findByTransactionIdAndVisibleToClientTrueOrderByTimestampAsc(txId);
    }
    
    @Test
    void getTimelineForClient_WhenEmpty_ReturnsEmptyList() {
        UUID txId = UUID.randomUUID();
        when(repository.findByTransactionIdAndVisibleToClientTrueOrderByTimestampAsc(txId))
                .thenReturn(Collections.emptyList());

        List<TimelineEntryDTO> result = service.getTimelineForClient(txId);

        assertThat(result).isEmpty();
    }
}
