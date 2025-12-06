package com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationSettingsAuditEventRepository
        extends JpaRepository<OrganizationSettingsAuditEvent, UUID> {

    List<OrganizationSettingsAuditEvent> findAllByOrderByTimestampDesc();
}
