package com.example.courtierprobackend.Organization.presentationlayer;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizationSettingsController.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationSettingsControllerTest {

    @Mock
    private OrganizationSettingsService organizationSettingsService;

    private OrganizationSettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new OrganizationSettingsController(organizationSettingsService);
    }

    @Test
    void getSettings_ReturnsCurrentSettings() {
        // Arrange
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("Welcome")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        // Act
        ResponseEntity<OrganizationSettingsResponseModel> response = controller.getSettings();

        // Assert
        assertThat(response.getBody().getDefaultLanguage()).isEqualTo("en");
    }

    @Test
    void getSettings_DelegatesCorrectlyToService() {
        // Arrange
        when(organizationSettingsService.getSettings()).thenReturn(OrganizationSettingsResponseModel.builder().build());

        // Act
        controller.getSettings();

        // Assert
        verify(organizationSettingsService).getSettings();
    }

    @Test
    void updateSettings_UpdatesAndReturnsNewSettings() {
        // Arrange
        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("fr");
        request.setInviteSubjectEn("Subject EN");
        request.setInviteBodyEn("Body EN");
        request.setInviteSubjectFr("Subject FR");
        request.setInviteBodyFr("Body FR");
        OrganizationSettingsResponseModel updated = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .build();
        when(organizationSettingsService.updateSettings(any())).thenReturn(updated);

        // Act
        ResponseEntity<OrganizationSettingsResponseModel> response = controller.updateSettings(request);

        // Assert
        assertThat(response.getBody().getDefaultLanguage()).isEqualTo("fr");
    }

    @Test
    void updateSettings_DelegatesCorrectlyToService() {
        // Arrange
        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("en");
        request.setInviteSubjectEn("S");
        request.setInviteBodyEn("B");
        request.setInviteSubjectFr("S");
        request.setInviteBodyFr("B");
        when(organizationSettingsService.updateSettings(request)).thenReturn(OrganizationSettingsResponseModel.builder().build());

        // Act
        controller.updateSettings(request);

        // Assert
        verify(organizationSettingsService).updateSettings(request);
    }
}
