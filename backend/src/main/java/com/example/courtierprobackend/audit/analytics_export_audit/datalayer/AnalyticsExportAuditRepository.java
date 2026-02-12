package com.example.courtierprobackend.audit.analytics_export_audit.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalyticsExportAuditRepository extends JpaRepository<AnalyticsExportAuditEvent, UUID> {
}
