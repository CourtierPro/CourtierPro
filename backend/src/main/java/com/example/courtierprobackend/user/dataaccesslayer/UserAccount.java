package com.example.courtierprobackend.user.dataaccesslayer;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String auth0UserId; // ex: "auth0|abc123"

    @Column(nullable = false, unique = true)
    private String email;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String preferredLanguage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;


    protected UserAccount() {
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
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }


    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }


}
