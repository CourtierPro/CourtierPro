package com.example.courtierprobackend.transactions.datalayer.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified document DTO that aggregates documents from all sources:
 * - CLIENT_UPLOAD: Documents submitted by clients (from DocumentRequest/SubmittedDocument)
 * - OFFER_ATTACHMENT: Documents attached to sell-side offers
 * - PROPERTY_OFFER_ATTACHMENT: Documents attached to buy-side property offers
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnifiedDocumentDTO {

    private UUID documentId;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private LocalDateTime uploadedAt;
    
    /**
     * Source of the document:
     * - CLIENT_UPLOAD
     * - OFFER_ATTACHMENT
     * - PROPERTY_OFFER_ATTACHMENT
     */
    private String source;
    
    /**
     * ID of the source entity (requestId, offerId, or propertyOfferId)
     */
    private UUID sourceId;
    
    /**
     * Human-readable name for the source:
     * - For CLIENT_UPLOAD: document type (e.g., "Pre-approval Letter")
     * - For OFFER_ATTACHMENT: buyer name
     * - For PROPERTY_OFFER_ATTACHMENT: property address
     */
    private String sourceName;
    
    /**
     * Status (only applicable for CLIENT_UPLOAD documents)
     */
    private String status;
}
