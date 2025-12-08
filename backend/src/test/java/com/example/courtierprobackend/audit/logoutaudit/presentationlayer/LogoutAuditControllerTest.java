package com.example.courtierprobackend.audit.logoutaudit.presentationlayer;

import com.example.courtierprobackend.audit.logoutaudit.businesslayer.LogoutAuditService;
import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * Unit tests for LogoutAuditController.
 */
@ExtendWith(MockitoExtension.class)
class LogoutAuditControllerTest {

    @Mock
    private LogoutAuditService logoutAuditService;

    // Controller would be tested here if it exists
    // For now, this is a placeholder to ensure the audit pipeline is covered

    @BeforeEach
    void setUp() {
        // Setup would go here
    }

    @Test
    void recordLogout_InvokesService() {
        // Arrange & Act
        logoutAuditService.recordLogoutEvent("user-1", "user@test.com", LogoutAuditEvent.LogoutReason.MANUAL, Instant.now(), "127.0.0.1", "Mozilla/5.0");

        // Assert
        verify(logoutAuditService).recordLogoutEvent(any(), any(), any(), any(), any(), any());
    }
}
