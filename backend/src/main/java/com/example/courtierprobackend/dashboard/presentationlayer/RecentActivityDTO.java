package com.example.courtierprobackend.dashboard.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for recent activity entries in the broker dashboard feed.
 * Includes enriched transaction context for display in the activity timeline.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentActivityDTO {
    private UUID activityId;
    private UUID transactionId;
    private String type;
    private String title;
    private String note;
    private Instant occurredAt;
    private String actorName;
    private String propertyAddress;
    
    // Enriched transaction context fields
    private String clientName;
    private String side;
    private String currentStage;
    
    // Document-specific fields
    private String docType;
    private String status;
    
    // Full transaction info for event-specific details (stage changes, offers, conditions, etc.)
    private TransactionInfo transactionInfo;
    
    // Whether this activity has been marked as seen by the broker
    private boolean seen;
}
