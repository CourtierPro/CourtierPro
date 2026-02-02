package com.example.courtierprobackend.transactions.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Join entity linking conditions to offers, property offers, or documents.
 * Exactly one of offerId, propertyOfferId, or documentId must be set.
 */
@Entity
@Table(name = "document_conditions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentConditionLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condition_id", nullable = false)
    private UUID conditionId;

    @Column(name = "offer_id")
    private UUID offerId;

    @Column(name = "property_offer_id")
    private UUID propertyOfferId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
