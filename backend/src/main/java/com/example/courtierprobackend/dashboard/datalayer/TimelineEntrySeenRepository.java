package com.example.courtierprobackend.dashboard.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for TimelineEntrySeen entity.
 * Provides methods to check and manage broker's seen status on timeline entries.
 */
@Repository
public interface TimelineEntrySeenRepository extends JpaRepository<TimelineEntrySeen, UUID> {

    /**
     * Finds all seen records for a broker and a set of timeline entry IDs.
     * Used to efficiently check seen status for multiple entries at once.
     */
    List<TimelineEntrySeen> findByBrokerIdAndTimelineEntryIdIn(UUID brokerId, Set<UUID> timelineEntryIds);

    /**
     * Checks if a specific timeline entry has been seen by a broker.
     */
    boolean existsByBrokerIdAndTimelineEntryId(UUID brokerId, UUID timelineEntryId);

    /**
     * Deletes all seen records for a specific broker.
     * Could be used for cleanup or testing.
     */
    void deleteByBrokerId(UUID brokerId);
}
