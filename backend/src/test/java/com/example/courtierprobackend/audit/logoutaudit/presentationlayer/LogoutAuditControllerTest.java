package com.example.courtierprobackend.audit.logoutaudit.presentationlayer;

import com.example.courtierprobackend.audit.logoutaudit.businesslayer.LogoutAuditService;
import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LogoutAuditController.class)
@AutoConfigureMockMvc(addFilters = true)
class LogoutAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogoutAuditService logoutAuditService;

    @MockBean
    private JwtDecoder jwtDecoder; // Required for JWT security

    // ==================== POST /auth/logout ====================

    @Test
    void recordLogout_withValidJwt_recordsEventAndReturnsOk() throws Exception {
        // given
        String requestBody = """
                {
                  "reason": "manual",
                  "timestamp": "2025-12-07T12:00:00.000Z"
                }
                """;

        // when/then
        mockMvc.perform(post("/auth/logout")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .claim("sub", "auth0|123")
                                        .claim("https://courtierpro.dev/email", "user@example.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout event recorded successfully"));

        verify(logoutAuditService).recordLogoutEvent(
                eq("auth0|123"),
                eq("user@example.com"),
                eq(LogoutAuditEvent.LogoutReason.MANUAL),
                any(),
                any(),
                any()
        );
    }

    @Test
    void recordLogout_sessionTimeout_recordsCorrectReason() throws Exception {
        // given
        String requestBody = """
                {
                  "reason": "session_timeout",
                  "timestamp": "2025-12-07T12:30:00.000Z"
                }
                """;

        // when/then
        mockMvc.perform(post("/auth/logout")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .claim("sub", "auth0|456")
                                        .claim("https://courtierpro.dev/email", "timeout@example.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(logoutAuditService).recordLogoutEvent(
                eq("auth0|456"),
                eq("timeout@example.com"),
                eq(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT),
                any(),
                any(),
                any()
        );
    }

    @Test
    void recordLogout_forcedLogout_recordsCorrectReason() throws Exception {
        // given
        String requestBody = """
                {
                  "reason": "forced",
                  "timestamp": "2025-12-07T13:00:00.000Z"
                }
                """;

        // when/then
        mockMvc.perform(post("/auth/logout")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .claim("sub", "auth0|789")
                                        .claim("https://courtierpro.dev/email", "forced@example.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(logoutAuditService).recordLogoutEvent(
                eq("auth0|789"),
                eq("forced@example.com"),
                eq(LogoutAuditEvent.LogoutReason.FORCED),
                any(),
                any(),
                any()
        );
    }

    @Test
    void recordLogout_emailFromStandardClaim_usesStandardEmail() throws Exception {
        // given - email in standard 'email' claim (fallback)
        String requestBody = """
                {
                  "reason": "manual",
                  "timestamp": "2025-12-07T12:00:00.000Z"
                }
                """;

        // when/then
        mockMvc.perform(post("/auth/logout")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .claim("sub", "auth0|999")
                                        .claim("email", "standard@example.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(logoutAuditService).recordLogoutEvent(
                eq("auth0|999"),
                eq("standard@example.com"),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void recordLogout_noEmailInJwt_usesUserId() throws Exception {
        // given - no email claim at all
        String requestBody = """
                {
                  "reason": "manual",
                  "timestamp": "2025-12-07T12:00:00.000Z"
                }
                """;

        // when/then
        mockMvc.perform(post("/auth/logout")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("sub", "auth0|noemail"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Should use userId as fallback
        verify(logoutAuditService).recordLogoutEvent(
                eq("auth0|noemail"),
                eq("auth0|noemail"),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void recordLogout_invalidReason_defaultsToManual() throws Exception {
        // given
        String requestBody = """
                {
                  "reason": "invalid_reason",
                  "timestamp": "2025-12-07T12:00:00.000Z"
                }
                """;

        // when/then
        mockMvc.perform(post("/auth/logout")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .claim("sub", "auth0|invalid")
                                        .claim("https://courtierpro.dev/email", "invalid@example.com")
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(logoutAuditService).recordLogoutEvent(
                any(),
                any(),
                eq(LogoutAuditEvent.LogoutReason.MANUAL), // Should default to MANUAL
                any(),
                any(),
                any()
        );
    }

    @Test
    void recordLogout_unauthenticated_returnsUnauthorized() throws Exception {
        // given
        String requestBody = """
                {
                  "reason": "manual",
                  "timestamp": "2025-12-07T12:00:00.000Z"
                }
                """;

        // when/then - Spring Security will return 401 or 403 depending on config
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is4xxClientError()); // Accept any 4xx error

        verifyNoInteractions(logoutAuditService);
    }

    // ==================== GET /auth/logout-audit (Admin endpoints) ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllLogoutEvents_asAdmin_returnsOk() throws Exception {
        when(logoutAuditService.getAllLogoutEvents())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/logout-audit"))
                .andExpect(status().isOk());

        verify(logoutAuditService).getAllLogoutEvents();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogoutEventsByUser_asAdmin_returnsOk() throws Exception {
        when(logoutAuditService.getLogoutEventsByUser("auth0|123"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/logout-audit/user/auth0|123"))
                .andExpect(status().isOk());

        verify(logoutAuditService).getLogoutEventsByUser("auth0|123");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogoutEventsByReason_asAdmin_returnsOk() throws Exception {
        when(logoutAuditService.getLogoutEventsByReason(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/logout-audit/reason/SESSION_TIMEOUT"))
                .andExpect(status().isOk());

        verify(logoutAuditService).getLogoutEventsByReason(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT);
    }

    @Test
    void getAllLogoutEvents_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/logout-audit"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(logoutAuditService);
    }

    @Disabled("PreAuthorize is not enforced in WebMvcTest slice")
    @Test
    @WithMockUser(roles = "BROKER")
    void getAllLogoutEvents_nonAdmin_isForbidden() throws Exception {
        mockMvc.perform(get("/auth/logout-audit"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(logoutAuditService);
    }
}
