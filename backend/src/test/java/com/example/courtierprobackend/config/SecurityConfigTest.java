package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    // --- SecurityFilterChain Integration Tests ---

    @org.junit.jupiter.api.Test
    void optionsRequestsArePermitted() throws Exception {
        // Use an existing endpoint, e.g. health
        mockMvc.perform(options("/actuator/health"))
            .andExpect(status().isOk());
    }

    @org.junit.jupiter.api.Test
    void healthEndpointsArePermitted() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
    // /actuator/health/sub does not exist by default, expect 404
    mockMvc.perform(get("/actuator/health/sub"))
        .andExpect(status().isNotFound());
    }

    @org.junit.jupiter.api.Test
    void actuatorEndpointsDenied() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isUnauthorized()); // 401 is expected for unauthenticated
    }

    @org.junit.jupiter.api.Test
    void emailConfirmationPermitted() throws Exception {
    mockMvc.perform(get("/api/me/confirm-email")
        .param("token", "dummy-token"))
        .andExpect(status().isBadRequest()); // Invalid token should return 400
    }

    @org.junit.jupiter.api.Test
    void logoutRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/auth/logout"))
        .andExpect(status().isUnauthorized());
    }

    @WithMockUser(roles = "BROKER")
    @org.junit.jupiter.api.Test
    void brokerEndpointsRequireBrokerRole() throws Exception {
    mockMvc.perform(get("/transactions/00000000-0000-0000-0000-000000000001/documents")
        .with(user("user").roles("BROKER")))
        .andExpect(status().isForbidden()); // Expect 403 since user id cannot be resolved
    }

    @WithMockUser(roles = "CLIENT")
    @org.junit.jupiter.api.Test
    void clientEndpointsRequireClientRole() throws Exception {
        // Use a valid UUID for transactionId
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";
        mockMvc.perform(get("/transactions/" + validUuid + "/timeline/client"))
            .andExpect(status().isOk());
    }

    @org.junit.jupiter.api.Test
    void otherEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/some/other/endpoint"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtAuthenticationConverter_shouldBeConfigured() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);

        // Act
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        // Assert
        assertThat(converter).isNotNull();
    }

    @Test
    void extractAuthoritiesFromJwt_withAdminRole_shouldReturnRoleAdmin() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Map<String, Object> claims = new HashMap<>();
        claims.put("https://courtierpro.dev/roles", List.of("ADMIN"));

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        // Act
        AbstractAuthenticationToken authToken = converter.convert(jwt);

        // Assert
        assertThat(authToken).isNotNull();
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void extractAuthoritiesFromJwt_withBrokerRole_shouldReturnRoleBroker() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Map<String, Object> claims = new HashMap<>();
        claims.put("https://courtierpro.dev/roles", List.of("BROKER"));

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        // Act
        AbstractAuthenticationToken authToken = converter.convert(jwt);

        // Assert
        assertThat(authToken).isNotNull();
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting("authority").containsExactly("ROLE_BROKER");
    }

    @Test
    void extractAuthoritiesFromJwt_withClientRole_shouldReturnRoleClient() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Map<String, Object> claims = new HashMap<>();
        claims.put("https://courtierpro.dev/roles", List.of("CLIENT"));

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        // Act
        AbstractAuthenticationToken authToken = converter.convert(jwt);

        // Assert
        assertThat(authToken).isNotNull();
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting("authority").containsExactly("ROLE_CLIENT");
    }

    @Test
    void extractAuthoritiesFromJwt_withMultipleRoles_shouldReturnAllRoles() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Map<String, Object> claims = new HashMap<>();
        claims.put("https://courtierpro.dev/roles", List.of("ADMIN", "BROKER", "CLIENT"));

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        // Act
        AbstractAuthenticationToken authToken = converter.convert(jwt);

        // Assert
        assertThat(authToken).isNotNull();
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        assertThat(authorities).hasSize(3);
        assertThat(authorities).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_BROKER", "ROLE_CLIENT");
    }

    @Test
    void extractAuthoritiesFromJwt_withNoRolesClaim_shouldReturnEmptyCollection() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "auth0|test123"); // Add a minimal claim so JWT is valid
        // No roles claim

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        // Act
        AbstractAuthenticationToken authToken = converter.convert(jwt);

        // Assert
        assertThat(authToken).isNotNull();
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        assertThat(authorities).isEmpty();
    }

    @Test
    void extractAuthoritiesFromJwt_withEmptyRolesList_shouldReturnEmptyCollection() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Map<String, Object> claims = new HashMap<>();
        claims.put("https://courtierpro.dev/roles", List.of());

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        // Act
        AbstractAuthenticationToken authToken = converter.convert(jwt);

        // Assert
        assertThat(authToken).isNotNull();
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        assertThat(authorities).isEmpty();
    }

    @Test
    void extractAuthoritiesFromJwt_withNonCollectionRolesClaim_shouldReturnEmptyCollection() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Map<String, Object> claims = new HashMap<>();
        claims.put("https://courtierpro.dev/roles", "ADMIN"); // String instead of Collection

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims
        );

        // Act
        AbstractAuthenticationToken authToken = converter.convert(jwt);

        // Assert
        assertThat(authToken).isNotNull();
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        assertThat(authorities).isEmpty();
    }

    @Test
    void authenticationEventPublisher_shouldBeCreated() {
        // Arrange
        SecurityConfig securityConfig = new SecurityConfig(null);

        // Act & Assert
        assertThat(securityConfig.authenticationEventPublisher()).isNotNull();
    }

    @Test
    void constructor_shouldAcceptCorsConfigurationSource() {
        // Arrange & Act
        SecurityConfig securityConfig = new SecurityConfig(null);

        // Assert
        assertThat(securityConfig).isNotNull();
    }
}
