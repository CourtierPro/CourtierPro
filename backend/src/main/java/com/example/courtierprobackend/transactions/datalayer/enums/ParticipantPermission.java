package com.example.courtierprobackend.transactions.datalayer.enums;

public enum ParticipantPermission {
    // Document Management
    VIEW_DOCUMENTS,
    EDIT_DOCUMENTS,

    // Property Management (Buyer side)
    VIEW_PROPERTIES,
    EDIT_PROPERTIES,

    // Stage Management
    VIEW_STAGE,
    EDIT_STAGE,

    // Offer Management (Seller side) & Property Offers (Buyer side)
    VIEW_OFFERS,
    EDIT_OFFERS,

    // Conditions Management
    VIEW_CONDITIONS,
    EDIT_CONDITIONS,

    // Notes Management
    VIEW_NOTES,
    EDIT_NOTES
}
