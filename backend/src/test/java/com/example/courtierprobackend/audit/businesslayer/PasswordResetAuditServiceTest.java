package com.example.courtierprobackend.audit.businesslayer;

import com.example.courtierprobackend.audit.dataaccesslayer.PasswordResetEvent;
import com.example.courtierprobackend.audit.dataaccesslayer.PasswordResetEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetAuditServiceTest {

    @Mock
    private PasswordResetEventRepository passwordResetEventRepository;

    @InjectMocks
    private PasswordResetAuditService passwordResetAuditService;

    private static final String TEST_USER_ID = "auth0|test123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_IP = "192.168.1.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(passwordResetEventRepository);
    }

    @Test
    void recordPasswordResetRequest_shouldSaveEventWithCorrectData() {
        // Arrange
        ArgumentCaptor<PasswordResetEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetEvent.class);

        // Act
        passwordResetAuditService.recordPasswordResetRequest(
                TEST_USER_ID,
                TEST_EMAIL,
                TEST_IP,
                TEST_USER_AGENT
        );

        // Assert
        verify(passwordResetEventRepository, times(1)).save(eventCaptor.capture());
        PasswordResetEvent savedEvent = eventCaptor.getValue();

        assertThat(savedEvent.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedEvent.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(savedEvent.getEventType()).isEqualTo(PasswordResetEvent.ResetEventType.REQUESTED);
        assertThat(savedEvent.getIpAddress()).isEqualTo(TEST_IP);
        assertThat(savedEvent.getUserAgent()).isEqualTo(TEST_USER_AGENT);
        assertThat(savedEvent.getTimestamp()).isNotNull();
        assertThat(savedEvent.getTimestamp()).isBefore(Instant.now().plusSeconds(1));
    }

    @Test
    void recordPasswordResetCompletion_shouldSaveEventWithCorrectData() {
        // Arrange
        ArgumentCaptor<PasswordResetEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetEvent.class);

        // Act
        passwordResetAuditService.recordPasswordResetCompletion(
                TEST_USER_ID,
                TEST_EMAIL,
                TEST_IP,
                TEST_USER_AGENT
        );

        // Assert
        verify(passwordResetEventRepository, times(1)).save(eventCaptor.capture());
        PasswordResetEvent savedEvent = eventCaptor.getValue();

        assertThat(savedEvent.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedEvent.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(savedEvent.getEventType()).isEqualTo(PasswordResetEvent.ResetEventType.COMPLETED);
        assertThat(savedEvent.getIpAddress()).isEqualTo(TEST_IP);
        assertThat(savedEvent.getUserAgent()).isEqualTo(TEST_USER_AGENT);
        assertThat(savedEvent.getTimestamp()).isNotNull();
    }

    @Test
    void recordPasswordResetRequest_withNullIpAndUserAgent_shouldSaveEvent() {
        // Arrange
        ArgumentCaptor<PasswordResetEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetEvent.class);

        // Act
        passwordResetAuditService.recordPasswordResetRequest(
                TEST_USER_ID,
                TEST_EMAIL,
                null,
                null
        );

        // Assert
        verify(passwordResetEventRepository, times(1)).save(eventCaptor.capture());
        PasswordResetEvent savedEvent = eventCaptor.getValue();

        assertThat(savedEvent.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedEvent.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(savedEvent.getIpAddress()).isNull();
        assertThat(savedEvent.getUserAgent()).isNull();
    }

    @Test
    void getAllPasswordResetEvents_shouldReturnAllEvents() {
        // Arrange
        PasswordResetEvent event1 = PasswordResetEvent.builder()
                .userId(TEST_USER_ID)
                .email(TEST_EMAIL)
                .eventType(PasswordResetEvent.ResetEventType.REQUESTED)
                .timestamp(Instant.now())
                .build();

        PasswordResetEvent event2 = PasswordResetEvent.builder()
                .userId(TEST_USER_ID)
                .email(TEST_EMAIL)
                .eventType(PasswordResetEvent.ResetEventType.COMPLETED)
                .timestamp(Instant.now())
                .build();

        List<PasswordResetEvent> mockEvents = List.of(event1, event2);
        when(passwordResetEventRepository.findAllByOrderByTimestampDesc()).thenReturn(mockEvents);

        // Act
        List<PasswordResetEvent> result = passwordResetAuditService.getAllPasswordResetEvents();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEventType()).isEqualTo(PasswordResetEvent.ResetEventType.REQUESTED);
        assertThat(result.get(1).getEventType()).isEqualTo(PasswordResetEvent.ResetEventType.COMPLETED);
        verify(passwordResetEventRepository, times(1)).findAllByOrderByTimestampDesc();
    }

    @Test
    void getPasswordResetEventsForUser_shouldReturnUserSpecificEvents() {
        // Arrange
        PasswordResetEvent event = PasswordResetEvent.builder()
                .userId(TEST_USER_ID)
                .email(TEST_EMAIL)
                .eventType(PasswordResetEvent.ResetEventType.REQUESTED)
                .timestamp(Instant.now())
                .build();

        List<PasswordResetEvent> mockEvents = List.of(event);
        when(passwordResetEventRepository.findByUserIdOrderByTimestampDesc(TEST_USER_ID))
                .thenReturn(mockEvents);

        // Act
        List<PasswordResetEvent> result = passwordResetAuditService.getPasswordResetEventsForUser(TEST_USER_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(TEST_USER_ID);
        verify(passwordResetEventRepository, times(1))
                .findByUserIdOrderByTimestampDesc(TEST_USER_ID);
    }

    @Test
    void getAllPasswordResetEvents_whenNoEvents_shouldReturnEmptyList() {
        // Arrange
        when(passwordResetEventRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of());

        // Act
        List<PasswordResetEvent> result = passwordResetAuditService.getAllPasswordResetEvents();

        // Assert
        assertThat(result).isEmpty();
        verify(passwordResetEventRepository, times(1)).findAllByOrderByTimestampDesc();
    }
}
