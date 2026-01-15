package com.example.courtierprobackend.audit.passwordresetaudit.presentationlayer;

import com.example.courtierprobackend.audit.passwordresetaudit.businesslayer.PasswordResetAuditService;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Auth0WebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class Auth0WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordResetAuditService passwordResetAuditService;

    @MockBean
    private UserContextFilter userContextFilter;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void handleAuth0Event_withForgotPasswordEvent_shouldRecordRequest() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com",
                    "ip": "192.168.1.1",
                    "user_agent": "Mozilla/5.0"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("192.168.1.1"),
                        eq("Mozilla/5.0")
                );
    }

    @Test
    void handleAuth0Event_withSuccessfulChangePasswordEvent_shouldRecordCompletion() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "scp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com",
                    "ip": "192.168.1.1",
                    "user_agent": "Mozilla/5.0"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetCompletion(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("192.168.1.1"),
                        eq("Mozilla/5.0")
                );
    }

    @Test
    void handleAuth0Event_withFailedChangePasswordEvent_shouldNotRecordAnything() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fcp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
        verify(passwordResetAuditService, never()).recordPasswordResetCompletion(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withMissingUserId_shouldNotRecordEvent() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withMissingEmail_shouldNotRecordEvent() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withUnknownEventType_shouldReturnOk() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "unknown",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
        verify(passwordResetAuditService, never()).recordPasswordResetCompletion(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withEmailInUserName_shouldUseUserNameAsEmail() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "user_name": "test@example.com",
                    "ip": "192.168.1.1",
                    "user_agent": "Chrome/120.0"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("192.168.1.1"),
                        eq("Chrome/120.0")
                );
    }

    @Test
    void handleAuth0Event_whenServiceThrowsException_shouldReturnOkAndNotPropagate() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com",
                    "ip": "192.168.1.1",
                    "user_agent": "Mozilla/5.0"
                }
                """;

        doThrow(new RuntimeException("Database error"))
                .when(passwordResetAuditService)
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("192.168.1.1"),
                        eq("Mozilla/5.0")
                );

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("192.168.1.1"),
                        eq("Mozilla/5.0")
                );
    }

    @Test
    void handleAuth0Event_withCommaSeparatedIpAddresses_shouldUseFirstIp() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1, 192.0.2.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("203.0.113.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withOnlyUserName_shouldExtractEmailFromUserName() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "user_name": "user@domain.com",
                    "ip": "10.0.0.1"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("user@domain.com"),
                        eq("10.0.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withEmptyEmail_shouldUseUserNameIfAvailable() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "",
                    "user_name": "fallback@example.com",
                    "ip": "172.16.0.1"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("fallback@example.com"),
                        eq("172.16.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withNonEmailUserName_shouldNotRecordEvent() throws Exception {
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "user_name": "john_doe"
                }
                """;

        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never())
                .recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
    }
}
