package com.example.courtierprobackend.audit.timeline_audit.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TimelineService {
    void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType);

    // Nouvelle méthode pour supporter TransactionInfo
    void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType, TransactionInfo transactionInfo);


    List<TimelineEntryDTO> getTimelineForTransaction(UUID transactionId);


    List<TimelineEntryDTO> getTimelineForClient(UUID transactionId);
    
    /**
     * Get recent timeline entries across multiple transactions.
     * Returns entries sorted by occurredAt descending, limited to the specified count.
     */
    List<TimelineEntryDTO> getRecentEntriesForTransactions(Set<UUID> transactionIds, int limit);
    
    /**
     * Get recent timeline entries across multiple transactions with pagination.
     * Returns entries sorted by occurredAt descending.
     */
    Page<TimelineEntryDTO> getRecentEntriesForTransactionsPaged(Set<UUID> transactionIds, Pageable pageable);
}

