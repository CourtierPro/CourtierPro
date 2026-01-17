package com.example.courtierprobackend.dashboard.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track which timeline entries a broker has marked as seen.
 * Each broker maintains their own independent seen state for all events.
 */
@Entity
@Table(name = "timeline_entries_seen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEntrySeen {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID brokerId;

    @Column(nullable = false)
    private UUID timelineEntryId;

    @Column(nullable = false)
    private Instant seenAt;
}
