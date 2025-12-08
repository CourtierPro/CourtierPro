package com.example.courtierprobackend.Organization.businesslayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettingsRepository;
import com.example.courtierprobackend.Organization.datamapperlayer.OrganizationSettingsMapper;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
import com.example.courtierprobackend.audit.organization_settings_audit.businesslayer.OrganizationSettingsAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationSettingsServiceImplTest {

    @Mock
    private OrganizationSettingsRepository repository;

    @Mock
    private OrganizationSettingsMapper mapper;

    @Mock
    private OrganizationSettingsAuditService auditService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private OrganizationSettingsServiceImpl service;

    @Test
    void getSettings_WhenSettingsExist_ReturnsSettings() {
        // Arrange
        OrganizationSettings existingSettings = OrganizationSettings.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome")
                .inviteBodyEn("Body EN")
                .inviteSubjectFr("Bienvenue")
                .inviteBodyFr("Body FR")
                .updatedAt(Instant.now())
                .build();

        OrganizationSettingsResponseModel responseModel = OrganizationSettingsResponseModel.builder()
                .id(existingSettings.getId())
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome")
                .inviteBodyEn("Body EN")
                .inviteSubjectFr("Bienvenue")
                .inviteBodyFr("Body FR")
                .updatedAt(existingSettings.getUpdatedAt())
                .build();

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existingSettings));
        when(mapper.toResponseModel(existingSettings)).thenReturn(responseModel);

        // Act
        OrganizationSettingsResponseModel result = service.getSettings();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDefaultLanguage()).isEqualTo("fr");
        assertThat(result.getInviteSubjectEn()).isEqualTo("Welcome");
        verify(repository).findTopByOrderByUpdatedAtDesc();
        verify(mapper).toResponseModel(existingSettings);
    }

    @Test
    void getSettings_WhenNoSettings_CreatesDefaultSettings() {
        // Arrange
        OrganizationSettings defaultSettings = OrganizationSettings.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome to CourtierPro")
                .inviteBodyEn("Hi {{name}}, your CourtierPro account has been created.")
                .inviteSubjectFr("Bienvenue sur CourtierPro")
                .inviteBodyFr("Bonjour {{name}}, votre compte CourtierPro a été créé.")
                .updatedAt(Instant.now())
                .build();

        OrganizationSettingsResponseModel responseModel = OrganizationSettingsResponseModel.builder()
                .id(defaultSettings.getId())
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome to CourtierPro")
                .inviteBodyEn("Hi {{name}}, your CourtierPro account has been created.")
                .inviteSubjectFr("Bienvenue sur CourtierPro")
                .inviteBodyFr("Bonjour {{name}}, votre compte CourtierPro a été créé.")
                .updatedAt(defaultSettings.getUpdatedAt())
                .build();

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(repository.save(any(OrganizationSettings.class))).thenReturn(defaultSettings);
        when(mapper.toResponseModel(defaultSettings)).thenReturn(responseModel);

        // Act
        OrganizationSettingsResponseModel result = service.getSettings();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDefaultLanguage()).isEqualTo("fr");
        assertThat(result.getInviteSubjectEn()).isEqualTo("Welcome to CourtierPro");
        verify(repository).findTopByOrderByUpdatedAtDesc();
        verify(repository).save(any(OrganizationSettings.class));
        verify(mapper).toResponseModel(defaultSettings);
    }

    @Test
    void updateSettings_WithValidData_UpdatesSuccessfully() {
        // Arrange
        OrganizationSettings existingSettings = OrganizationSettings.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("fr")
                .inviteSubjectEn("Old Subject EN")
                .inviteBodyEn("Old Body EN")
                .inviteSubjectFr("Old Subject FR")
                .inviteBodyFr("Old Body FR")
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel(
                "en",
                "New Subject EN",
                "New Body EN",
                "New Subject FR",
                "New Body FR"
        );

        OrganizationSettings updatedSettings = OrganizationSettings.builder()
                .id(existingSettings.getId())
                .defaultLanguage("en")
                .inviteSubjectEn("New Subject EN")
                .inviteBodyEn("New Body EN")
                .inviteSubjectFr("New Subject FR")
                .inviteBodyFr("New Body FR")
                .updatedAt(Instant.now())
                .build();

        OrganizationSettingsResponseModel responseModel = OrganizationSettingsResponseModel.builder()
                .id(updatedSettings.getId())
                .defaultLanguage("en")
                .inviteSubjectEn("New Subject EN")
                .inviteBodyEn("New Body EN")
                .inviteSubjectFr("New Subject FR")
                .inviteBodyFr("New Body FR")
                .updatedAt(updatedSettings.getUpdatedAt())
                .build();

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existingSettings));
        when(repository.save(any(OrganizationSettings.class))).thenReturn(updatedSettings);
        when(mapper.toResponseModel(updatedSettings)).thenReturn(responseModel);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = mock(Jwt.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn("auth0|123456");
        when(jwt.getClaims()).thenReturn(Map.of("email", "admin@test.com"));
        SecurityContextHolder.setContext(securityContext);

        // Act
        OrganizationSettingsResponseModel result = service.updateSettings(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDefaultLanguage()).isEqualTo("en");
        assertThat(result.getInviteSubjectEn()).isEqualTo("New Subject EN");
        verify(repository).findTopByOrderByUpdatedAtDesc();
        verify(repository).save(any(OrganizationSettings.class));
        verify(mapper).toResponseModel(updatedSettings);
        verify(auditService).recordSettingsUpdated(
                eq("auth0|123456"),
                eq("admin@test.com"),
                eq("127.0.0.1"),
                eq("fr"),
                eq("en"),
                eq(true),
                eq(true)
        );

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateSettings_LogsAuditCorrectly() {
        // Arrange
        OrganizationSettings existingSettings = OrganizationSettings.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("fr")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN")
                .inviteSubjectFr("Subject FR")
                .inviteBodyFr("Body FR")
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel(
                "en",
                "Subject EN",  // No change
                "Body EN",        // No change
                "New Subject FR",  // Changed
                "Body FR"        // No change in body
        );

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existingSettings));
        when(repository.save(any(OrganizationSettings.class))).thenReturn(existingSettings);
        when(mapper.toResponseModel(any())).thenReturn(mock(OrganizationSettingsResponseModel.class));
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin-user");
        when(authentication.getPrincipal()).thenReturn("admin-user");
        SecurityContextHolder.setContext(securityContext);

        // Act
        service.updateSettings(request);

        // Assert
        verify(auditService).recordSettingsUpdated(
                eq("admin-user"),
                eq("unknown"),
                eq("192.168.1.1"),
                eq("fr"),
                eq("en"),
                eq(false),  // EN template not changed
                eq(true)    // FR template changed (subject)
        );

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDefaultSettings_CreatesWithCorrectDefaults() {
        // Arrange
        OrganizationSettings defaultSettings = OrganizationSettings.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome to CourtierPro")
                .inviteBodyEn("Hi {{name}}, your CourtierPro account has been created.")
                .inviteSubjectFr("Bienvenue sur CourtierPro")
                .inviteBodyFr("Bonjour {{name}}, votre compte CourtierPro a été créé.")
                .updatedAt(Instant.now())
                .build();

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(repository.save(any(OrganizationSettings.class))).thenReturn(defaultSettings);
        when(mapper.toResponseModel(any())).thenReturn(mock(OrganizationSettingsResponseModel.class));

        // Act
        service.getSettings();

        // Assert
        verify(repository).save(any(OrganizationSettings.class));
    }
}
