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

        @Mock
        private com.example.courtierprobackend.notifications.datalayer.BroadcastAuditRepository broadcastAuditRepository;

        @Mock
        private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        private NotificationService notificationService;

        @BeforeEach
        void setup() {
                notificationService = new NotificationServiceImpl(notificationRepository, notificationMapper,
                                userAccountRepository, broadcastAuditRepository, objectMapper);
        }

        @Test
        void createNotification_shouldSaveNotification() {
                // Arrange
                String recipientId = UUID.randomUUID().toString(); // Internal UUID
                String title = "Test Title";
                String message = "Test Message";
                String relatedTransactionId = UUID.randomUUID().toString();

                // Act
                notificationService.createNotification(recipientId, title, message, relatedTransactionId,
                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.GENERAL);

                // Assert
                org.mockito.ArgumentCaptor<Notification> notificationCaptor = org.mockito.ArgumentCaptor
                                .forClass(Notification.class);
                verify(notificationRepository).save(notificationCaptor.capture());
                assertThat(notificationCaptor.getValue().getType())
                                .isEqualTo(com.example.courtierprobackend.notifications.datalayer.enums.NotificationType.GENERAL);
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

                when(userAccountRepository.findByAuth0UserId(auth0UserId))
                                .thenReturn(java.util.Optional.of(userAccount));
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
                                .assertThrows(com.example.courtierprobackend.common.exceptions.NotFoundException.class,
                                                () -> {
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
                                .assertThrows(com.example.courtierprobackend.common.exceptions.NotFoundException.class,
                                                () -> {
                                                        notificationService.getUserNotifications(auth0UserId);
                                                });
        }

        @Test
        void sendBroadcast_shouldNotifyActiveUsersAndLogAudit() {
                // Arrange
                com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO request = new com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO(
                                "Title", "Message");
                String adminId = "auth0|admin";

                com.example.courtierprobackend.user.dataaccesslayer.UserAccount user1 = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
                user1.setId(UUID.randomUUID());
                com.example.courtierprobackend.user.dataaccesslayer.UserAccount user2 = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
                user2.setId(UUID.randomUUID());

                List<com.example.courtierprobackend.user.dataaccesslayer.UserAccount> activeUsers = List.of(user1,
                                user2);

                when(userAccountRepository.findByActiveTrue()).thenReturn(activeUsers);

                // Act
                notificationService.sendBroadcast(request, adminId);

                // Assert
                // Should save all notifications in one batch
                @SuppressWarnings("unchecked")
                org.mockito.ArgumentCaptor<List<Notification>> notificationsCaptor = org.mockito.ArgumentCaptor
                                .forClass(List.class);
                verify(notificationRepository).saveAll(notificationsCaptor.capture());

                List<Notification> capturedNotifications = notificationsCaptor.getValue();
                assertThat(capturedNotifications).hasSize(2);
                assertThat(capturedNotifications).allMatch(n -> n
                                .getType() == com.example.courtierprobackend.notifications.datalayer.enums.NotificationType.BROADCAST);
                assertThat(capturedNotifications).allMatch(n -> n
                                .getCategory() == com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.BROADCAST);

                // Should save 1 audit log
                org.mockito.ArgumentCaptor<com.example.courtierprobackend.notifications.datalayer.BroadcastAudit> auditCaptor = org.mockito.ArgumentCaptor
                                .forClass(com.example.courtierprobackend.notifications.datalayer.BroadcastAudit.class);
                verify(broadcastAuditRepository).save(auditCaptor.capture());

                com.example.courtierprobackend.notifications.datalayer.BroadcastAudit capturedAudit = auditCaptor
                                .getValue();
                assertThat(capturedAudit.getAdminId()).isEqualTo(adminId);
                assertThat(capturedAudit.getTitle()).isEqualTo("Title");
                assertThat(capturedAudit.getMessage()).isEqualTo("Message");
                assertThat(capturedAudit.getRecipientCount()).isEqualTo(2);
        }

        @Test
        void sendBroadcast_shouldLogAuditEvenIfNoUsers() {
                // Arrange
                com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO request = new com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO(
                                "Title", "Message");
                String adminId = "auth0|admin";

                when(userAccountRepository.findByActiveTrue()).thenReturn(List.of());

                // Act
                notificationService.sendBroadcast(request, adminId);

                // Assert
                // Should NOT save any notifications (saveAll not called)
                verify(notificationRepository, org.mockito.Mockito.never()).saveAll(any());

                // Should save 1 audit log with count 0
                org.mockito.ArgumentCaptor<com.example.courtierprobackend.notifications.datalayer.BroadcastAudit> auditCaptor = org.mockito.ArgumentCaptor
                                .forClass(com.example.courtierprobackend.notifications.datalayer.BroadcastAudit.class);
                verify(broadcastAuditRepository).save(auditCaptor.capture());

                assertThat(auditCaptor.getValue().getRecipientCount()).isZero();
        }
}
