// backend/src/main/java/com/example/courtierprobackend/audit/organization_settings_audit/businesslayer/OrganizationSettingsAuditService.java
package com.example.courtierprobackend.audit.organization_settings_audit.businesslayer;

import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEvent;
import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationSettingsAuditService {

    private final OrganizationSettingsAuditEventRepository repository;

    public void recordSettingsUpdated(
            String adminUserId,
            String adminEmail,
            String ipAddress,
            String previousDefaultLanguage,
            String newDefaultLanguage,
            boolean inviteTemplateEnChanged,
            boolean inviteTemplateFrChanged
    ) {
        OrganizationSettingsAuditEvent event = OrganizationSettingsAuditEvent.builder()
                .timestamp(Instant.now())
                .adminUserId(adminUserId)
                .adminEmail(adminEmail)
                .ipAddress(ipAddress)
                .action("ORGANIZATION_SETTINGS_UPDATED")
                .previousDefaultLanguage(previousDefaultLanguage)
                .newDefaultLanguage(newDefaultLanguage)
                .inviteTemplateEnChanged(inviteTemplateEnChanged)
                .inviteTemplateFrChanged(inviteTemplateFrChanged)
                .build();

        repository.save(event);
    }

    public List<OrganizationSettingsAuditEvent> getAllAuditEvents() {
        return repository.findAllByOrderByTimestampDesc();
    }
}
