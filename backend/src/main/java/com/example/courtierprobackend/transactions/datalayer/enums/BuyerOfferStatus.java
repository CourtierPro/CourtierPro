package com.example.courtierprobackend.transactions.datalayer.enums;

/**
 * Status values for offers made on buy-side properties.
 * Tracks the lifecycle of an offer from the buyer's perspective.
 */
public enum BuyerOfferStatus {
    OFFER_MADE,     // Offer submitted to seller
    COUNTERED,      // Seller has countered
    ACCEPTED,       // Offer accepted by seller
    DECLINED,       // Offer rejected by seller
    WITHDRAWN,      // Buyer withdrew the offer
    EXPIRED         // Offer expired before response
}
