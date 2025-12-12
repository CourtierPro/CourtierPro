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

    @Mock
    private com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setup() {
        notificationService = new NotificationServiceImpl(notificationRepository, notificationMapper,
                userAccountRepository);
    }

    @Test
    void createNotification_shouldSaveNotification() {
        // Arrange
        String recipientId = UUID.randomUUID().toString(); // Internal UUID
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
        String auth0UserId = "auth0|123";
        UUID internalId = UUID.randomUUID();

        com.example.courtierprobackend.user.dataaccesslayer.UserAccount userAccount = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        userAccount.setId(internalId);

        Notification notification = new Notification();
        notification.setRecipientId(internalId.toString());

        List<Notification> notifications = List.of(notification);
        List<NotificationResponseDTO> dtos = List.of(new NotificationResponseDTO());

        when(userAccountRepository.findByAuth0UserId(auth0UserId)).thenReturn(java.util.Optional.of(userAccount));
        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(internalId.toString()))
                .thenReturn(notifications);
        when(notificationMapper.toResponseList(notifications)).thenReturn(dtos);

        // Act
        List<NotificationResponseDTO> result = notificationService.getUserNotifications(auth0UserId);

        // Assert
        assertThat(result).isSameAs(dtos);
        verify(userAccountRepository).findByAuth0UserId(auth0UserId);
        verify(notificationRepository).findAllByRecipientIdOrderByCreatedAtDesc(internalId.toString());
        verify(notificationMapper).toResponseList(notifications);
    }

    @Test
    void markAsRead_shouldUpdateNotification() {
        // Arrange
        String publicId = UUID.randomUUID().toString();
        Notification notification = new Notification();
        notification.setPublicId(publicId);
        notification.setRead(false);

        Notification saved = new Notification();
        saved.setPublicId(publicId);
        saved.setRead(true);

        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setPublicId(publicId);
        dto.setRead(true);

        when(notificationRepository.findByPublicId(publicId)).thenReturn(java.util.Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(saved);
        when(notificationMapper.toResponseDTO(saved)).thenReturn(dto);

        // Act
        NotificationResponseDTO result = notificationService.markAsRead(publicId);

        // Assert
        assertThat(result.isRead()).isTrue();
        verify(notificationRepository).findByPublicId(publicId);
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_shouldThrowIfNotFound() {
        // Arrange
        String publicId = UUID.randomUUID().toString();
        when(notificationRepository.findByPublicId(publicId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions
                .assertThrows(com.example.courtierprobackend.common.exceptions.NotFoundException.class, () -> {
                    notificationService.markAsRead(publicId);
                });
    }

    @Test
    void getUserNotifications_shouldThrowIfUserNotFound() {
        // Arrange
        String auth0UserId = "auth0|unknown";
        when(userAccountRepository.findByAuth0UserId(auth0UserId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions
                .assertThrows(com.example.courtierprobackend.common.exceptions.NotFoundException.class, () -> {
                    notificationService.getUserNotifications(auth0UserId);
                });
    }
}
