package com.example.courtierprobackend.audit.logoutaudit.businesslayer;

import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEvent;
import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private LogoutAuditService logoutAuditService;

    @Test
    void recordLogoutEvent_buildsAndSavesEvent() {
        // given
        String userId = "auth0|123";
        String email = "user@example.com";
        LogoutAuditEvent.LogoutReason reason = LogoutAuditEvent.LogoutReason.MANUAL;
        Instant timestamp = Instant.now();
        String ipAddress = "203.0.113.5";
        String userAgent = "JUnit-Agent";

        // when
        logoutAuditService.recordLogoutEvent(userId, email, reason, timestamp, ipAddress, userAgent);

        // then
        ArgumentCaptor<LogoutAuditEvent> captor = ArgumentCaptor.forClass(LogoutAuditEvent.class);
        verify(repository).save(captor.capture());

        LogoutAuditEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getReason()).isEqualTo(reason);
        assertThat(saved.getTimestamp()).isEqualTo(timestamp);
        assertThat(saved.getIpAddress()).isEqualTo(ipAddress);
        assertThat(saved.getUserAgent()).isEqualTo(userAgent);
    }

    @Test
    void recordLogoutEvent_sessionTimeout_savesCorrectReason() {
        // given
        LogoutAuditEvent.LogoutReason reason = LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT;

        // when
        logoutAuditService.recordLogoutEvent(
                "auth0|456",
                "timeout@example.com",
                reason,
                Instant.now(),
                "192.168.1.1",
                "Browser"
        );

        // then
        ArgumentCaptor<LogoutAuditEvent> captor = ArgumentCaptor.forClass(LogoutAuditEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT);
    }

    @Test
    void recordLogoutEvent_forcedLogout_savesCorrectReason() {
        // given
        LogoutAuditEvent.LogoutReason reason = LogoutAuditEvent.LogoutReason.FORCED;

        // when
        logoutAuditService.recordLogoutEvent(
                "auth0|789",
                "forced@example.com",
                reason,
                Instant.now(),
                "10.0.0.1",
                "Admin-Tool"
        );

        // then
        ArgumentCaptor<LogoutAuditEvent> captor = ArgumentCaptor.forClass(LogoutAuditEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(LogoutAuditEvent.LogoutReason.FORCED);
    }

    @Test
    void recordLogoutEvent_exceptionDuringSave_doesNotThrow() {
        // given
        when(repository.save(any())).thenThrow(new RuntimeException("DB Error"));

        // when/then - should not throw exception (logout must succeed even if logging fails)
        logoutAuditService.recordLogoutEvent(
                "auth0|999",
                "error@example.com",
                LogoutAuditEvent.LogoutReason.MANUAL,
                Instant.now(),
                "127.0.0.1",
                "Test"
        );

        // verify save was attempted
        verify(repository).save(any(LogoutAuditEvent.class));
    }

    @Test
    void getAllLogoutEvents_delegatesToRepository() {
        // given
        List<LogoutAuditEvent> events = List.of(mock(LogoutAuditEvent.class));
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);

        // when
        List<LogoutAuditEvent> result = logoutAuditService.getAllLogoutEvents();

        // then
        verify(repository).findAllByOrderByTimestampDesc();
        assertThat(result).isEqualTo(events);
    }

    @Test
    void getLogoutEventsByUser_delegatesToRepository() {
        // given
        String userId = "auth0|456";
        List<LogoutAuditEvent> events = List.of(mock(LogoutAuditEvent.class));
        when(repository.findByUserIdOrderByTimestampDesc(userId)).thenReturn(events);

        // when
        List<LogoutAuditEvent> result = logoutAuditService.getLogoutEventsByUser(userId);

        // then
        verify(repository).findByUserIdOrderByTimestampDesc(userId);
        assertThat(result).isEqualTo(events);
    }

    @Test
    void getLogoutEventsByReason_delegatesToRepository() {
        // given
        LogoutAuditEvent.LogoutReason reason = LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT;
        List<LogoutAuditEvent> events = List.of(mock(LogoutAuditEvent.class));
        when(repository.findByReasonOrderByTimestampDesc(reason)).thenReturn(events);

        // when
        List<LogoutAuditEvent> result = logoutAuditService.getLogoutEventsByReason(reason);

        // then
        verify(repository).findByReasonOrderByTimestampDesc(reason);
        assertThat(result).isEqualTo(events);
    }
}
