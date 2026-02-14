package com.example.courtierprobackend.user.dataaccesslayer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    private UUID id;

    @Column(name = "auth0user_id", nullable = false, unique = true)
    private String auth0UserId; // ex: "auth0|abc123"

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(nullable = false)
    private boolean inAppNotificationsEnabled = true;

    @Column(nullable = false)
    private boolean weeklyDigestEnabled = true;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, length = 10)
    private String preferredLanguage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public UserAccount() {
    }

    public UserAccount(String auth0UserId,
            String email,
            String firstName,
            String lastName,
            UserRole role,
            String preferredLanguage) {

        this.id = UUID.randomUUID();
        this.auth0UserId = auth0UserId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.preferredLanguage = preferredLanguage;
        this.emailNotificationsEnabled = true;
        this.inAppNotificationsEnabled = true;
        this.weeklyDigestEnabled = true;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
        this.updatedAt = Instant.now();
    }

    public void setEmailNotificationsEnabled(boolean enabled) {
        this.emailNotificationsEnabled = enabled;
        this.updatedAt = Instant.now();
    }

    public void setInAppNotificationsEnabled(boolean enabled) {
        this.inAppNotificationsEnabled = enabled;
        this.updatedAt = Instant.now();
    }

    public void setWeeklyDigestEnabled(boolean enabled) {
        this.weeklyDigestEnabled = enabled;
        this.updatedAt = Instant.now();
    }

}
