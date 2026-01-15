package com.example.courtierprobackend.transactions.datalayer.enums;

/**
 * Response status from the counterparty (seller) for a buy-side offer.
 */
public enum CounterpartyResponse {
    PENDING,    // Awaiting response
    ACCEPTED,   // Seller accepted
    COUNTERED,  // Seller countered
    DECLINED    // Seller declined
}
