package com.example.courtierprobackend.transactions.datalayer.enums;

/**
 * Represents the role of a participant in a transaction. Includes BUYER and SELLER
 * roles even though the primary buyer and seller are modeled on the Transaction
 * entity, to support additional participant-role assignments.
 */
public enum ParticipantRole {
    BROKER,
    CO_BROKER,
    NOTARY,
    LAWYER,
    BUYER,
    SELLER,
    OTHER
}
