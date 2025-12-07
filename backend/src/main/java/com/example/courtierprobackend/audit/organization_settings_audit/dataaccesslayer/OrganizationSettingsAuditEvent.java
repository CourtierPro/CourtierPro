// backend/src/main/java/com/example/courtierprobackend/audit/organization_settings_audit/dataaccesslayer/OrganizationSettingsAuditEvent.java
package com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_settings_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettingsAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Instant timestamp;

    private String adminUserId;
    private String adminEmail;

    @Column(name = "ip_address")
    private String ipAddress;

    private String action;

    private String previousDefaultLanguage;
    private String newDefaultLanguage;

    private Boolean inviteTemplateEnChanged;
    private Boolean inviteTemplateFrChanged;
}
