package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "street")),
        @AttributeOverride(name = "city", column = @Column(name = "city")),
        @AttributeOverride(name = "province", column = @Column(name = "province")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "postal_code"))
    })
    private PropertyAddress address;

    @Column(name = "asking_price", precision = 15, scale = 2)
    private BigDecimal askingPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status", nullable = false)
    private OfferStatus offerStatus;

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
            offerStatus = OfferStatus.OFFER_TO_BE_MADE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
