package com.example.courtierprobackend.Organization.presentationlayer;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;

/**
 * Integration test for OrganizationSettingsController.
 * Tests HTTP request/response handling with mocked service layer.
 */
@WebMvcTest(value = OrganizationSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrganizationSettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationSettingsService organizationSettingsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public OrganizationSettingsService organizationSettingsService() {
            return Mockito.mock(OrganizationSettingsService.class);
        }
    }

    @BeforeEach
    void setUp() {
        reset(organizationSettingsService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSettings_ReturnsOk() throws Exception {
        // Arrange
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome")
                .inviteBodyEn("Body EN")
                .inviteSubjectFr("Bienvenue")
                .inviteBodyFr("Body FR")
                .updatedAt(Instant.now())
                .build();

        when(organizationSettingsService.getSettings()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/admin/settings")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLanguage").value("fr"))
                .andExpect(jsonPath("$.inviteSubjectEn").value("Welcome"))
                .andExpect(jsonPath("$.inviteSubjectFr").value("Bienvenue"));

        verify(organizationSettingsService).getSettings();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSettings_WithValidRequest_ReturnsOk() throws Exception {
        // Arrange
        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel(
                "en",
                "New Subject EN",
                "New Body EN",
                "New Subject FR",
                "New Body FR"
        );

        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("en")
                .inviteSubjectEn("New Subject EN")
                .inviteBodyEn("New Body EN")
                .inviteSubjectFr("New Subject FR")
                .inviteBodyFr("New Body FR")
                .updatedAt(Instant.now())
                .build();

        when(organizationSettingsService.updateSettings(any(UpdateOrganizationSettingsRequestModel.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/admin/settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLanguage").value("en"))
                .andExpect(jsonPath("$.inviteSubjectEn").value("New Subject EN"));

        verify(organizationSettingsService).updateSettings(any(UpdateOrganizationSettingsRequestModel.class));
    }

}
