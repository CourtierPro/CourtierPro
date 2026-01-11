package com.example.courtierprobackend.notifications.businesslayer;

import com.example.courtierprobackend.notifications.presentationlayer.NotificationResponseDTO;
import com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    void createNotification(String recipientId, String title, String message, String relatedTransactionId,
            com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory category);

    /**
     * Creates a notification with translation keys for i18n support.
     * The frontend will use titleKey and messageKey to display translated content.
     */
    void createNotification(String recipientId, String titleKey, String messageKey, 
            Map<String, Object> params, String relatedTransactionId,
            com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory category);

    List<NotificationResponseDTO> getUserNotifications(String auth0UserId);

    NotificationResponseDTO markAsRead(String publicId);

    void sendBroadcast(BroadcastRequestDTO request, String adminId);
}
