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

    private Instant updatedAt;
}
