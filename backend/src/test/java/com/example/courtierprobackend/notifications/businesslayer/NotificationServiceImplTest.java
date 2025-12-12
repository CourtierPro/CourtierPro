package com.example.courtierprobackend.notifications.businesslayer;

import com.example.courtierprobackend.notifications.datalayer.Notification;
import com.example.courtierprobackend.notifications.datalayer.NotificationRepository;
import com.example.courtierprobackend.notifications.presentationlayer.NotificationMapper;
import com.example.courtierprobackend.notifications.presentationlayer.NotificationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationService notificationService;

    @BeforeEach
    void setup() {
        notificationService = new NotificationServiceImpl(notificationRepository, notificationMapper);
    }

    @Test
    void createNotification_shouldSaveNotification() {
        // Arrange
        String recipientId = "auth0|123";
        String title = "Test Title";
        String message = "Test Message";
        String relatedTransactionId = UUID.randomUUID().toString();

        // Act
        notificationService.createNotification(recipientId, title, message, relatedTransactionId);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getUserNotifications_shouldReturnMappedDTOs() {
        // Arrange
        String userId = "auth0|123";
        Notification notification = new Notification();
        notification.setRecipientId(userId);

        List<Notification> notifications = List.of(notification);
        List<NotificationResponseDTO> dtos = List.of(new NotificationResponseDTO());

        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId)).thenReturn(notifications);
        when(notificationMapper.toResponseList(notifications)).thenReturn(dtos);

        // Act
        List<NotificationResponseDTO> result = notificationService.getUserNotifications(userId);

        // Assert
        assertThat(result).isSameAs(dtos);
        verify(notificationRepository).findAllByRecipientIdOrderByCreatedAtDesc(userId);
        verify(notificationMapper).toResponseList(notifications);
    }
}
