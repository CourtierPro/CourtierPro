package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an offer received on a sell-side transaction.
 * Sellers can receive multiple offers from potential buyers.
 */
@Entity
@Table(name = "offers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false, unique = true)
    private UUID offerId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(name = "offer_amount", precision = 15, scale = 2)
    private BigDecimal offerAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReceivedOfferStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (offerId == null) {
            offerId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReceivedOfferStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

