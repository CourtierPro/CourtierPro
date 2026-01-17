package com.example.courtierprobackend.audit.organization_settings_audit.presentationlayer;

import com.example.courtierprobackend.audit.organization_settings_audit.businesslayer.OrganizationSettingsAuditService;
import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationSettingsAuditController {

    private final OrganizationSettingsAuditService auditService;

    @GetMapping
    public ResponseEntity<List<OrganizationSettingsAuditEvent>> getAuditLogs(@RequestParam(value = "limit", required = false) Integer limit) {
        if (limit != null && limit > 0) {
            return ResponseEntity.ok(auditService.getRecentAuditEvents(limit));
        } else {
            return ResponseEntity.ok(auditService.getAllAuditEvents());
        }
    }
}
