package com.example.courtierprobackend.audit.timeline_audit.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.audit.timeline_audit.datamapperlayer.TimelineEntryMapper;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimelineServiceImpl implements TimelineService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TimelineServiceImpl.class);
    private final TimelineEntryRepository repository;
    private final TimelineEntryMapper timelineEntryMapper;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    @Autowired
    public TimelineServiceImpl(TimelineEntryRepository repository, TimelineEntryMapper timelineEntryMapper, TransactionRepository transactionRepository, Clock clock) {
        this.repository = repository;
        this.timelineEntryMapper = timelineEntryMapper;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    public void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType) {
        addEntry(transactionId, actorId, type, note, docType, null);
    }

    @Override
    @Transactional
    public void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType,
            TransactionInfo transactionInfo) {
        boolean visibleToClient = switch (type) {
            case CREATED, DOCUMENT_REQUESTED, DOCUMENT_SUBMITTED, DOCUMENT_APPROVED, DOCUMENT_NEEDS_REVISION,
                    STAGE_CHANGE, STAGE_ROLLBACK,
                    PROPERTY_ADDED, PROPERTY_UPDATED, PROPERTY_REMOVED,
                    OFFER_RECEIVED, OFFER_UPDATED, OFFER_REMOVED, PROPERTY_OFFER_MADE, PROPERTY_OFFER_UPDATED,
                    OFFER_DOCUMENT_UPLOADED,
                    CONDITION_ADDED, CONDITION_UPDATED, CONDITION_REMOVED, CONDITION_SATISFIED, CONDITION_FAILED,
                    TRANSACTION_TERMINATED,
                    APPOINTMENT_CONFIRMED, APPOINTMENT_CANCELLED, APPOINTMENT_DECLINED, APPOINTMENT_RESCHEDULED,
                    APPOINTMENT_REQUESTED ->
                true;
            default -> false;
        };
        TimelineEntry entry = TimelineEntry.builder()
                .transactionId(transactionId)
                .actorId(actorId)
                .type(type)
                .note(note)
                .docType(docType)
                .timestamp(Instant.now(clock))
                .visibleToClient(visibleToClient)
                .transactionInfo(transactionInfo)
                .build();
        repository.save(entry);

        // Update transaction lastUpdated timestamp
        // We use UTC for conversion, consistent with how timestamps are typically handled
        transactionRepository.findByTransactionId(transactionId).ifPresent(transaction -> {
            transaction.setLastUpdated(LocalDateTime.ofInstant(entry.getTimestamp(), ZoneId.of("UTC")));
            transactionRepository.save(transaction);
        });
    }

    @Override
    public List<TimelineEntryDTO> getTimelineForTransaction(UUID transactionId) {
        List<TimelineEntry> entries = repository.findByTransactionIdOrderByTimestampAsc(transactionId);
        return entries.stream().map(timelineEntryMapper::toDTO).toList();
    }

    @Override
    public List<TimelineEntryDTO> getTimelineForClient(UUID transactionId) {
        log.info("[Timeline] Fetching client-visible timeline for transaction {}", transactionId);
        List<TimelineEntry> entries = repository
                .findByTransactionIdAndVisibleToClientTrueOrderByTimestampAsc(transactionId);
        log.info("[Timeline] Found {} client-visible entries for transaction {}", entries.size(), transactionId);
        for (TimelineEntry entry : entries) {
            log.info("[Timeline] Entry: id={}, type={}, visibleToClient={}",
                    entry.getId(), entry.getType(), entry.isVisibleToClient());
        }
        return entries.stream().map(timelineEntryMapper::toDTO).toList();
    }

    @Override
    public List<TimelineEntryDTO> getRecentEntriesForTransactions(Set<UUID> transactionIds, int limit) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return List.of();
        }

        return transactionIds.stream()
                .flatMap(txId -> repository.findByTransactionIdOrderByTimestampAsc(txId).stream())
                .sorted(Comparator.comparing(TimelineEntry::getTimestamp).reversed())
                .limit(limit)
                .map(timelineEntryMapper::toDTO)
                .toList();
    }

    @Override
    public Page<TimelineEntryDTO> getRecentEntriesForTransactionsPaged(Set<UUID> transactionIds, Pageable pageable) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<TimelineEntry> entriesPage = repository.findByTransactionIdInOrderByTimestampDesc(transactionIds,
                pageable);
        return entriesPage.map(timelineEntryMapper::toDTO);
    }
}
