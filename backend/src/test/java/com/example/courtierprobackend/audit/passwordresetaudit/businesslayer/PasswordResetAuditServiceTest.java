package com.example.courtierprobackend.audit.passwordresetaudit.businesslayer;

import com.example.courtierprobackend.audit.passwordresetaudit.dataaccesslayer.PasswordResetEvent;
import com.example.courtierprobackend.audit.passwordresetaudit.dataaccesslayer.PasswordResetEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordResetAuditService.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetAuditServiceTest {

    @Mock
    private PasswordResetEventRepository repository;

    private PasswordResetAuditService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetAuditService(repository);
    }

    // ========== recordPasswordResetRequest Tests ==========

    @Test
    void recordPasswordResetRequest_WithValidData_SavesEvent() {
        // Act
        service.recordPasswordResetRequest("user-1", "user@test.com", "127.0.0.1", "Mozilla/5.0");

        // Assert
        ArgumentCaptor<PasswordResetEvent> captor = ArgumentCaptor.forClass(PasswordResetEvent.class);
        verify(repository).save(captor.capture());
        
        PasswordResetEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo("user-1");
        assertThat(event.getEmail()).isEqualTo("user@test.com");
        assertThat(event.getEventType()).isEqualTo(PasswordResetEvent.ResetEventType.REQUESTED);
    }

    @Test
    void recordPasswordResetRequest_WithNullUserId_ThrowsException() {
        assertThatThrownBy(() -> service.recordPasswordResetRequest(null, "email@test.com", "127.0.0.1", "agent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId cannot be null");
    }

    @Test
    void recordPasswordResetRequest_WithNullEmail_ThrowsException() {
        assertThatThrownBy(() -> service.recordPasswordResetRequest("user-1", null, "127.0.0.1", "agent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email cannot be null");
    }

    // ========== recordPasswordResetCompletion Tests ==========

    @Test
    void recordPasswordResetCompletion_WithValidData_SavesEvent() {
        // Act
        service.recordPasswordResetCompletion("user-1", "user@test.com", "127.0.0.1", "Mozilla/5.0");

        // Assert
        ArgumentCaptor<PasswordResetEvent> captor = ArgumentCaptor.forClass(PasswordResetEvent.class);
        verify(repository).save(captor.capture());
        
        PasswordResetEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo("user-1");
        assertThat(event.getEventType()).isEqualTo(PasswordResetEvent.ResetEventType.COMPLETED);
    }

    @Test
    void recordPasswordResetCompletion_WithNullUserId_ThrowsException() {
        assertThatThrownBy(() -> service.recordPasswordResetCompletion(null, "email@test.com", "127.0.0.1", "agent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId cannot be null");
    }

    // ========== Query Methods Tests ==========

    @Test
    void getAllPasswordResetEvents_ReturnsAllEvents() {
        // Arrange
        List<PasswordResetEvent> events = List.of(
                PasswordResetEvent.builder().userId("u1").build(),
                PasswordResetEvent.builder().userId("u2").build()
        );
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);

        // Act
        List<PasswordResetEvent> result = service.getAllPasswordResetEvents();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void getPasswordResetEventsForUser_ReturnsUserEvents() {
        // Arrange
        List<PasswordResetEvent> events = List.of(
                PasswordResetEvent.builder().userId("user-1").build()
        );
        when(repository.findByUserIdOrderByTimestampDesc("user-1")).thenReturn(events);

        // Act
        List<PasswordResetEvent> result = service.getPasswordResetEventsForUser("user-1");

        // Assert
        assertThat(result).hasSize(1);
    }
}
