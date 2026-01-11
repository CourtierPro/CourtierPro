package com.example.courtierprobackend.notifications.businesslayer;

import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.notifications.datalayer.Notification;
import com.example.courtierprobackend.notifications.datalayer.NotificationRepository;
import com.example.courtierprobackend.notifications.presentationlayer.NotificationMapper;
import com.example.courtierprobackend.notifications.presentationlayer.NotificationResponseDTO;
import lombok.RequiredArgsConstructor;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.notifications.datalayer.BroadcastAudit;
import com.example.courtierprobackend.notifications.datalayer.BroadcastAuditRepository;
import com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

        private final NotificationRepository notificationRepository;
        private final NotificationMapper notificationMapper;
        private final com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;
        private final BroadcastAuditRepository broadcastAuditRepository;
        private final ObjectMapper objectMapper;

        @Override
        @org.springframework.transaction.annotation.Transactional
        public void createNotification(String recipientId, String title, String message, String relatedTransactionId,
                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory category) {
                Notification notification = Notification.builder()
                                .recipientId(recipientId) // Expecting internal UUID here
                                .title(title)
                                .message(message)
                                .type(com.example.courtierprobackend.notifications.datalayer.enums.NotificationType.GENERAL)
                                .category(category)
                                .isRead(false)
                                .relatedTransactionId(relatedTransactionId)
                                .build();

                notificationRepository.save(notification);
        }

        @Override
        @org.springframework.transaction.annotation.Transactional
        public void createNotification(String recipientId, String titleKey, String messageKey,
                        Map<String, Object> params, String relatedTransactionId,
                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory category) {
                String paramsJson = null;
                if (params != null && !params.isEmpty()) {
                        try {
                                paramsJson = objectMapper.writeValueAsString(params);
                        } catch (JsonProcessingException e) {
                                // Log and continue without params
                        }
                }

                Notification notification = Notification.builder()
                                .recipientId(recipientId)
                                .title(titleKey) // Use key as fallback title
                                .message(messageKey) // Use key as fallback message
                                .titleKey(titleKey)
                                .messageKey(messageKey)
                                .params(paramsJson)
                                .type(com.example.courtierprobackend.notifications.datalayer.enums.NotificationType.GENERAL)
                                .category(category)
                                .isRead(false)
                                .relatedTransactionId(relatedTransactionId)
                                .build();

                notificationRepository.save(notification);
        }

        @Override
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public List<NotificationResponseDTO> getUserNotifications(String auth0UserId) {
                // ID Mapping: Auth0 ID -> Internal UUID
                // The controller gives us auth0UserId (subject). We must resolve to internal
                // UUID to query data.
                var user = userAccountRepository.findByAuth0UserId(auth0UserId)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                // Use internal UUID to query notifications
                List<Notification> notifications = notificationRepository
                                .findAllByRecipientIdOrderByCreatedAtDesc(user.getId().toString());
                return notificationMapper.toResponseList(notifications);
        }

        @Override
        @org.springframework.transaction.annotation.Transactional
        public NotificationResponseDTO markAsRead(String publicId) {
                Notification notification = notificationRepository.findByPublicId(publicId)
                                .orElseThrow(() -> new NotFoundException("Notification not found for id: " + publicId));

                notification.setRead(true);
                Notification saved = notificationRepository.save(notification);
                return notificationMapper.toResponseDTO(saved);
        }

        @Override
        @org.springframework.transaction.annotation.Transactional
        /**
         * Sends a broadcast notification to all active users in the system and records
         * an audit entry.
         * <p>
         * For each active {@link UserAccount}, this method creates and persists a new
         * {@link Notification} of type {@code BROADCAST} using the title and message
         * provided
         * in the {@link BroadcastRequestDTO}. In addition, a {@link BroadcastAudit}
         * record is
         * created to capture details about the broadcast operation for auditing
         * purposes.
         *
         * @param request the broadcast request containing the title and message to be
         *                sent
         *                to all active users.
         * @param adminId the identifier of the administrator initiating the broadcast;
         *                this value
         *                is stored in {@link BroadcastAudit} and used for audit logging
         *                of who
         *                performed the broadcast.
         * @implNote This operation may create a large number of {@link Notification}
         *           entities,
         *           one for each active user, and will always create a single
         *           {@link BroadcastAudit}
         *           entry to record the broadcast metadata (including {@code adminId}
         *           and
         *           recipient count).
         */
        public void sendBroadcast(BroadcastRequestDTO request, String adminId) {
                List<UserAccount> activeUsers = userAccountRepository.findByActiveTrue();

                List<Notification> notifications = activeUsers.stream()
                                .map(user -> Notification.builder()
                                                .publicId(UUID.randomUUID().toString())
                                                .recipientId(user.getId().toString())
                                                .title(request.getTitle())
                                                .message(request.getMessage())
                                                .type(com.example.courtierprobackend.notifications.datalayer.enums.NotificationType.BROADCAST)
                                                .category(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.BROADCAST)
                                                .isRead(false)
                                                .createdAt(LocalDateTime.now())
                                                .relatedTransactionId(null)
                                                .build())
                                .toList();

                if (!notifications.isEmpty()) {
                        notificationRepository.saveAll(notifications);
                }

                BroadcastAudit audit = BroadcastAudit.builder()
                                .id(UUID.randomUUID())
                                .adminId(adminId)
                                .title(request.getTitle())
                                .message(request.getMessage())
                                .sentAt(LocalDateTime.now())
                                .recipientCount(activeUsers.size())
                                .build();

                broadcastAuditRepository.save(audit);
        }
}
