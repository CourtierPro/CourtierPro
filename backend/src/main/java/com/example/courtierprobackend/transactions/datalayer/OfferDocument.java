package com.example.courtierprobackend.transactions.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a document attached to an offer (either sell-side or buy-side).
 * Documents can be Promise to Purchase, counter-offer letters, etc.
 */
@Entity
@Table(name = "offer_documents")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "property_offer_id")
    private UUID propertyOfferId;

    @Column(name = "s3_key", nullable = false, length = 1000)
    private String s3Key;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (documentId == null) {
            documentId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
