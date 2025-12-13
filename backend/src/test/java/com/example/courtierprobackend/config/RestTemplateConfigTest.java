package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RestTemplateConfig.
 */
class RestTemplateConfigTest {

    @Test
    void restTemplate_CreatesNonNullBean() {
        RestTemplateConfig config = new RestTemplateConfig();
        
        RestTemplate restTemplate = config.restTemplate();
        
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void restTemplate_HasRequestFactory() {
        RestTemplateConfig config = new RestTemplateConfig();
        
        RestTemplate restTemplate = config.restTemplate();
        
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }

    @Test
    void restTemplate_CanBeUsedForRequests() {
        RestTemplateConfig config = new RestTemplateConfig();
        
        RestTemplate restTemplate = config.restTemplate();
        
        // Verify the RestTemplate has interceptors list (even if empty)
        assertThat(restTemplate.getInterceptors()).isNotNull();
    }

    @Test
    void restTemplate_HasMessageConverters() {
        RestTemplateConfig config = new RestTemplateConfig();
        
        RestTemplate restTemplate = config.restTemplate();
        
        // RestTemplate should have default message converters
        assertThat(restTemplate.getMessageConverters()).isNotEmpty();
    }

    @Test
    void restTemplate_CreatesNewInstanceEachTime() {
        RestTemplateConfig config = new RestTemplateConfig();
        
        RestTemplate restTemplate1 = config.restTemplate();
        RestTemplate restTemplate2 = config.restTemplate();
        
        // Each call should create a new instance (not the same object reference)
        assertThat(restTemplate1).isNotSameAs(restTemplate2);
    }
}
