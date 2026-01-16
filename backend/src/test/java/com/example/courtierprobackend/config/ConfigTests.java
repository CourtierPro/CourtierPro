package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for config classes (SecurityConfig, JwtConfig, CorsConfig).
 * Tests simple configuration beans and utility methods.
 */
class ConfigTests {

    @Test
    void jwtAuthenticationConverter_extractsRoles() {
        SecurityConfig securityConfig = new SecurityConfig(new CorsConfig().corsConfigurationSource());
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("https://courtierpro.dev/roles", List.of("ADMIN", "BROKER"))
                .build();

        var auth = converter.convert(jwt);

        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_BROKER");
    }

    @Test
    void jwtAuthenticationConverter_withEmptyRoles() {
        SecurityConfig securityConfig = new SecurityConfig(new CorsConfig().corsConfigurationSource());
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("https://courtierpro.dev/roles", List.of())
                .build();

        var auth = converter.convert(jwt);

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void corsConfigurationSource_containsExpectedOriginsAndHeaders() {
        CorsConfig corsConfig = new CorsConfig();
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(config.getAllowedOrigins()).contains("http://localhost:8081", "http://localhost:5173");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PATCH", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).contains("Authorization", "Content-Type", "x-broker-id");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsFilter_isNotNull() {
        CorsConfig corsConfig = new CorsConfig();
        assertThat(corsConfig.corsFilter()).isNotNull();
    }

    @Test
    void jwtDecoder_buildsWithConfiguredDomain() {
        JwtConfig jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "domain", "example.auth0.com");

        assertThat(jwtConfig.jwtDecoder()).isNotNull();
    }
}
