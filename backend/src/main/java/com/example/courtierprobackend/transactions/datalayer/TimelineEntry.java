package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.TimelineEntryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timeline_entries")
@Where(clause = "deleted_at IS NULL")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TimelineEntryType type;

    private String note;
    private String title;

    private Boolean visibleToClient;

    private LocalDateTime occurredAt;

    private UUID addedByBrokerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    // Soft delete fields
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
