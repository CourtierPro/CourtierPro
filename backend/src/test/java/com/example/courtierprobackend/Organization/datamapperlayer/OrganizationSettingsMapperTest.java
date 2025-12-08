package com.example.courtierprobackend.Organization.datamapperlayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for OrganizationSettingsMapper.
 * Tests simple mapping logic without Spring context.
 */
class OrganizationSettingsMapperTest {

    private OrganizationSettingsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrganizationSettingsMapper();
    }

    @Test
    void toResponseModel_WithValidEntity_MapsCorrectly() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant updatedAt = Instant.now();

        OrganizationSettings entity = OrganizationSettings.builder()
                .id(id)
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome to CourtierPro")
                .inviteBodyEn("Hi {{name}}, your account has been created.")
                .inviteSubjectFr("Bienvenue sur CourtierPro")
                .inviteBodyFr("Bonjour {{name}}, votre compte a été créé.")
                .updatedAt(updatedAt)
                .build();

        // Act
        OrganizationSettingsResponseModel result = mapper.toResponseModel(entity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getDefaultLanguage()).isEqualTo("fr");
        assertThat(result.getInviteSubjectEn()).isEqualTo("Welcome to CourtierPro");
        assertThat(result.getInviteBodyEn()).isEqualTo("Hi {{name}}, your account has been created.");
        assertThat(result.getInviteSubjectFr()).isEqualTo("Bienvenue sur CourtierPro");
        assertThat(result.getInviteBodyFr()).isEqualTo("Bonjour {{name}}, votre compte a été créé.");
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponseModel_WithNullEntity_ReturnsNull() {
        // Act
        OrganizationSettingsResponseModel result = mapper.toResponseModel(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void toResponseModel_WithAllFields_PreservesAllData() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2025-12-07T10:15:30Z");

        OrganizationSettings entity = OrganizationSettings.builder()
                .id(id)
                .defaultLanguage("en")
                .inviteSubjectEn("Test Subject EN")
                .inviteBodyEn("Test Body EN with {{name}} placeholder")
                .inviteSubjectFr("Test Subject FR")
                .inviteBodyFr("Test Body FR with {{name}} placeholder")
                .updatedAt(updatedAt)
                .build();

        // Act
        OrganizationSettingsResponseModel result = mapper.toResponseModel(entity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getDefaultLanguage()).isEqualTo("en");
        assertThat(result.getInviteSubjectEn()).isEqualTo("Test Subject EN");
        assertThat(result.getInviteBodyEn()).contains("{{name}}");
        assertThat(result.getInviteSubjectFr()).isEqualTo("Test Subject FR");
        assertThat(result.getInviteBodyFr()).contains("{{name}}");
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
