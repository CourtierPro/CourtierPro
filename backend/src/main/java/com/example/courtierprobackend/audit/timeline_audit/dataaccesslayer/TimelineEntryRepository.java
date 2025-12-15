package com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineEntryRepository extends JpaRepository<TimelineEntry, UUID> {
    List<TimelineEntry> findByTransactionIdOrderByTimestampAsc(UUID transactionId);

    List<TimelineEntry> findByTransactionIdAndVisibleToClientTrueOrderByTimestampAsc(UUID transactionId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM timeline_entries WHERE transaction_id = :transactionId", nativeQuery = true)
    List<TimelineEntry> findByTransactionIdIncludingDeleted(UUID transactionId);
}
