package com.example.courtierprobackend.Organization.datamapperlayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
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
    @Test
    void updateEntityFromRequest_WithValidRequest_UpdatesAllFields() {
        // Arrange
        OrganizationSettings settings = OrganizationSettings.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("Old Subject EN")
                .inviteBodyEn("Old Body EN")
                .inviteSubjectFr("Old Subject FR")
                .inviteBodyFr("Old Body FR")
                .documentSubmittedSubjectEn("Old DocSubjEn")
                .documentSubmittedBodyEn("Old DocBodyEn")
                .documentSubmittedSubjectFr("Old DocSubjFr")
                .documentSubmittedBodyFr("Old DocBodyFr")
                .documentRequestedSubjectEn("Old DocReqSubjEn")
                .documentRequestedBodyEn("Old DocReqBodyEn")
                .documentRequestedSubjectFr("Old DocReqSubjFr")
                .documentRequestedBodyFr("Old DocReqBodyFr")
                .documentReviewSubjectEn("Old DocRevSubjEn")
                .documentReviewBodyEn("Old DocRevBodyEn")
                .documentReviewSubjectFr("Old DocRevSubjFr")
                .documentReviewBodyFr("Old DocRevBodyFr")
                .stageUpdateSubjectEn("Old StageSubjEn")
                .stageUpdateBodyEn("Old StageBodyEn")
                .stageUpdateSubjectFr("Old StageSubjFr")
                .stageUpdateBodyFr("Old StageBodyFr")
                .propertyOfferMadeSubjectEn("Old OfferMadeSubjEn")
                .propertyOfferMadeBodyEn("Old OfferMadeBodyEn")
                .propertyOfferMadeSubjectFr("Old OfferMadeSubjFr")
                .propertyOfferMadeBodyFr("Old OfferMadeBodyFr")
                .propertyOfferStatusSubjectEn("Old OfferStatusSubjEn")
                .propertyOfferStatusBodyEn("Old OfferStatusBodyEn")
                .propertyOfferStatusSubjectFr("Old OfferStatusSubjFr")
                .propertyOfferStatusBodyFr("Old OfferStatusBodyFr")
                .offerReceivedSubjectEn("Old OfferReceivedSubjEn")
                .offerReceivedBodyEn("Old OfferReceivedBodyEn")
                .offerReceivedSubjectFr("Old OfferReceivedSubjFr")
                .offerReceivedBodyFr("Old OfferReceivedBodyFr")
                .offerStatusSubjectEn("Old OfferStatusSubjEn")
                .offerStatusBodyEn("Old OfferStatusBodyEn")
                .offerStatusSubjectFr("Old OfferStatusSubjFr")
                .offerStatusBodyFr("Old OfferStatusBodyFr")
                .build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel(
                "fr",
                "New Subject EN",
                "New Body EN",
                "New Subject FR",
                "New Body FR",
                "New DocSubjEn",
                "New DocBodyEn",
                "New DocSubjFr",
                "New DocBodyFr",
                "New DocReqSubjEn",
                "New DocReqBodyEn",
                "New DocReqSubjFr",
                "New DocReqBodyFr",
                "New DocRevSubjEn",
                "New DocRevBodyEn",
                "New DocRevSubjFr",
                "New DocRevBodyFr",
                "New StageSubjEn",
                "New StageBodyEn",
                "New StageSubjFr",
                "New StageBodyFr",
                "New OfferMadeSubjEn",
                "New OfferMadeBodyEn",
                "New OfferMadeSubjFr",
                "New OfferMadeBodyFr",
                "New OfferStatusSubjEn",
                "New OfferStatusBodyEn",
                "New OfferStatusSubjFr",
                "New OfferStatusBodyFr",
                "New OfferReceivedSubjEn",
                "New OfferReceivedBodyEn",
                "New OfferReceivedSubjFr",
                "New OfferReceivedBodyFr",
                "New OfferStatusSubjEn",
                "New OfferStatusBodyEn",
                "New OfferStatusSubjFr",
                "New OfferStatusBodyFr"
        );

        // Act
        mapper.updateEntityFromRequest(request, settings);

        // Assert
        assertThat(settings.getDefaultLanguage()).isEqualTo("fr");
        assertThat(settings.getInviteSubjectEn()).isEqualTo("New Subject EN");
        assertThat(settings.getInviteBodyEn()).isEqualTo("New Body EN");
        assertThat(settings.getInviteSubjectFr()).isEqualTo("New Subject FR");
        assertThat(settings.getInviteBodyFr()).isEqualTo("New Body FR");
        assertThat(settings.getDocumentSubmittedSubjectEn()).isEqualTo("New DocSubjEn");
        assertThat(settings.getDocumentSubmittedBodyEn()).isEqualTo("New DocBodyEn");
        assertThat(settings.getDocumentSubmittedSubjectFr()).isEqualTo("New DocSubjFr");
        assertThat(settings.getDocumentSubmittedBodyFr()).isEqualTo("New DocBodyFr");
        assertThat(settings.getDocumentRequestedSubjectEn()).isEqualTo("New DocReqSubjEn");
        assertThat(settings.getDocumentRequestedBodyEn()).isEqualTo("New DocReqBodyEn");
        assertThat(settings.getDocumentRequestedSubjectFr()).isEqualTo("New DocReqSubjFr");
        assertThat(settings.getDocumentRequestedBodyFr()).isEqualTo("New DocReqBodyFr");
        assertThat(settings.getDocumentReviewSubjectEn()).isEqualTo("New DocRevSubjEn");
        assertThat(settings.getDocumentReviewBodyEn()).isEqualTo("New DocRevBodyEn");
        assertThat(settings.getDocumentReviewSubjectFr()).isEqualTo("New DocRevSubjFr");
        assertThat(settings.getDocumentReviewBodyFr()).isEqualTo("New DocRevBodyFr");
        assertThat(settings.getStageUpdateSubjectEn()).isEqualTo("New StageSubjEn");
        assertThat(settings.getStageUpdateBodyEn()).isEqualTo("New StageBodyEn");
        assertThat(settings.getStageUpdateSubjectFr()).isEqualTo("New StageSubjFr");
        assertThat(settings.getStageUpdateBodyFr()).isEqualTo("New StageBodyFr");
        assertThat(settings.getPropertyOfferMadeSubjectEn()).isEqualTo("New OfferMadeSubjEn");
        assertThat(settings.getPropertyOfferMadeBodyEn()).isEqualTo("New OfferMadeBodyEn");
        assertThat(settings.getPropertyOfferMadeSubjectFr()).isEqualTo("New OfferMadeSubjFr");
        assertThat(settings.getPropertyOfferMadeBodyFr()).isEqualTo("New OfferMadeBodyFr");
        assertThat(settings.getPropertyOfferStatusSubjectEn()).isEqualTo("New OfferStatusSubjEn");
        assertThat(settings.getPropertyOfferStatusBodyEn()).isEqualTo("New OfferStatusBodyEn");
        assertThat(settings.getPropertyOfferStatusSubjectFr()).isEqualTo("New OfferStatusSubjFr");
        assertThat(settings.getPropertyOfferStatusBodyFr()).isEqualTo("New OfferStatusBodyFr");
        assertThat(settings.getOfferReceivedSubjectEn()).isEqualTo("New OfferReceivedSubjEn");
        assertThat(settings.getOfferReceivedBodyEn()).isEqualTo("New OfferReceivedBodyEn");
        assertThat(settings.getOfferReceivedSubjectFr()).isEqualTo("New OfferReceivedSubjFr");
        assertThat(settings.getOfferReceivedBodyFr()).isEqualTo("New OfferReceivedBodyFr");
        assertThat(settings.getOfferStatusSubjectEn()).isEqualTo("New OfferStatusSubjEn");
        assertThat(settings.getOfferStatusBodyEn()).isEqualTo("New OfferStatusBodyEn");
        assertThat(settings.getOfferStatusSubjectFr()).isEqualTo("New OfferStatusSubjFr");
        assertThat(settings.getOfferStatusBodyFr()).isEqualTo("New OfferStatusBodyFr");
    }

    @Test
    void updateEntityFromRequest_WithNullRequestOrSettings_DoesNothing() {
        OrganizationSettings settings = OrganizationSettings.builder().defaultLanguage("en").build();
        mapper.updateEntityFromRequest(null, settings);
        assertThat(settings.getDefaultLanguage()).isEqualTo("en");

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        mapper.updateEntityFromRequest(request, null); // Should not throw
    }
}
