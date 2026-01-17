package com.example.courtierprobackend.Organization.businesslayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettingsRepository;
import com.example.courtierprobackend.Organization.datamapperlayer.OrganizationSettingsMapper;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
import com.example.courtierprobackend.audit.organization_settings_audit.businesslayer.OrganizationSettingsAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizationSettingsServiceImpl.
 */
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

    private OrganizationSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrganizationSettingsServiceImpl(repository, mapper, auditService, httpServletRequest);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSettings_WithExistingSettings_ReturnsSettings() {
        // Arrange
        OrganizationSettings settings = OrganizationSettings.builder()
                .id(UUID.randomUUID())
                .defaultLanguage("en")
                .build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(settings));
        when(mapper.toResponseModel(settings)).thenReturn(response);

        // Act
        OrganizationSettingsResponseModel result = service.getSettings();

        // Assert
        assertThat(result.getDefaultLanguage()).isEqualTo("en");
    }

    @Test
    void getSettings_WithNoSettings_CreatesDefaultSettings() {
        // Arrange
        OrganizationSettings defaultSettings = OrganizationSettings.builder()
                .defaultLanguage("fr")
                .build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .build();

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(defaultSettings);
        when(mapper.toResponseModel(defaultSettings)).thenReturn(response);

        // Act
        OrganizationSettingsResponseModel result = service.getSettings();

        // Assert
        assertThat(result.getDefaultLanguage()).isEqualTo("fr");
        verify(repository).save(any());
    }

    @Test
    void updateSettings_WithValidRequest_UpdatesAndAudits() {
        // Arrange
        OrganizationSettings existing = OrganizationSettings.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("Old Subject")
                .build();
        OrganizationSettings saved = OrganizationSettings.builder()
                .defaultLanguage("fr")
                .inviteSubjectEn("New Subject")
                .updatedAt(Instant.now())
                .build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("fr");
        request.setInviteSubjectEn("New Subject");
        request.setInviteBodyEn("body en");
        request.setInviteSubjectFr("Subject Fr");
        request.setInviteBodyFr("body fr");

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponseModel(saved)).thenReturn(response);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        OrganizationSettingsResponseModel result = service.updateSettings(request);

        // Assert
        assertThat(result.getDefaultLanguage()).isEqualTo("fr");
        verify(auditService).recordSettingsUpdated(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void updateSettings_WithJwtPrincipal_ExtractsUserIdFromToken() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|admin123")
                .claim("email", "admin@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        OrganizationSettings existing = OrganizationSettings.builder()
                .defaultLanguage("en")
                .build();
        OrganizationSettings saved = OrganizationSettings.builder()
                .defaultLanguage("fr")
                .updatedAt(Instant.now())
                .build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder().build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("fr");
        request.setInviteSubjectEn("Subject");
        request.setInviteBodyEn("Body");
        request.setInviteSubjectFr("Sujet");
        request.setInviteBodyFr("Corps");

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponseModel(saved)).thenReturn(response);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act
        service.updateSettings(request);

        // Assert
        verify(auditService).recordSettingsUpdated(
                eq("auth0|admin123"), eq("admin@example.com"), eq("192.168.1.1"),
                any(), any(), anyBoolean(), anyBoolean()
        );
    }

    @Test
    void updateSettings_WithJwtWithoutEmail_UsesUnknownEmail() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|admin456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        OrganizationSettings existing = OrganizationSettings.builder().defaultLanguage("en").build();
        OrganizationSettings saved = OrganizationSettings.builder().defaultLanguage("fr").updatedAt(Instant.now()).build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder().build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("fr");
        request.setInviteSubjectEn("Subject");
        request.setInviteBodyEn("Body");
        request.setInviteSubjectFr("Sujet");
        request.setInviteBodyFr("Corps");

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponseModel(saved)).thenReturn(response);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        service.updateSettings(request);

        // Assert
        verify(auditService).recordSettingsUpdated(
                eq("auth0|admin456"), eq("unknown"), any(), any(), any(), anyBoolean(), anyBoolean()
        );
    }

    @Test
    void updateSettings_WithNonJwtAuth_UsesAuthName() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("regular-user");
        when(auth.getName()).thenReturn("regular-user");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        OrganizationSettings existing = OrganizationSettings.builder().defaultLanguage("en").build();
        OrganizationSettings saved = OrganizationSettings.builder().defaultLanguage("fr").updatedAt(Instant.now()).build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder().build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("fr");
        request.setInviteSubjectEn("Subject");
        request.setInviteBodyEn("Body");
        request.setInviteSubjectFr("Sujet");
        request.setInviteBodyFr("Corps");

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponseModel(saved)).thenReturn(response);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        service.updateSettings(request);

        // Assert
        verify(auditService).recordSettingsUpdated(
                eq("regular-user"), any(), any(), any(), any(), anyBoolean(), anyBoolean()
        );
    }

    @Test
    void updateSettings_WithNoTemplateChanges_DetectsNoChanges() {
        // Arrange
        OrganizationSettings existing = OrganizationSettings.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("Same Subject")
                .inviteBodyEn("Same Body")
                .inviteSubjectFr("Même Sujet")
                .inviteBodyFr("Même Corps")
                .build();
        OrganizationSettings saved = OrganizationSettings.builder()
                .defaultLanguage("fr")
                .updatedAt(Instant.now())
                .build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder().build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("fr");
        request.setInviteSubjectEn("Same Subject");
        request.setInviteBodyEn("Same Body");
        request.setInviteSubjectFr("Même Sujet");
        request.setInviteBodyFr("Même Corps");

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponseModel(saved)).thenReturn(response);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act
        service.updateSettings(request);

        // Assert - both template change flags should be false
        verify(auditService).recordSettingsUpdated(
                any(), any(), any(), any(), any(), eq(false), eq(false)
        );
    }

    @Test
    void updateSettings_WithNullHttpRequest_UsesUnknownIp() {
        // Arrange
        OrganizationSettingsServiceImpl serviceWithNullRequest = 
                new OrganizationSettingsServiceImpl(repository, mapper, auditService, null);

        OrganizationSettings existing = OrganizationSettings.builder().defaultLanguage("en").build();
        OrganizationSettings saved = OrganizationSettings.builder().defaultLanguage("fr").updatedAt(Instant.now()).build();
        OrganizationSettingsResponseModel response = OrganizationSettingsResponseModel.builder().build();

        UpdateOrganizationSettingsRequestModel request = new UpdateOrganizationSettingsRequestModel();
        request.setDefaultLanguage("fr");
        request.setInviteSubjectEn("Subject");
        request.setInviteBodyEn("Body");
        request.setInviteSubjectFr("Sujet");
        request.setInviteBodyFr("Corps");

        when(repository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponseModel(saved)).thenReturn(response);

        // Act
        serviceWithNullRequest.updateSettings(request);

        // Assert
        verify(auditService).recordSettingsUpdated(
                any(), any(), eq("unknown"), any(), any(), anyBoolean(), anyBoolean()
        );
    }
}

