package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

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
