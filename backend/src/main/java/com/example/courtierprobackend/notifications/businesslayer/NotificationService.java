package com.example.courtierprobackend.notifications.businesslayer;

import com.example.courtierprobackend.notifications.presentationlayer.NotificationResponseDTO;
import com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO;

import java.util.List;

public interface NotificationService {

    void createNotification(String recipientId, String title, String message, String relatedTransactionId);

    List<NotificationResponseDTO> getUserNotifications(String auth0UserId);

    NotificationResponseDTO markAsRead(String publicId);

    void sendBroadcast(BroadcastRequestDTO request, String adminId);
}
