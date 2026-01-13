package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.BuyerOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.CounterpartyResponse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an offer made on a buy-side property.
 * Tracks each offer round during negotiations for a specific property.
 */
@Entity
@Table(name = "property_offers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PropertyOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_offer_id", nullable = false, unique = true)
    private UUID propertyOfferId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "offer_round", nullable = false)
    private Integer offerRound;

    @Column(name = "offer_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal offerAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BuyerOfferStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "counterparty_response")
    private CounterpartyResponse counterpartyResponse;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (propertyOfferId == null) {
            propertyOfferId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BuyerOfferStatus.OFFER_MADE;
        }
        if (offerRound == null) {
            offerRound = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
