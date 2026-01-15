package com.example.courtierprobackend.Organization.presentationlayer.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationSettingsRequestModel {

    @NotBlank
    @Pattern(regexp = "en|fr", message = "defaultLanguage must be 'en' or 'fr'")
    private String defaultLanguage;

    @NotBlank
    @Size(max = 255)
    private String inviteSubjectEn;

    @NotBlank
    private String inviteBodyEn;

    @NotBlank
    @Size(max = 255)
    private String inviteSubjectFr;

    @NotBlank
    private String inviteBodyFr;

    // Document Submitted Template
    @Size(max = 255)
    private String documentSubmittedSubjectEn;

    private String documentSubmittedBodyEn;

    @Size(max = 255)
    private String documentSubmittedSubjectFr;

    private String documentSubmittedBodyFr;

    // Document Requested Template
    @Size(max = 255)
    private String documentRequestedSubjectEn;

    private String documentRequestedBodyEn;

    @Size(max = 255)
    private String documentRequestedSubjectFr;

    private String documentRequestedBodyFr;

    // Document Review Template
    @Size(max = 255)
    private String documentReviewSubjectEn;

    private String documentReviewBodyEn;

    @Size(max = 255)
    private String documentReviewSubjectFr;

    private String documentReviewBodyFr;

    // Stage Update Template
    @Size(max = 255)
    private String stageUpdateSubjectEn;

    private String stageUpdateBodyEn;

    @Size(max = 255)
    private String stageUpdateSubjectFr;

    private String stageUpdateBodyFr;
}
