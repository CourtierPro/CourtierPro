package com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timeline_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEntry {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimelineEntryType type;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 100)
    private String docType; 

    @Column(name = "visible_to_client", nullable = false)
    @Builder.Default
    private boolean visibleToClient = false;

    @Embedded
    private TransactionInfo transactionInfo;

}

