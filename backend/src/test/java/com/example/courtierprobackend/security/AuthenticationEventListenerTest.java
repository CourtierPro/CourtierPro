package com.example.courtierprobackend.security;

import com.example.courtierprobackend.audit.loginaudit.businesslayer.LoginAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationEventListener.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationEventListenerTest {

    @Mock
    private LoginAuditService loginAuditService;

    @Mock
    private Authentication authentication;

    private AuthenticationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuthenticationEventListener(loginAuditService);
    }

    @Test
    void onAuthenticationSuccess_WithValidJwt_RecordsLoginEvent() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|user123")
                .claim("https://courtierpro.dev/email", "user@example.com")
                .claim("https://courtierpro.dev/roles", List.of("BROKER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(loginAuditService).recordLoginEvent(
                eq("auth0|user123"),
                eq("user@example.com"),
                eq("BROKER"),
                isNull(), // IP address null without request context
                isNull()  // User agent null without request context
        );
    }

    @Test
    void onAuthenticationSuccess_WithFallbackEmailClaim_UsesStandardEmail() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|user456")
                .claim("email", "fallback@example.com")
                .claim("https://courtierpro.dev/roles", List.of("CLIENT"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(loginAuditService).recordLoginEvent(
                eq("auth0|user456"),
                eq("fallback@example.com"),
                eq("CLIENT"),
                any(),
                any()
        );
    }

    @Test
    void onAuthenticationSuccess_WithNonJwtPrincipal_DoesNotRecord() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn("string-principal");
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verifyNoInteractions(loginAuditService);
    }

    @Test
    void onAuthenticationSuccess_WithNoEmail_DoesNotRecord() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|user789")
                .claim("https://courtierpro.dev/roles", List.of("ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verifyNoInteractions(loginAuditService);
    }

    @Test
    void onAuthenticationSuccess_WithNoRoles_RecordsUnknownRole() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|userNoRole")
                .claim("https://courtierpro.dev/email", "norole@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(loginAuditService).recordLoginEvent(
                eq("auth0|userNoRole"),
                eq("norole@example.com"),
                eq("UNKNOWN"),
                any(),
                any()
        );
    }

    @Test
    void onAuthenticationSuccess_WithEmptyEmailClaim_DoesNotRecord() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|userEmpty")
                .claim("https://courtierpro.dev/email", "")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert - falls back to regular email claim which is also not set
        verifyNoInteractions(loginAuditService);
    }

    @Test
    void onAuthenticationSuccess_WithEmptyRolesList_RecordsUnknownRole() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|userEmptyRoles")
                .claim("https://courtierpro.dev/email", "user@example.com")
                .claim("https://courtierpro.dev/roles", List.of())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(loginAuditService).recordLoginEvent(
                eq("auth0|userEmptyRoles"),
                eq("user@example.com"),
                eq("UNKNOWN"),
                any(),
                any()
        );
    }

    @Test
    void onAuthenticationSuccess_WithMultipleRoles_UsesFirstRole() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|userMultiRole")
                .claim("https://courtierpro.dev/email", "multi@example.com")
                .claim("https://courtierpro.dev/roles", List.of("ADMIN", "BROKER", "CLIENT"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act
        listener.onAuthenticationSuccess(event);

        // Assert - should use first role
        verify(loginAuditService).recordLoginEvent(
                eq("auth0|userMultiRole"),
                eq("multi@example.com"),
                eq("ADMIN"),
                any(),
                any()
        );
    }

    @Test
    void onAuthenticationSuccess_WhenServiceThrowsException_DoesNotBreakAuthentication() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|user123")
                .claim("https://courtierpro.dev/email", "user@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getPrincipal()).thenReturn(jwt);
        doThrow(new RuntimeException("Database connection failed"))
                .when(loginAuditService).recordLoginEvent(any(), any(), any(), any(), any());
        
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // Act - should not throw
        listener.onAuthenticationSuccess(event);

        // Assert - method completes without throwing (exception is caught and logged)
        verify(loginAuditService).recordLoginEvent(any(), any(), any(), any(), any());
    }
}