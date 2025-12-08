package com.example.courtierprobackend.audit.passwordresetaudit.dataaccesslayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track password reset events for audit purposes.
 * Records when users request password resets and when they complete them.
 */
@Entity
@Table(name = "password_reset_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId; // Auth0 sub claim

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResetEventType eventType; // REQUESTED or COMPLETED

    @Column(nullable = false)
    private Instant timestamp;

    @Column
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    public enum ResetEventType {
        REQUESTED,  // User clicked "Forgot Password" and email was sent
        COMPLETED   // User successfully reset their password
    }
}
