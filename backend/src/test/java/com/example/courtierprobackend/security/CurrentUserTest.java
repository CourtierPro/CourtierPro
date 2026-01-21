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

    @Test
    void internalId_returnsUuidFromRequestAttribute() {
        // Arrange
        java.util.UUID expectedId = java.util.UUID.randomUUID();
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, expectedId);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        java.util.UUID result = currentUser.internalId();

        // Assert
        assertThat(result).isEqualTo(expectedId);

        // Cleanup
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void internalId_returnsNullWhenNoRequestAttributes() {
        // Arrange
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        java.util.UUID result = currentUser.internalId();

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void internalId_returnsNullWhenAttributeNotUuid() {
        // Arrange
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, "not-a-uuid");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        java.util.UUID result = currentUser.internalId();

        // Assert
        assertThat(result).isNull();

        // Cleanup
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void internalIdString_returnsStringRepresentation() {
        // Arrange
        java.util.UUID expectedId = java.util.UUID.randomUUID();
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, expectedId);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String result = currentUser.internalIdString();

        // Assert
        assertThat(result).isEqualTo(expectedId.toString());

        // Cleanup
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void internalIdString_returnsNullWhenNoInternalId() {
        // Arrange
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String result = currentUser.internalIdString();

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void auth0Id_returnsSubClaim() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|user123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        CurrentUser currentUser = new CurrentUser(jwt);

        // Act
        String result = currentUser.auth0Id();

        // Assert
        assertThat(result).isEqualTo("auth0|user123");
    }
}