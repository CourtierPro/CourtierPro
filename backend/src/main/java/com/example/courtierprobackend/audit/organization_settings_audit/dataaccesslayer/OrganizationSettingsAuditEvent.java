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

    // When the change happened
    private Instant timestamp;

    // Admin who changed the settings
    private String adminUserId;   // e.g. Auth0 sub
    private String adminEmail;

    // Simple action label
    private String action;        // e.g. "ORGANIZATION_SETTINGS_UPDATED"

    // What changed (keep it simple for now)
    private String previousDefaultLanguage;
    private String newDefaultLanguage;
}
