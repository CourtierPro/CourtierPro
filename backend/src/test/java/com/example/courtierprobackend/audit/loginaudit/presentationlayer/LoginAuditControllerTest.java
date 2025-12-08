package com.example.courtierprobackend.audit.loginaudit.presentationlayer;

import com.example.courtierprobackend.audit.loginaudit.businesslayer.LoginAuditService;
import com.example.courtierprobackend.audit.loginaudit.dataaccesslayer.LoginAuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginAuditController.
 */
@ExtendWith(MockitoExtension.class)
class LoginAuditControllerTest {

    @Mock
    private LoginAuditService loginAuditService;

    private LoginAuditController controller;

    @BeforeEach
    void setUp() {
        controller = new LoginAuditController(loginAuditService);
    }

    @Test
    void getAllLoginEvents_ReturnsAllEvents() {
        // Arrange
        List<LoginAuditEvent> events = List.of(
                LoginAuditEvent.builder().id(UUID.randomUUID()).userId("u1").email("u1@test.com").timestamp(Instant.now()).build(),
                LoginAuditEvent.builder().id(UUID.randomUUID()).userId("u2").email("u2@test.com").timestamp(Instant.now()).build()
        );
        when(loginAuditService.getAllLoginEvents()).thenReturn(events);

        // Act
        List<LoginAuditEvent> result = controller.getAllLoginEvents();

        // Assert
        assertThat(result).hasSize(2);
        verify(loginAuditService).getAllLoginEvents();
    }

    @Test
    void getAllLoginEvents_WithNoEvents_ReturnsEmptyList() {
        // Arrange
        when(loginAuditService.getAllLoginEvents()).thenReturn(List.of());

        // Act
        List<LoginAuditEvent> result = controller.getAllLoginEvents();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getLoginEventsByUser_ReturnsUserEvents() {
        // Arrange
        List<LoginAuditEvent> events = List.of(
                LoginAuditEvent.builder().userId("user-1").email("user@test.com").build()
        );
        when(loginAuditService.getLoginEventsByUser("user-1")).thenReturn(events);

        // Act
        List<LoginAuditEvent> result = controller.getLoginEventsByUser("user-1");

        // Assert
        assertThat(result).hasSize(1);
        verify(loginAuditService).getLoginEventsByUser("user-1");
    }

    @Test
    void getLoginEventsByRole_ReturnsRoleEvents() {
        // Arrange
        List<LoginAuditEvent> events = List.of(
                LoginAuditEvent.builder().userId("u1").role("BROKER").build()
        );
        when(loginAuditService.getLoginEventsByRole("BROKER")).thenReturn(events);

        // Act
        List<LoginAuditEvent> result = controller.getLoginEventsByRole("BROKER");

        // Assert
        assertThat(result).hasSize(1);
        verify(loginAuditService).getLoginEventsByRole("BROKER");
    }
}