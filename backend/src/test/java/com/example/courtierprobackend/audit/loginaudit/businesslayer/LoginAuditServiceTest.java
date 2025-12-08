package com.example.courtierprobackend.audit.loginaudit.businesslayer;

import com.example.courtierprobackend.audit.loginaudit.dataaccesslayer.LoginAuditEvent;
import com.example.courtierprobackend.audit.loginaudit.dataaccesslayer.LoginAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginAuditService.
 */
@ExtendWith(MockitoExtension.class)
class LoginAuditServiceTest {

    @Mock
    private LoginAuditEventRepository repository;

    private LoginAuditService service;

    @BeforeEach
    void setUp() {
        service = new LoginAuditService(repository);
    }

    @Test
    void recordLoginEvent_SavesEventWithAllFields() {
        // Act
        service.recordLoginEvent("user-1", "user@test.com", "BROKER", "127.0.0.1", "Mozilla/5.0");

        // Assert
        ArgumentCaptor<LoginAuditEvent> captor = ArgumentCaptor.forClass(LoginAuditEvent.class);
        verify(repository).save(captor.capture());
        
        LoginAuditEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo("user-1");
        assertThat(event.getEmail()).isEqualTo("user@test.com");
        assertThat(event.getRole()).isEqualTo("BROKER");
        assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void getAllLoginEvents_ReturnsAllEvents() {
        // Arrange
        List<LoginAuditEvent> events = List.of(
                LoginAuditEvent.builder().userId("u1").build(),
                LoginAuditEvent.builder().userId("u2").build()
        );
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);

        // Act
        List<LoginAuditEvent> result = service.getAllLoginEvents();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void getLoginEventsByUser_ReturnsUserEvents() {
        // Arrange
        List<LoginAuditEvent> events = List.of(
                LoginAuditEvent.builder().userId("user-1").build()
        );
        when(repository.findByUserIdOrderByTimestampDesc("user-1")).thenReturn(events);

        // Act
        List<LoginAuditEvent> result = service.getLoginEventsByUser("user-1");

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void getLoginEventsByRole_ReturnsRoleEvents() {
        // Arrange
        List<LoginAuditEvent> events = List.of(
                LoginAuditEvent.builder().role("ADMIN").build()
        );
        when(repository.findByRoleOrderByTimestampDesc("ADMIN")).thenReturn(events);

        // Act
        List<LoginAuditEvent> result = service.getLoginEventsByRole("ADMIN");

        // Assert
        assertThat(result).hasSize(1);
    }
}
