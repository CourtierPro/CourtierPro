package com.example.courtierprobackend.notifications.businesslayer;

import com.example.courtierprobackend.notifications.presentationlayer.NotificationResponseDTO;

import java.util.List;

public interface NotificationService {

    void createNotification(String recipientId, String title, String message, String relatedTransactionId);

    List<NotificationResponseDTO> getUserNotifications(String userId);

    NotificationResponseDTO markAsRead(String publicId);
}
