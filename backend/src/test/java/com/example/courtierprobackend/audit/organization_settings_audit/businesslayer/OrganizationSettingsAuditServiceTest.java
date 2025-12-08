package com.example.courtierprobackend.audit.organization_settings_audit.businesslayer;

import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEvent;
import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * Unit test for OrganizationSettingsAuditService.
 * Tests audit event creation and repository delegation.
 */
class OrganizationSettingsAuditServiceTest {

    @Mock
    private OrganizationSettingsAuditEventRepository repository;

    @InjectMocks
    private OrganizationSettingsAuditService auditService;

    @Test
    void recordSettingsUpdated_buildsAndSavesEvent() {
        String adminUserId = "admin-123";
        String adminEmail = "admin@example.com";
        String ipAddress = "192.0.2.10";
        String previousLang = "en";
        String newLang = "fr";
        boolean inviteEnChanged = true;
        boolean inviteFrChanged = false;

        auditService.recordSettingsUpdated(
                adminUserId,
                adminEmail,
                ipAddress,
                previousLang,
                newLang,
                inviteEnChanged,
                inviteFrChanged
        );

        ArgumentCaptor<OrganizationSettingsAuditEvent> captor = ArgumentCaptor.forClass(OrganizationSettingsAuditEvent.class);
        verify(repository).save(captor.capture());

        OrganizationSettingsAuditEvent saved = captor.getValue();
        assertThat(saved.getAdminUserId()).isEqualTo(adminUserId);
        assertThat(saved.getAdminEmail()).isEqualTo(adminEmail);
        assertThat(saved.getIpAddress()).isEqualTo(ipAddress);
        assertThat(saved.getPreviousDefaultLanguage()).isEqualTo(previousLang);
        assertThat(saved.getNewDefaultLanguage()).isEqualTo(newLang);
        assertThat(saved.getInviteTemplateEnChanged()).isEqualTo(inviteEnChanged);
        assertThat(saved.getInviteTemplateFrChanged()).isEqualTo(inviteFrChanged);
        assertThat(saved.getAction()).isEqualTo("ORGANIZATION_SETTINGS_UPDATED");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void getAllAuditEvents_delegatesToRepository() {
        List<OrganizationSettingsAuditEvent> events = List.of(mock(OrganizationSettingsAuditEvent.class));
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);

        List<OrganizationSettingsAuditEvent> result = auditService.getAllAuditEvents();

        verify(repository).findAllByOrderByTimestampDesc();
        assertThat(result).isEqualTo(events);
    }
}
