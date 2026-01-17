package com.example.courtierprobackend.audit.timeline_audit.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for TimelineEntry.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimelineEntryDTO {
    private UUID id;
    private UUID transactionId;
    private TimelineEntryType type;
    private String note;
    private String title;
    private String docType;
    private Boolean visibleToClient;
    private Instant occurredAt;
    private UUID addedByBrokerId;
    private String actorName; //: display name of the actor (broker/client)
    private TransactionInfo transactionInfo;
}
