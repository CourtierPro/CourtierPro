package com.example.courtierprobackend.audit.logoutaudit.businesslayer;

import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEvent;
import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LogoutAuditService.
 */
@ExtendWith(MockitoExtension.class)
class LogoutAuditServiceTest {

    @Mock
    private LogoutAuditEventRepository repository;

    private LogoutAuditService service;

    @BeforeEach
    void setUp() {
        service = new LogoutAuditService(repository);
    }

    @Test
    void recordLogoutEvent_SavesEventWithAllFields() {
        // Act
        service.recordLogoutEvent("user-1", "user@test.com", LogoutAuditEvent.LogoutReason.MANUAL, Instant.now(), "127.0.0.1", "Mozilla/5.0");

        // Assert
        ArgumentCaptor<LogoutAuditEvent> captor = ArgumentCaptor.forClass(LogoutAuditEvent.class);
        verify(repository).save(captor.capture());
        
        LogoutAuditEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo("user-1");
        assertThat(event.getEmail()).isEqualTo("user@test.com");
        assertThat(event.getReason()).isEqualTo(LogoutAuditEvent.LogoutReason.MANUAL);
    }

    @Test
    void recordLogoutEvent_WithException_DoesNotThrow() {
        // Arrange
        when(repository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Act & Assert - should not throw
        service.recordLogoutEvent("user-1", "user@test.com", LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT, Instant.now(), "127.0.0.1", "Mozilla/5.0");
    }

    @Test
    void getAllLogoutEvents_ReturnsAllEvents() {
        // Arrange
        List<LogoutAuditEvent> events = List.of(
                LogoutAuditEvent.builder().userId("u1").build(),
                LogoutAuditEvent.builder().userId("u2").build()
        );
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);

        // Act
        List<LogoutAuditEvent> result = service.getAllLogoutEvents();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void getLogoutEventsByUser_ReturnsUserEvents() {
        // Arrange
        List<LogoutAuditEvent> events = List.of(
                LogoutAuditEvent.builder().userId("user-1").build()
        );
        when(repository.findByUserIdOrderByTimestampDesc("user-1")).thenReturn(events);

        // Act
        List<LogoutAuditEvent> result = service.getLogoutEventsByUser("user-1");

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void getLogoutEventsByReason_ReturnsReasonEvents() {
        // Arrange
        List<LogoutAuditEvent> events = List.of(
                LogoutAuditEvent.builder().reason(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT).build()
        );
        when(repository.findByReasonOrderByTimestampDesc(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT)).thenReturn(events);

        // Act
        List<LogoutAuditEvent> result = service.getLogoutEventsByReason(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT);

        // Assert
        assertThat(result).hasSize(1);
    }
}
