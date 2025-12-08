package com.example.courtierprobackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CurrentUser record.
 */
class CurrentUserTest {

    @Test
    void brokerId_ReturnsSubjectFromJwt() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|broker123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String brokerId = currentUser.brokerId();

        // Assert
        assertThat(brokerId).isEqualTo("auth0|broker123");
    }

    @Test
    void email_ReturnsEmailClaimFromJwt() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|broker123")
                .claim("email", "broker@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String email = currentUser.email();

        // Assert
        assertThat(email).isEqualTo("broker@example.com");
    }

    @Test
    void name_ReturnsNameClaimFromJwt() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|broker123")
                .claim("name", "John Doe")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String name = currentUser.name();

        // Assert
        assertThat(name).isEqualTo("John Doe");
    }

    @Test
    void email_WithNullClaim_ReturnsNull() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|broker123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String email = currentUser.email();

        // Assert
        assertThat(email).isNull();
    }

    @Test
    void name_WithNullClaim_ReturnsNull() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|broker123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String name = currentUser.name();

        // Assert
        assertThat(name).isNull();
    }

    @Test
    void jwt_ReturnsOriginalJwt() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|broker123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act & Assert
        assertThat(currentUser.jwt()).isEqualTo(jwt);
    }
}
