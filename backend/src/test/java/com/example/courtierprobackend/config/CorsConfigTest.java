package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_shouldConfigureAllowedOrigins() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // Act
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        // Assert
        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactlyInAnyOrder(
                "http://localhost:8081",
                "http://localhost:5173",
                "http://localhost:3000",
                "https://courtierproapp.sraldon.work");
    }

    @Test
    void corsConfigurationSource_shouldConfigureAllowedMethods() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // Act
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(request);

        // Assert
        assertThat(config.getAllowedMethods()).containsExactlyInAnyOrder(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    void corsConfigurationSource_shouldConfigureAllowedHeaders() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // Act
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(request);

        // Assert
        assertThat(config.getAllowedHeaders()).containsExactlyInAnyOrder(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "x-broker-id");
    }

    @Test
    void corsConfigurationSource_shouldEnableCredentials() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // Act
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(request);

        // Assert
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigurationSource_shouldSetMaxAge() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // Act
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(request);

        // Assert
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsConfigurationSource_shouldApplyToAllPaths() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        // Act & Assert - Test various paths
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRequestURI("/api/admin/users");
        assertThat(source.getCorsConfiguration(request1)).isNotNull();

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRequestURI("/api/transactions");
        assertThat(source.getCorsConfiguration(request2)).isNotNull();

        MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.setRequestURI("/actuator/health");
        assertThat(source.getCorsConfiguration(request3)).isNotNull();

        MockHttpServletRequest request4 = new MockHttpServletRequest();
        request4.setRequestURI("/any/random/path");
        assertThat(source.getCorsConfiguration(request4)).isNotNull();
    }

    @Test
    void corsFilter_shouldBeCreated() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();

        // Act
        CorsFilter corsFilter = corsConfig.corsFilter();

        // Assert
        assertThat(corsFilter).isNotNull();
    }

    @Test
    void corsFilter_shouldUseConfigurationSource() {
        // Arrange
        CorsConfig corsConfig = new CorsConfig();
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        // Act
        CorsFilter corsFilter = corsConfig.corsFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // Assert
        assertThat(corsFilter).isNotNull();
        // Verify the configuration is accessible
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
    }
}
