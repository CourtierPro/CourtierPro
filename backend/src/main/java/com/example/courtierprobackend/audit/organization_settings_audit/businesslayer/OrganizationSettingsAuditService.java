package com.example.courtierprobackend.audit.organization_settings_audit.businesslayer;

import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEvent;
import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrganizationSettingsAuditService {

    private final OrganizationSettingsAuditEventRepository repository;

    // Simple helper to record an update
    public void recordSettingsUpdated(
            String adminUserId,
            String adminEmail,
            String previousDefaultLanguage,
            String newDefaultLanguage
    ) {
        OrganizationSettingsAuditEvent event = OrganizationSettingsAuditEvent.builder()
                .timestamp(Instant.now())
                .adminUserId(adminUserId)
                .adminEmail(adminEmail)
                .action("ORGANIZATION_SETTINGS_UPDATED")
                .previousDefaultLanguage(previousDefaultLanguage)
                .newDefaultLanguage(newDefaultLanguage)
                .build();

        repository.save(event);
    }
}
