package com.example.courtierprobackend.dashboard.presentationlayer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a document awaiting broker review.
 * Used in the broker dashboard to highlight action items.
 */
@Data
@Builder
public class PendingDocumentDTO {
    
    private UUID requestId;
    private UUID transactionId;
    private String clientName;
    private String documentType;
    private String customTitle;
    private LocalDateTime submittedAt;
    private String propertyAddress;
}
