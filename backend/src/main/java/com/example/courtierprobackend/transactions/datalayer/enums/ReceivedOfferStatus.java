package com.example.courtierprobackend.transactions.datalayer.enums;

/**
 * Status values for offers received on sell-side transactions.
 * These represent the seller's perspective on incoming offers from buyers.
 */
public enum ReceivedOfferStatus {
    PENDING,        // Offer received, awaiting review
    UNDER_REVIEW,   // Actively being considered
    COUNTERED,      // Counter-offer sent to buyer
    ACCEPTED,       // Offer accepted
    DECLINED        // Offer rejected
}
