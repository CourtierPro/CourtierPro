package com.example.courtierprobackend.Organization.presentationlayer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettingsResponseModel {

    private UUID id;

    private String defaultLanguage;

    private String inviteSubjectEn;
    private String inviteBodyEn;
    private String inviteSubjectFr;
    private String inviteBodyFr;

    // Document Submitted Template
    private String documentSubmittedSubjectEn;
    private String documentSubmittedBodyEn;
    private String documentSubmittedSubjectFr;
    private String documentSubmittedBodyFr;

    // Document Requested Template
    private String documentRequestedSubjectEn;
    private String documentRequestedBodyEn;
    private String documentRequestedSubjectFr;
    private String documentRequestedBodyFr;

    // Document Signature Requested Template
    private String documentSignatureRequestedSubjectEn;
    private String documentSignatureRequestedBodyEn;
    private String documentSignatureRequestedSubjectFr;
    private String documentSignatureRequestedBodyFr;

    // Document Review Template
    private String documentReviewSubjectEn;
    private String documentReviewBodyEn;
    private String documentReviewSubjectFr;
    private String documentReviewBodyFr;

    // Stage Update Template
    private String stageUpdateSubjectEn;
    private String stageUpdateBodyEn;
    private String stageUpdateSubjectFr;
    private String stageUpdateBodyFr;

    // Property Offer Made Template
    private String propertyOfferMadeSubjectEn;
    private String propertyOfferMadeBodyEn;
    private String propertyOfferMadeSubjectFr;
    private String propertyOfferMadeBodyFr;

    // Property Offer Status Template
    private String propertyOfferStatusSubjectEn;
    private String propertyOfferStatusBodyEn;
    private String propertyOfferStatusSubjectFr;
    private String propertyOfferStatusBodyFr;

    // Offer Received Template
    private String offerReceivedSubjectEn;
    private String offerReceivedBodyEn;
    private String offerReceivedSubjectFr;
    private String offerReceivedBodyFr;

    // Offer Status Template
    private String offerStatusSubjectEn;
    private String offerStatusBodyEn;
    private String offerStatusSubjectFr;
    private String offerStatusBodyFr;

    private Instant updatedAt;
}
