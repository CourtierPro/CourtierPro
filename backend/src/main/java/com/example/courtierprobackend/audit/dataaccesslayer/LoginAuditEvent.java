package com.example.courtierprobackend.audit.dataaccesslayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_audit_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId; // Auth0 sub claim

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Instant timestamp;

    @Column
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;
}
