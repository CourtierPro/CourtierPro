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
}
