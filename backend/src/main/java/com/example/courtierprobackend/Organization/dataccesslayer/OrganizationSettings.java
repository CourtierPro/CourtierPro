package com.example.courtierprobackend.Organization.dataccesslayer;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettings {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "default_language", nullable = false, length = 2)
    private String defaultLanguage;

    @Column(name = "invite_subject_en", nullable = false, length = 255)
    private String inviteSubjectEn;

    @Column(name = "invite_body_en", nullable = false, columnDefinition = "TEXT")
    private String inviteBodyEn;

    @Column(name = "invite_subject_fr", nullable = false, length = 255)
    private String inviteSubjectFr;

    @Column(name = "invite_body_fr", nullable = false, columnDefinition = "TEXT")
    private String inviteBodyFr;

    // Document Submitted Template
    @Column(name = "document_submitted_subject_en", length = 255)
    private String documentSubmittedSubjectEn;

    @Column(name = "document_submitted_body_en", columnDefinition = "TEXT")
    private String documentSubmittedBodyEn;

    @Column(name = "document_submitted_subject_fr", length = 255)
    private String documentSubmittedSubjectFr;

    @Column(name = "document_submitted_body_fr", columnDefinition = "TEXT")
    private String documentSubmittedBodyFr;

    // Document Requested Template
    @Column(name = "document_requested_subject_en", length = 255)
    private String documentRequestedSubjectEn;

    @Column(name = "document_requested_body_en", columnDefinition = "TEXT")
    private String documentRequestedBodyEn;

    @Column(name = "document_requested_subject_fr", length = 255)
    private String documentRequestedSubjectFr;

    @Column(name = "document_requested_body_fr", columnDefinition = "TEXT")
    private String documentRequestedBodyFr;

    // Document Review Template
    @Column(name = "document_review_subject_en", length = 255)
    private String documentReviewSubjectEn;

    @Column(name = "document_review_body_en", columnDefinition = "TEXT")
    private String documentReviewBodyEn;

    @Column(name = "document_review_subject_fr", length = 255)
    private String documentReviewSubjectFr;

    @Column(name = "document_review_body_fr", columnDefinition = "TEXT")
    private String documentReviewBodyFr;

    // Stage Update Template
    @Column(name = "stage_update_subject_en", length = 255)
    private String stageUpdateSubjectEn;

    @Column(name = "stage_update_body_en", columnDefinition = "TEXT")
    private String stageUpdateBodyEn;

    @Column(name = "stage_update_subject_fr", length = 255)
    private String stageUpdateSubjectFr;

    @Column(name = "stage_update_body_fr", columnDefinition = "TEXT")
    private String stageUpdateBodyFr;

    // Property Offer Made Template
    @Column(name = "property_offer_made_subject_en", length = 255)
    private String propertyOfferMadeSubjectEn;

    @Column(name = "property_offer_made_body_en", columnDefinition = "TEXT")
    private String propertyOfferMadeBodyEn;

    @Column(name = "property_offer_made_subject_fr", length = 255)
    private String propertyOfferMadeSubjectFr;

    @Column(name = "property_offer_made_body_fr", columnDefinition = "TEXT")
    private String propertyOfferMadeBodyFr;

    // Property Offer Status Template
    @Column(name = "property_offer_status_subject_en", length = 255)
    private String propertyOfferStatusSubjectEn;

    @Column(name = "property_offer_status_body_en", columnDefinition = "TEXT")
    private String propertyOfferStatusBodyEn;

    @Column(name = "property_offer_status_subject_fr", length = 255)
    private String propertyOfferStatusSubjectFr;

    @Column(name = "property_offer_status_body_fr", columnDefinition = "TEXT")
    private String propertyOfferStatusBodyFr;

    // Offer Received Template
    @Column(name = "offer_received_subject_en", length = 255)
    private String offerReceivedSubjectEn;

    @Column(name = "offer_received_body_en", columnDefinition = "TEXT")
    private String offerReceivedBodyEn;

    @Column(name = "offer_received_subject_fr", length = 255)
    private String offerReceivedSubjectFr;

    @Column(name = "offer_received_body_fr", columnDefinition = "TEXT")
    private String offerReceivedBodyFr;

    // Offer Status Template
    @Column(name = "offer_status_subject_en", length = 255)
    private String offerStatusSubjectEn;

    @Column(name = "offer_status_body_en", columnDefinition = "TEXT")
    private String offerStatusBodyEn;

    @Column(name = "offer_status_subject_fr", length = 255)
    private String offerStatusSubjectFr;

    @Column(name = "offer_status_body_fr", columnDefinition = "TEXT")
    private String offerStatusBodyFr;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
