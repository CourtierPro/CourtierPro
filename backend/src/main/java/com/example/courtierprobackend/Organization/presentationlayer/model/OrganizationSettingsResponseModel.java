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

    private Instant updatedAt;
}
