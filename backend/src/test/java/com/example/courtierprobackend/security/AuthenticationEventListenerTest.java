package com.example.courtierprobackend.security;

import com.example.courtierprobackend.audit.businesslayer.LoginAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthenticationEventListenerTest {

    private LoginAuditService loginAuditService;
    private AuthenticationEventListener listener;

    @BeforeEach
    void setUp() {
        loginAuditService = mock(LoginAuditService.class);
        listener = new AuthenticationEventListener(loginAuditService);
    }

    @AfterEach
    void tearDown() {
        // Clean up any request attributes between tests
        RequestContextHolder.resetRequestAttributes();
    }

    private AuthenticationSuccessEvent buildEventWithPrincipal(Object principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        return new AuthenticationSuccessEvent(authentication);
    }

    @Test
    void onAuthenticationSuccess_recordsLoginEventWithXForwardedForAndUserAgent() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "auth0|123456")
                .claim("https://courtierpro.dev/email", "user@example.com")
                .claim("https://courtierpro.dev/roles", List.of("ADMIN"))
                .build();

        AuthenticationSuccessEvent event = buildEventWithPrincipal(jwt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        request.addHeader("User-Agent", "JUnit-Agent");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        listener.onAuthenticationSuccess(event);

        verify(loginAuditService).recordLoginEvent(
                eq("auth0|123456"),
                eq("user@example.com"),
                eq("ADMIN"),
                eq("203.0.113.5"),
                eq("JUnit-Agent")
        );
    }

    @Test
    void onAuthenticationSuccess_usesStandardEmailClaimAsFallback() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "auth0|fallback")
                // no custom email claim
                .claim("email", "fallback@example.com")
                // no roles → should map to "UNKNOWN"
                .build();

        AuthenticationSuccessEvent event = buildEventWithPrincipal(jwt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        listener.onAuthenticationSuccess(event);

        verify(loginAuditService).recordLoginEvent(
                eq("auth0|fallback"),
                eq("fallback@example.com"),
                eq("UNKNOWN"),
                eq("192.0.2.10"),
                isNull()
        );
    }

    @Test
    void onAuthenticationSuccess_doesNotRecordEventWhenEmailMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "auth0|no-email")
                // no email claims at all
                .build();

        AuthenticationSuccessEvent event = buildEventWithPrincipal(jwt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        listener.onAuthenticationSuccess(event);

        // Should exit early and never call the audit service
        verifyNoInteractions(loginAuditService);
    }

    @Test
    void onAuthenticationSuccess_swallowsExceptionsFromAuditService() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "auth0|boom")
                .claim("https://courtierpro.dev/email", "boom@example.com")
                .claim("https://courtierpro.dev/roles", List.of("BROKER"))
                .build();

        AuthenticationSuccessEvent event = buildEventWithPrincipal(jwt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        doThrow(new RuntimeException("DB down"))
                .when(loginAuditService)
                .recordLoginEvent(any(), any(), any(), any(), any());

        // Method should catch the exception and not rethrow it
        assertDoesNotThrow(() -> listener.onAuthenticationSuccess(event));
    }
}