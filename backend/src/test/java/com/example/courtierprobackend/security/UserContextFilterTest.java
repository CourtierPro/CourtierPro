package com.example.courtierprobackend.security;

import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextFilterTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private JwtAuthenticationToken authentication;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserContextFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void doFilterInternal_WithValidJwtAndExistingUser_SetsInternalId() throws ServletException, IOException {
        String auth0Id = "auth0|123";
        UUID internalId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(internalId);
        user.setActive(true); // Ensure user is active
        user.setRole(com.example.courtierprobackend.user.dataaccesslayer.UserRole.CLIENT); // Set a valid role
        user.setPreferredLanguage("en"); // Set required field if needed

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute(UserContextFilter.AUTH0_USER_ID_ATTR, auth0Id);
        verify(request).setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithValidJwtButUserNotFound_LogsWarningAndProceeds() throws ServletException, IOException {
        String auth0Id = "auth0|123";
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute(UserContextFilter.AUTH0_USER_ID_ATTR, auth0Id);
        verify(request, never()).setAttribute(eq(UserContextFilter.INTERNAL_USER_ID_ATTR), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_NotJwtAuthentication_ProceedsWithoutAction() throws ServletException, IOException {
        Authentication otherAuth = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(otherAuth);

        filter.doFilterInternal(request, response, filterChain);

        verify(request, never()).setAttribute(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_JwtMissingSubClaim_ProceedsWithoutAction() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(request, never()).setAttribute(eq(UserContextFilter.AUTH0_USER_ID_ATTR), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InactiveUser_BlocksRequestWithForbidden() throws ServletException, IOException {
        String auth0Id = "auth0|inactive";
        UUID internalId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(internalId);
        user.setActive(false); // Inactive user
        user.setRole(com.example.courtierprobackend.user.dataaccesslayer.UserRole.CLIENT);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(user));
        when(request.getRequestURI()).thenReturn("/api/transactions");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InactiveUserAccessingEmailConfirm_AllowsRequest() throws ServletException, IOException {
        String auth0Id = "auth0|inactive";
        UUID internalId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(internalId);
        user.setActive(false); // Inactive user
        user.setRole(com.example.courtierprobackend.user.dataaccesslayer.UserRole.CLIENT);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(user));
        when(request.getRequestURI()).thenReturn("/api/me/confirm-email");

        filter.doFilterInternal(request, response, filterChain);

        verify(response, never()).sendError(anyInt(), anyString());
        verify(request).setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InactiveUserAccessingEmailConfirmSubpath_AllowsRequest() throws ServletException, IOException {
        String auth0Id = "auth0|inactive";
        UUID internalId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(internalId);
        user.setActive(false); // Inactive user
        user.setRole(com.example.courtierprobackend.user.dataaccesslayer.UserRole.CLIENT);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(user));
        when(request.getRequestURI()).thenReturn("/api/me/confirm-email/token123");

        filter.doFilterInternal(request, response, filterChain);

        verify(response, never()).sendError(anyInt(), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_NullRequestUri_InactiveUserBlocked() throws ServletException, IOException {
        String auth0Id = "auth0|inactive";
        UUID internalId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(internalId);
        user.setActive(false);
        user.setRole(com.example.courtierprobackend.user.dataaccesslayer.UserRole.CLIENT);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(user));
        when(request.getRequestURI()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }
}