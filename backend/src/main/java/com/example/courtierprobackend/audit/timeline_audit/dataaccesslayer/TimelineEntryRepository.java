package com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;

@Repository
public interface TimelineEntryRepository extends JpaRepository<TimelineEntry, UUID> {
    List<TimelineEntry> findByTransactionIdOrderByTimestampAsc(UUID transactionId);

    List<TimelineEntry> findByTransactionIdAndVisibleToClientTrueOrderByTimestampAsc(UUID transactionId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM timeline_entries WHERE transaction_id = :transactionId", nativeQuery = true)
    List<TimelineEntry> findByTransactionIdIncludingDeleted(UUID transactionId);

    /**
     * Paginated query to fetch timeline entries for multiple transactions,
     * ordered by timestamp descending (most recent first).
     */
    Page<TimelineEntry> findByTransactionIdInOrderByTimestampDesc(Set<UUID> transactionIds, Pageable pageable);

    /**
     * Batch fetch specific event types for a set of transactions, ordered by time.
     * Used for analytics pipeline calculations.
     */
    List<TimelineEntry> findByTransactionIdInAndTypeInOrderByTimestampAsc(Collection<UUID> transactionIds,
            Collection<TimelineEntryType> types);
}
