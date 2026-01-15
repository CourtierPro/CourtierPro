package com.example.courtierprobackend.transactions.datalayer.enums;

/**
 * Represents the client's decision on a received offer (sell-side).
 * The client reviews offers and indicates their preference,
 * which the broker then finalizes.
 */
public enum ClientOfferDecision {
    ACCEPT,     // Client wants to accept the offer
    DECLINE,    // Client wants to decline the offer
    COUNTER     // Client wants to make a counter-offer
}
