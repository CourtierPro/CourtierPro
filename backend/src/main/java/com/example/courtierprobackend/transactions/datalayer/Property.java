package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a property that a buyer is interested in (for buy-side transactions).
 */
@Entity
@Table(name = "properties")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false, unique = true)
    private UUID propertyId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Embedded
    private PropertyAddress address;

    @Column(name = "asking_price", precision = 15, scale = 2)
    private BigDecimal askingPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status", nullable = false)
    private PropertyOfferStatus offerStatus;

    @Column(name = "offer_amount", precision = 15, scale = 2)
    private BigDecimal offerAmount;

    @Column(name = "centris_number")
    private String centrisNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.example.courtierprobackend.transactions.datalayer.enums.PropertyStatus status;

    @PrePersist
    protected void onCreate() {
        if (propertyId == null) {
            propertyId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (offerStatus == null) {
            offerStatus = PropertyOfferStatus.OFFER_TO_BE_MADE;
        }
        if (status == null) {
            status = com.example.courtierprobackend.transactions.datalayer.enums.PropertyStatus.SUGGESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
