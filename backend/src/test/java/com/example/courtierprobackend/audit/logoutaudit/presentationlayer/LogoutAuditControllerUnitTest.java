package com.example.courtierprobackend.audit.logoutaudit.presentationlayer;

import com.example.courtierprobackend.audit.logoutaudit.businesslayer.LogoutAuditService;
import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LogoutAuditController.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class LogoutAuditControllerUnitTest {

    @Mock
    private LogoutAuditService logoutAuditService;
    @Mock
    private Jwt jwt;
    @Mock
    private HttpServletRequest httpRequest;

    private LogoutAuditController controller;

    @BeforeEach
    void setUp() {
        controller = new LogoutAuditController(logoutAuditService);
    }

    // ========== Record Logout Tests ==========

    @Test
    void recordLogout_WithValidRequest_RecordsEvent() {
        // Arrange
        when(jwt.getSubject()).thenReturn("user-1");
        when(jwt.getClaimAsString("https://courtierpro.dev/email")).thenReturn("user@test.com");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        
        LogoutAuditController.LogoutRequestDto request = new LogoutAuditController.LogoutRequestDto("manual", "2025-12-08T12:00:00.000Z");

        // Act
        ResponseEntity<Map<String, String>> response = controller.recordLogout(request, jwt, httpRequest);

        // Assert
        assertThat(response.getBody().get("message")).isEqualTo("Logout event recorded successfully");
        verify(logoutAuditService).recordLogoutEvent(eq("user-1"), eq("user@test.com"), eq(LogoutAuditEvent.LogoutReason.MANUAL), any(), any(), any());
    }

    @Test
    void recordLogout_WithSessionTimeoutReason_UsesCorrectEnum() {
        // Arrange
        when(jwt.getSubject()).thenReturn("user-1");
        when(jwt.getClaimAsString("https://courtierpro.dev/email")).thenReturn("user@test.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        
        LogoutAuditController.LogoutRequestDto request = new LogoutAuditController.LogoutRequestDto("session_timeout", null);

        // Act
        controller.recordLogout(request, jwt, httpRequest);

        // Assert
        verify(logoutAuditService).recordLogoutEvent(any(), any(), eq(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT), any(), any(), any());
    }

    @Test
    void recordLogout_WithFallbackEmail_UsesStandardEmailClaim() {
        // Arrange
        when(jwt.getSubject()).thenReturn("user-1");
        when(jwt.getClaimAsString("https://courtierpro.dev/email")).thenReturn(null);
        when(jwt.getClaimAsString("email")).thenReturn("fallback@test.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        
        LogoutAuditController.LogoutRequestDto request = new LogoutAuditController.LogoutRequestDto(null, null);

        // Act
        controller.recordLogout(request, jwt, httpRequest);

        // Assert
        verify(logoutAuditService).recordLogoutEvent(any(), eq("fallback@test.com"), any(), any(), any(), any());
    }

    // ========== Get All Events Tests ==========

    @Test
    void getAllLogoutEvents_ReturnsAllEvents() {
        // Arrange
        List<LogoutAuditEvent> events = List.of(
                LogoutAuditEvent.builder().id(UUID.randomUUID()).userId("u1").build(),
                LogoutAuditEvent.builder().id(UUID.randomUUID()).userId("u2").build()
        );
        when(logoutAuditService.getAllLogoutEvents()).thenReturn(events);

        // Act
        List<LogoutAuditEvent> result = controller.getAllLogoutEvents();

        // Assert
        assertThat(result).hasSize(2);
    }

    // ========== Get Events By User Tests ==========

    @Test
    void getLogoutEventsByUser_ReturnsUserEvents() {
        // Arrange
        List<LogoutAuditEvent> events = List.of(
                LogoutAuditEvent.builder().userId("user-1").build()
        );
        when(logoutAuditService.getLogoutEventsByUser("user-1")).thenReturn(events);

        // Act
        List<LogoutAuditEvent> result = controller.getLogoutEventsByUser("user-1");

        // Assert
        assertThat(result).hasSize(1);
    }

    // ========== Get Events By Reason Tests ==========

    @Test
    void getLogoutEventsByReason_WithValidReason_ReturnsEvents() {
        // Arrange
        List<LogoutAuditEvent> events = List.of(
                LogoutAuditEvent.builder().reason(LogoutAuditEvent.LogoutReason.MANUAL).build()
        );
        when(logoutAuditService.getLogoutEventsByReason(LogoutAuditEvent.LogoutReason.MANUAL)).thenReturn(events);

        // Act
        ResponseEntity<List<LogoutAuditEvent>> response = controller.getLogoutEventsByReason("MANUAL");

        // Assert
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getLogoutEventsByReason_WithLowercaseReason_ParsesCorrectly() {
        // Arrange
        when(logoutAuditService.getLogoutEventsByReason(LogoutAuditEvent.LogoutReason.FORCED)).thenReturn(List.of());

        // Act
        controller.getLogoutEventsByReason("forced");

        // Assert
        verify(logoutAuditService).getLogoutEventsByReason(LogoutAuditEvent.LogoutReason.FORCED);
    }
}
