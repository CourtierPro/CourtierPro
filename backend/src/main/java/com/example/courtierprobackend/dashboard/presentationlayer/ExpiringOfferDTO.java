package com.example.courtierprobackend.dashboard.presentationlayer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing an offer that is expiring soon.
 * Used in the broker dashboard to highlight urgent items.
 */
@Data
@Builder
public class ExpiringOfferDTO {
    
    private UUID offerId;
    private UUID transactionId;
    private String propertyAddress;
    private String clientName;
    private BigDecimal offerAmount;
    private LocalDate expiryDate;
    private int daysUntilExpiry;
    private String offerType; // "BUY_SIDE" (PropertyOffer) or "SELL_SIDE" (Offer)
    private String status;
}
