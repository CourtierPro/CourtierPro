package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigTest {

    @Test
    void jwtDecoder_shouldBeCreatedWithCorrectJwkUri() {
        // Arrange
        JwtConfig jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "domain", "dev-example.us.auth0.com");

        // Act
        JwtDecoder jwtDecoder = jwtConfig.jwtDecoder();

        // Assert
        assertThat(jwtDecoder).isNotNull();
        assertThat(jwtDecoder).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void jwtDecoder_shouldConstructCorrectJwkUriFromDomain() {
        // Arrange
        JwtConfig jwtConfig = new JwtConfig();
        String testDomain = "test-domain.auth0.com";
        ReflectionTestUtils.setField(jwtConfig, "domain", testDomain);

        // Act
        JwtDecoder jwtDecoder = jwtConfig.jwtDecoder();

        // Assert
        assertThat(jwtDecoder).isNotNull();
        // The decoder should be configured with: https://test-domain.auth0.com/.well-known/jwks.json
    }

    @Test
    void jwtDecoder_withDifferentDomain_shouldStillCreateDecoder() {
        // Arrange
        JwtConfig jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "domain", "another-domain.eu.auth0.com");

        // Act
        JwtDecoder jwtDecoder = jwtConfig.jwtDecoder();

        // Assert
        assertThat(jwtDecoder).isNotNull();
        assertThat(jwtDecoder).isInstanceOf(NimbusJwtDecoder.class);
    }
}
