package com.example.courtierprobackend.transactions.datalayer.enums;

public enum ParticipantRole {
    BROKER,
    CO_BROKER,
    NOTARY,
    LAWYER,
    BUYER, // Added as per requirements/plan, though primary buyer/seller are on
           // Transaction
    SELLER,
    OTHER
}
