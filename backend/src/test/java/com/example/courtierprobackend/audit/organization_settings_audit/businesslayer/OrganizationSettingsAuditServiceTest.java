package com.example.courtierprobackend.audit.organization_settings_audit.businesslayer;

import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEvent;
import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizationSettingsAuditService.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationSettingsAuditServiceTest {

    @Mock
    private OrganizationSettingsAuditEventRepository repository;

    private OrganizationSettingsAuditService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationSettingsAuditService(repository);
    }

    @Test
    void recordSettingsUpdated_SavesEventWithAllFields() {
        // Act
        service.recordSettingsUpdated("admin-1", "admin@test.com", "127.0.0.1", "en", "fr", true, false);

        // Assert
        ArgumentCaptor<OrganizationSettingsAuditEvent> captor = ArgumentCaptor.forClass(OrganizationSettingsAuditEvent.class);
        verify(repository).save(captor.capture());
        
        OrganizationSettingsAuditEvent event = captor.getValue();
        assertThat(event.getAdminUserId()).isEqualTo("admin-1");
        assertThat(event.getAdminEmail()).isEqualTo("admin@test.com");
        assertThat(event.getPreviousDefaultLanguage()).isEqualTo("en");
        assertThat(event.getNewDefaultLanguage()).isEqualTo("fr");
        assertThat(event.getInviteTemplateEnChanged()).isTrue();
        assertThat(event.getInviteTemplateFrChanged()).isFalse();
    }

    @Test
    void recordSettingsUpdated_WithNoChanges_StillSavesEvent() {
        // Act
        service.recordSettingsUpdated("admin-1", "admin@test.com", "127.0.0.1", "en", "en", false, false);

        // Assert
        verify(repository).save(any());
    }

    @Test
    void getAllAuditEvents_ReturnsAllEvents() {
        // Arrange
        List<OrganizationSettingsAuditEvent> events = List.of(
                OrganizationSettingsAuditEvent.builder().adminUserId("a1").build(),
                OrganizationSettingsAuditEvent.builder().adminUserId("a2").build()
        );
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);

        // Act
        List<OrganizationSettingsAuditEvent> result = service.getAllAuditEvents();

        // Assert
        assertThat(result).hasSize(2);
    }
}
