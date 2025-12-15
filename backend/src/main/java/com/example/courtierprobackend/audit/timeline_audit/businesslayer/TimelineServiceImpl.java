package com.example.courtierprobackend.audit.timeline_audit.businesslayer;


import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.audit.timeline_audit.datamapperlayer.TimelineEntryMapper;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TimelineServiceImpl implements TimelineService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TimelineServiceImpl.class);
    private final TimelineEntryRepository repository;
    private final TimelineEntryMapper timelineEntryMapper;

    @Autowired
    public TimelineServiceImpl(TimelineEntryRepository repository, TimelineEntryMapper timelineEntryMapper) {
        this.repository = repository;
        this.timelineEntryMapper = timelineEntryMapper;
    }

    @Override
    public void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType) {
        addEntry(transactionId, actorId, type, note, docType, null);
    }

    @Override
    public void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType, TransactionInfo transactionInfo) {
        boolean visibleToClient = switch (type) {
            case CREATED, DOCUMENT_REQUESTED, DOCUMENT_SUBMITTED, DOCUMENT_APPROVED, DOCUMENT_NEEDS_REVISION, STAGE_CHANGE -> true;
            default -> false;
        };
        TimelineEntry entry = TimelineEntry.builder()
            .transactionId(transactionId)
            .actorId(actorId)
            .type(type)
            .note(note)
            .docType(docType)
            .timestamp(Instant.now())
            .visibleToClient(visibleToClient)
            .transactionInfo(transactionInfo)
            .build();
        repository.save(entry);
    }

    @Override
    public List<TimelineEntryDTO> getTimelineForTransaction(UUID transactionId) {
        List<TimelineEntry> entries = repository.findByTransactionIdOrderByTimestampAsc(transactionId);
        return entries.stream().map(timelineEntryMapper::toDTO).toList();
    }

    @Override
    public List<TimelineEntryDTO> getTimelineForClient(UUID transactionId) {
        log.info("[Timeline] Fetching client-visible timeline for transaction {}", transactionId);
        List<TimelineEntry> entries = repository.findByTransactionIdAndVisibleToClientTrueOrderByTimestampAsc(transactionId);
        log.info("[Timeline] Found {} client-visible entries for transaction {}", entries.size(), transactionId);
        for (TimelineEntry entry : entries) {
            log.info("[Timeline] Entry: id={}, type={}, docType={}, visibleToClient={}, timestamp={}",
                entry.getId(), entry.getType(), entry.getDocType(), entry.isVisibleToClient(), entry.getTimestamp());
        }
        return entries.stream().map(timelineEntryMapper::toDTO).toList();
    }
}
