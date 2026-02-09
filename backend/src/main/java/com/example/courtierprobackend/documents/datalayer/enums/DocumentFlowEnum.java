package com.example.courtierprobackend.documents.datalayer.enums;

/**
 * Distinguishes the document flow type.
 * REQUEST: Broker requests a document that the client must upload.
 * UPLOAD: Broker uploads a document directly for the client to access.
 */
public enum DocumentFlowEnum {
    REQUEST,
    UPLOAD
}
