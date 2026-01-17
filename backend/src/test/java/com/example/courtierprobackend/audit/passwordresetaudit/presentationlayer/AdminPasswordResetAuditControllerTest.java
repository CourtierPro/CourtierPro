package com.example.courtierprobackend.audit.passwordresetaudit.presentationlayer;

import com.example.courtierprobackend.audit.passwordresetaudit.businesslayer.PasswordResetAuditService;
import com.example.courtierprobackend.audit.passwordresetaudit.dataaccesslayer.PasswordResetEvent;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPasswordResetAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPasswordResetAuditControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PasswordResetAuditService passwordResetAuditService;

        @MockBean
        private UserContextFilter userContextFilter;

        @MockBean
        private UserAccountRepository userAccountRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPasswordResetEvents_asAdmin_shouldReturnEvents() throws Exception {
        // Arrange
        PasswordResetEvent event1 = PasswordResetEvent.builder()
                .userId("auth0|test123")
                .email("test@example.com")
                .eventType(PasswordResetEvent.ResetEventType.REQUESTED)
                .timestamp(Instant.parse("2025-12-07T10:00:00Z"))
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();

        PasswordResetEvent event2 = PasswordResetEvent.builder()
                .userId("auth0|test123")
                .email("test@example.com")
                .eventType(PasswordResetEvent.ResetEventType.COMPLETED)
                .timestamp(Instant.parse("2025-12-07T10:05:00Z"))
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();

        when(passwordResetAuditService.getAllPasswordResetEvents())
                .thenReturn(List.of(event1, event2));

        // Act & Assert
        mockMvc.perform(get("/api/admin/password-reset-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value("auth0|test123"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].eventType").value("REQUESTED"))
                .andExpect(jsonPath("$[0].ipAddress").value("192.168.1.1"))
                .andExpect(jsonPath("$[1].eventType").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPasswordResetEvents_whenNoEvents_shouldReturnEmptyList() throws Exception {
        // Arrange
        when(passwordResetAuditService.getAllPasswordResetEvents())
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/password-reset-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPasswordResetEvents_shouldMapAllFieldsCorrectly() throws Exception {
        // Arrange - Test with event that has all fields including null userAgent
        PasswordResetEvent eventWithNullUserAgent = PasswordResetEvent.builder()
                .userId("auth0|user456")
                .email("user@example.com")
                .eventType(PasswordResetEvent.ResetEventType.REQUESTED)
                .timestamp(Instant.parse("2025-12-07T15:30:00Z"))
                .ipAddress("10.0.0.1")
                .userAgent(null) // Test null userAgent
                .build();

        when(passwordResetAuditService.getAllPasswordResetEvents())
                .thenReturn(List.of(eventWithNullUserAgent));

        // Act & Assert
        mockMvc.perform(get("/api/admin/password-reset-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value("auth0|user456"))
                .andExpect(jsonPath("$[0].email").value("user@example.com"))
                .andExpect(jsonPath("$[0].eventType").value("REQUESTED"))
                .andExpect(jsonPath("$[0].ipAddress").value("10.0.0.1"))
                .andExpect(jsonPath("$[0].userAgent").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPasswordResetEvents_withMultipleEvents_shouldPreserveOrder() throws Exception {
        // Arrange - Test stream processing with multiple events
        PasswordResetEvent event1 = PasswordResetEvent.builder()
                .userId("auth0|user1")
                .email("user1@example.com")
                .eventType(PasswordResetEvent.ResetEventType.REQUESTED)
                .timestamp(Instant.parse("2025-12-07T10:00:00Z"))
                .ipAddress("192.168.1.1")
                .userAgent("Chrome")
                .build();

        PasswordResetEvent event2 = PasswordResetEvent.builder()
                .userId("auth0|user2")
                .email("user2@example.com")
                .eventType(PasswordResetEvent.ResetEventType.COMPLETED)
                .timestamp(Instant.parse("2025-12-07T11:00:00Z"))
                .ipAddress("192.168.1.2")
                .userAgent("Firefox")
                .build();

        PasswordResetEvent event3 = PasswordResetEvent.builder()
                .userId("auth0|user3")
                .email("user3@example.com")
                .eventType(PasswordResetEvent.ResetEventType.REQUESTED)
                .timestamp(Instant.parse("2025-12-07T12:00:00Z"))
                .ipAddress("192.168.1.3")
                .userAgent("Safari")
                .build();

        when(passwordResetAuditService.getAllPasswordResetEvents())
                .thenReturn(List.of(event1, event2, event3));

        // Act & Assert - Verify all events are mapped and order is preserved
        mockMvc.perform(get("/api/admin/password-reset-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].userId").value("auth0|user1"))
                .andExpect(jsonPath("$[0].userAgent").value("Chrome"))
                .andExpect(jsonPath("$[1].userId").value("auth0|user2"))
                .andExpect(jsonPath("$[1].userAgent").value("Firefox"))
                .andExpect(jsonPath("$[2].userId").value("auth0|user3"))
                .andExpect(jsonPath("$[2].userAgent").value("Safari"));
    }
}
