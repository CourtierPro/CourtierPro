package com.example.courtierprobackend.audit.presentationlayer;

import com.example.courtierprobackend.audit.businesslayer.PasswordResetAuditService;
import com.example.courtierprobackend.audit.presentationlayer.controller.Auth0WebhookController;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordResetAuditService passwordResetAuditService;

    @Test
    void handleAuth0Event_withForgotPasswordEvent_shouldRecordRequest() throws Exception {
        // Arrange
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

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("127.0.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withSuccessfulChangePasswordEvent_shouldRecordCompletion() throws Exception {
        // Arrange
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

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetCompletion(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("127.0.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withFailedChangePasswordEvent_shouldNotRecordAnything() throws Exception {
        // Arrange
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fcp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
        verify(passwordResetAuditService, never()).recordPasswordResetCompletion(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withMissingUserId_shouldNotRecordEvent() throws Exception {
        // Arrange
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "email": "test@example.com"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withMissingEmail_shouldNotRecordEvent() throws Exception {
        // Arrange
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withUnknownEventType_shouldReturnOk() throws Exception {
        // Arrange
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "unknown",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never()).recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
        verify(passwordResetAuditService, never()).recordPasswordResetCompletion(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleAuth0Event_withEmailInUserName_shouldUseUserNameAsEmail() throws Exception {
        // Arrange
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "user_name": "test@example.com",
                    "ip": "192.168.1.1"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("127.0.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_whenServiceThrowsException_shouldReturnOkAndNotPropagate() throws Exception {
        // Arrange
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com"
                }
                """;

        doThrow(new RuntimeException("Database error"))
                .when(passwordResetAuditService)
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("127.0.0.1"),
                        eq(null)
                );

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("test@example.com"),
                        eq("127.0.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withCommaSeparatedIpAddresses_shouldUseFirstIp() throws Exception {
        // Arrange - simulating X-Forwarded-For with multiple IPs
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "test@example.com"
                }
                """;

        // Act & Assert - MockMvc will use 127.0.0.1, but this tests the extraction logic
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
        // Arrange - email field is missing, but user_name contains email
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "user_name": "user@domain.com"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("user@domain.com"),
                        eq("127.0.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withEmptyEmail_shouldUseUserNameIfAvailable() throws Exception {
        // Arrange - empty email string, user_name has valid email
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "email": "",
                    "user_name": "fallback@example.com"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, times(1))
                .recordPasswordResetRequest(
                        eq("auth0|test123"),
                        eq("fallback@example.com"),
                        eq("127.0.0.1"),
                        eq(null)
                );
    }

    @Test
    void handleAuth0Event_withNonEmailUserName_shouldNotRecordEvent() throws Exception {
        // Arrange - no email and user_name doesn't contain @
        String eventJson = """
                {
                    "log_id": "12345",
                    "type": "fp",
                    "date": "2025-12-07T10:00:00Z",
                    "user_id": "auth0|test123",
                    "user_name": "john_doe"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/auth0-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());

        verify(passwordResetAuditService, never())
                .recordPasswordResetRequest(anyString(), anyString(), anyString(), anyString());
    }
}
