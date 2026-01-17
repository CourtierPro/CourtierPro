package com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationSettingsAuditEventRepository
        extends JpaRepository<OrganizationSettingsAuditEvent, UUID> {

    List<OrganizationSettingsAuditEvent> findAllByOrderByTimestampDesc();

    // Custom query to fetch only the latest N audit events
    @org.springframework.data.jpa.repository.Query("SELECT e FROM OrganizationSettingsAuditEvent e ORDER BY e.timestamp DESC")
    List<OrganizationSettingsAuditEvent> findTopNByOrderByTimestampDesc(org.springframework.data.domain.Pageable pageable);

    default List<OrganizationSettingsAuditEvent> findTopNByOrderByTimestampDesc(int limit) {
        return findTopNByOrderByTimestampDesc(org.springframework.data.domain.PageRequest.of(0, limit));
    }
}
