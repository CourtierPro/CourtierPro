package com.example.courtierprobackend.transactions.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks revisions/changes made to a sell-side offer.
 * Enables viewing the complete negotiation history of an offer.
 */
@Entity
@Table(name = "offer_revisions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revision_id", nullable = false, unique = true)
    private UUID revisionId;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Column(name = "previous_amount", precision = 15, scale = 2)
    private BigDecimal previousAmount;

    @Column(name = "new_amount", precision = 15, scale = 2)
    private BigDecimal newAmount;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "new_status")
    private String newStatus;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (revisionId == null) {
            revisionId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
