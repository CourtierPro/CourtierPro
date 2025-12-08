package com.example.courtierprobackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig filter chain.
 * Uses @SpringBootTest with H2 in-memory database to load full application context
 * and test security configuration including CORS, authentication, and authorization.
 * 
 * These tests increase code coverage on SecurityConfig.securityFilterChain() method
 * by exercising the actual Spring Security filter chain with real HTTP requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== CORS Configuration Tests ==========

    @Test
    void corsOrigin_allowsLocalhost8081() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                .header("Origin", "http://localhost:8081"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void corsOrigin_allowsLocalhost5173() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                .header("Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }



    @Test
    void corsOrigin_allowsCourtierDomain() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                .header("Origin", "https://courtierproapp.sraldon.work"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://courtierproapp.sraldon.work"));
    }

    // ========== OPTIONS Preflight Tests ==========

    @Test
    void optionsPreflight_returnsOkForAllEndpoints() throws Exception {
        mockMvc.perform(options("/api/v1/transactions")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void optionsPreflight_allowsPostMethod() throws Exception {
        mockMvc.perform(options("/api/v1/admin/users")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void optionsPreflight_allowsAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/v1/transactions")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    // ========== Actuator Endpoints Tests ==========

    @Test
    void actuatorHealth_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheus_isDenied() throws Exception {
        // SecurityConfig explicitly denies all actuator endpoints except health
        mockMvc.perform(get("/actuator/prometheus")
                .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorMetrics_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorInfo_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/info")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ========== Admin Endpoints Authentication Tests ==========

    @Test
    void adminUsersEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminOrganizationsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/organizations")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminUsersPost_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminSettingsPut_requiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/admin/organizations/settings/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ========== Transaction Endpoints Authentication Tests ==========

    @Test
    void transactionsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transactionsPost_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void brokerTransactionsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/broker/transactions")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void brokerClientsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/broker/clients")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ========== Organization Settings Endpoints Tests ==========

    @Test
    void organizationSettingsEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/settings")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void organizationAuditEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/settings/audit")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ========== CSRF Protection Tests ==========

    @Test
    void csrfProtection_isDisabledForStatelessAPI() throws Exception {
        // POST without CSRF token should fail with 401 (auth issue), not 403 (CSRF issue)
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized()); // Not 403 Forbidden
    }

    // ========== Content Type Tests ==========

    @Test
    void jsonContentType_isAcceptedForPosts() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"test\": \"data\"}"))
                .andExpect(status().isUnauthorized()); // Fails on auth, not content type
    }

    @Test
    void xmlContentType_shouldNotCauseServerError() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_XML)
                .content("<test>data</test>"))
                .andExpect(status().isUnauthorized()); // Fails on auth first
    }
}
