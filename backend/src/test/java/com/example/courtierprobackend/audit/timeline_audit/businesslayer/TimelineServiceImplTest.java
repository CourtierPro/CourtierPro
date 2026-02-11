package com.example.courtierprobackend.audit.timeline_audit.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.audit.timeline_audit.datamapperlayer.TimelineEntryMapper;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceImplTest {

    @Mock
    private TimelineEntryRepository repository;
    @Mock
    private TimelineEntryMapper mapper;
    @Mock
    private TransactionRepository transactionRepository;

    private TimelineServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TimelineServiceImpl(repository, mapper, transactionRepository);
    }

    @Test
    void addEntry_SimpleOverload_DelegatesToMainMethod() {
        UUID txId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        TimelineEntryType type = TimelineEntryType.CREATED;
        String note = "Test note";
        String docType = "CONTRACT";

        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        service.addEntry(txId, actorId, type, note, docType);

        ArgumentCaptor<TimelineEntry> captor = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repository).save(captor.capture());
        
        TimelineEntry saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo(txId);
        assertThat(saved.getTransactionInfo()).isNull();
    }

    @Test
    void addEntry_UpdatesTransactionLastUpdatedTimestamp() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        TimelineEntryType type = TimelineEntryType.NOTE;
        String note = "Update timestamp";
        String docType = null;
        
        Transaction mockTx = new Transaction();
        mockTx.setTransactionId(txId);
        mockTx.setLastUpdated(LocalDateTime.now().minusDays(1)); // Old timestamp
        
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.of(mockTx));

        // Act
        service.addEntry(txId, actorId, type, note, docType);

        // Assert
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).findByTransactionId(txId);
        verify(transactionRepository).save(txCaptor.capture());
        
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getTransactionId()).isEqualTo(txId);
        // Verify lastUpdated was updated to recent time (e.g. within last second)
        assertThat(savedTx.getLastUpdated()).isAfter(LocalDateTime.now().minusMinutes(1));
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

        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

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
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        service.addEntry(txId, UUID.randomUUID(), type, "note", null);

        ArgumentCaptor<TimelineEntry> captor = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isVisibleToClient()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TimelineEntryType.class, names = {
            "NOTE", "TRANSACTION_NOTE", "STATUS_CHANGE"
    })
    void addEntry_HiddenTypes_SetsVisibilityFalse(TimelineEntryType type) {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        service.addEntry(txId, UUID.randomUUID(), type, "note", null);

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

    // ========== getRecentEntriesForTransactions Tests ==========

    @Test
    void getRecentEntriesForTransactions_WithNullSet_ReturnsEmptyList() {
        List<TimelineEntryDTO> result = service.getRecentEntriesForTransactions(null, 10);
        assertThat(result).isEmpty();
    }

    @Test
    void getRecentEntriesForTransactions_WithEmptySet_ReturnsEmptyList() {
        List<TimelineEntryDTO> result = service.getRecentEntriesForTransactions(java.util.Set.of(), 10);
        assertThat(result).isEmpty();
    }

    @Test
    void getRecentEntriesForTransactions_WithSingleTransaction_ReturnsMappedEntries() {
        UUID txId = UUID.randomUUID();
        TimelineEntry entry = TimelineEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .timestamp(java.time.Instant.now())
                .build();
        TimelineEntryDTO dto = TimelineEntryDTO.builder().id(entry.getId()).build();

        when(repository.findByTransactionIdOrderByTimestampAsc(txId)).thenReturn(List.of(entry));
        when(mapper.toDTO(entry)).thenReturn(dto);

        List<TimelineEntryDTO> result = service.getRecentEntriesForTransactions(java.util.Set.of(txId), 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(dto);
    }

    @Test
    void getRecentEntriesForTransactions_WithMultipleTransactions_ReturnsSortedByTimestampDesc() {
        UUID txId1 = UUID.randomUUID();
        UUID txId2 = UUID.randomUUID();
        java.time.Instant olderTime = java.time.Instant.now().minusSeconds(3600);
        java.time.Instant newerTime = java.time.Instant.now();

        TimelineEntry olderEntry = TimelineEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(txId1)
                .timestamp(olderTime)
                .build();
        TimelineEntry newerEntry = TimelineEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(txId2)
                .timestamp(newerTime)
                .build();

        TimelineEntryDTO olderDto = TimelineEntryDTO.builder().id(olderEntry.getId()).occurredAt(olderTime).build();
        TimelineEntryDTO newerDto = TimelineEntryDTO.builder().id(newerEntry.getId()).occurredAt(newerTime).build();

        when(repository.findByTransactionIdOrderByTimestampAsc(txId1)).thenReturn(List.of(olderEntry));
        when(repository.findByTransactionIdOrderByTimestampAsc(txId2)).thenReturn(List.of(newerEntry));
        when(mapper.toDTO(olderEntry)).thenReturn(olderDto);
        when(mapper.toDTO(newerEntry)).thenReturn(newerDto);

        List<TimelineEntryDTO> result = service.getRecentEntriesForTransactions(java.util.Set.of(txId1, txId2), 10);

        assertThat(result).hasSize(2);
        // Should be sorted by timestamp descending (newest first)
        assertThat(result.get(0).getOccurredAt()).isEqualTo(newerTime);
        assertThat(result.get(1).getOccurredAt()).isEqualTo(olderTime);
    }

    @Test
    void getRecentEntriesForTransactions_WithLimitLowerThanEntries_ReturnsLimitedEntries() {
        UUID txId = UUID.randomUUID();
        java.util.List<TimelineEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TimelineEntry entry = TimelineEntry.builder()
                    .id(UUID.randomUUID())
                    .transactionId(txId)
                    .timestamp(java.time.Instant.now().minusSeconds(i * 100))
                    .build();
            entries.add(entry);
        }

        when(repository.findByTransactionIdOrderByTimestampAsc(txId)).thenReturn(entries);
        // Use lenient stubbing since only 3 of 5 will be used due to limit
        for (TimelineEntry entry : entries) {
            org.mockito.Mockito.lenient().when(mapper.toDTO(entry)).thenReturn(TimelineEntryDTO.builder().id(entry.getId()).build());
        }

        List<TimelineEntryDTO> result = service.getRecentEntriesForTransactions(java.util.Set.of(txId), 3);

        assertThat(result).hasSize(3);
    }

    // ========== Condition-Related Visibility Tests ==========

    @ParameterizedTest
    @EnumSource(value = TimelineEntryType.class, names = {
            "PROPERTY_OFFER_MADE", "PROPERTY_OFFER_UPDATED", "OFFER_DOCUMENT_UPLOADED",
            "CONDITION_ADDED", "CONDITION_UPDATED", "CONDITION_REMOVED", 
            "CONDITION_SATISFIED", "CONDITION_FAILED"
    })
    void addEntry_ConditionAndOfferTypes_SetsVisibilityTrue(TimelineEntryType type) {
        service.addEntry(UUID.randomUUID(), UUID.randomUUID(), type, "note", null);

        ArgumentCaptor<TimelineEntry> captor = ArgumentCaptor.forClass(TimelineEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isVisibleToClient()).isTrue();
    }

    // ========== getRecentEntriesForTransactionsPaged Tests ==========

    @Test
    void getRecentEntriesForTransactionsPaged_WithNullSet_ReturnsEmptyPage() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<TimelineEntryDTO> result = service.getRecentEntriesForTransactionsPaged(null, pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getRecentEntriesForTransactionsPaged_WithEmptySet_ReturnsEmptyPage() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<TimelineEntryDTO> result = service.getRecentEntriesForTransactionsPaged(java.util.Set.of(), pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getRecentEntriesForTransactionsPaged_WithValidSet_ReturnsPagedResults() {
        UUID txId = UUID.randomUUID();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        
        TimelineEntry entry = TimelineEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .timestamp(java.time.Instant.now())
                .build();
        TimelineEntryDTO dto = TimelineEntryDTO.builder().id(entry.getId()).build();
        
        org.springframework.data.domain.Page<TimelineEntry> entryPage = new org.springframework.data.domain.PageImpl<>(
                List.of(entry), pageable, 1);
        
        when(repository.findByTransactionIdInOrderByTimestampDesc(java.util.Set.of(txId), pageable))
                .thenReturn(entryPage);
        when(mapper.toDTO(entry)).thenReturn(dto);
        
        org.springframework.data.domain.Page<TimelineEntryDTO> result = 
                service.getRecentEntriesForTransactionsPaged(java.util.Set.of(txId), pageable);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(dto);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}

