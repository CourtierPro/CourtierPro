package com.example.courtierprobackend.audit.businesslayer;

import com.example.courtierprobackend.audit.dataaccesslayer.LoginAuditEvent;
import com.example.courtierprobackend.audit.dataaccesslayer.LoginAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginAuditService.
 */
@ExtendWith(MockitoExtension.class)
class LoginAuditServiceTest {

    @Mock
    private LoginAuditEventRepository repository;

    @InjectMocks
    private LoginAuditService loginAuditService;

    @Test
    void recordLoginEvent_buildsAndSavesEvent() {
        // given
        String userId = "auth0|123";
        String email = "user@example.com";
        String role = "ADMIN";
        String ipAddress = "203.0.113.5";
        String userAgent = "JUnit-Agent";

        // when
        loginAuditService.recordLoginEvent(userId, email, role, ipAddress, userAgent);

        // then
        ArgumentCaptor<LoginAuditEvent> captor = ArgumentCaptor.forClass(LoginAuditEvent.class);
        verify(repository).save(captor.capture());

        LoginAuditEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getRole()).isEqualTo(role);
        assertThat(saved.getIpAddress()).isEqualTo(ipAddress);
        assertThat(saved.getUserAgent()).isEqualTo(userAgent);
        // timestamp is generated inside the service, just check it's not null
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void getAllLoginEvents_delegatesToRepository() {
        // given
        List<LoginAuditEvent> events = List.of(mock(LoginAuditEvent.class));
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);

        // when
        List<LoginAuditEvent> result = loginAuditService.getAllLoginEvents();

        // then
        verify(repository).findAllByOrderByTimestampDesc();
        assertThat(result).isEqualTo(events);
    }

    @Test
    void getLoginEventsByUser_delegatesToRepository() {
        // given
        String userId = "auth0|456";
        List<LoginAuditEvent> events = List.of(mock(LoginAuditEvent.class));
        when(repository.findByUserIdOrderByTimestampDesc(userId)).thenReturn(events);

        // when
        List<LoginAuditEvent> result = loginAuditService.getLoginEventsByUser(userId);

        // then
        verify(repository).findByUserIdOrderByTimestampDesc(userId);
        assertThat(result).isEqualTo(events);
    }

    @Test
    void getLoginEventsByRole_delegatesToRepository() {
        // given
        String role = "BROKER";
        List<LoginAuditEvent> events = List.of(mock(LoginAuditEvent.class));
        when(repository.findByRoleOrderByTimestampDesc(role)).thenReturn(events);

        // when
        List<LoginAuditEvent> result = loginAuditService.getLoginEventsByRole(role);

        // then
        verify(repository).findByRoleOrderByTimestampDesc(role);
        assertThat(result).isEqualTo(events);
    }
}
