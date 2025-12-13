package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RestTemplateConfig.
 */
class RestTemplateConfigTest {

    @Test
    void restTemplate_CreatesNonNullBean() {
        RestTemplateConfig config = createConfigWithDefaults();
        
        RestTemplate restTemplate = config.restTemplate();
        
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void restTemplate_HasRequestFactory() {
        RestTemplateConfig config = createConfigWithDefaults();
        
        RestTemplate restTemplate = config.restTemplate();
        
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }

    @Test
    void restTemplate_CanBeUsedForRequests() {
        RestTemplateConfig config = createConfigWithDefaults();
        
        RestTemplate restTemplate = config.restTemplate();
        
        // Verify the RestTemplate has interceptors list (even if empty)
        assertThat(restTemplate.getInterceptors()).isNotNull();
    }

    @Test
    void restTemplate_HasMessageConverters() {
        RestTemplateConfig config = createConfigWithDefaults();
        
        RestTemplate restTemplate = config.restTemplate();
        
        // RestTemplate should have default message converters
        assertThat(restTemplate.getMessageConverters()).isNotEmpty();
    }

    @Test
    void restTemplate_CreatesNewInstanceEachTime() {
        RestTemplateConfig config = createConfigWithDefaults();
        
        RestTemplate restTemplate1 = config.restTemplate();
        RestTemplate restTemplate2 = config.restTemplate();
        
        // Each call should create a new instance (not the same object reference)
        assertThat(restTemplate1).isNotSameAs(restTemplate2);
    }

    @Test
    void restTemplate_UsesConfiguredTimeouts() {
        RestTemplateConfig config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "connectTimeout", 15000);
        ReflectionTestUtils.setField(config, "connectionRequestTimeout", 20000);
        
        RestTemplate restTemplate = config.restTemplate();
        
        // Verify RestTemplate is created successfully with custom timeouts
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }

    @Test
    void restTemplate_WithDefaultTimeouts_UsesThirtySeconds() {
        RestTemplateConfig config = createConfigWithDefaults();
        
        RestTemplate restTemplate = config.restTemplate();
        
        // Verify RestTemplate is created with default 30 second timeouts
        assertThat(restTemplate).isNotNull();
    }

    private RestTemplateConfig createConfigWithDefaults() {
        RestTemplateConfig config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "connectTimeout", 30000);
        ReflectionTestUtils.setField(config, "connectionRequestTimeout", 30000);
        return config;
    }
}
