package com.example.courtierprobackend.notifications.businesslayer;

import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.notifications.datalayer.Notification;
import com.example.courtierprobackend.notifications.datalayer.NotificationRepository;
import com.example.courtierprobackend.notifications.presentationlayer.NotificationMapper;
import com.example.courtierprobackend.notifications.presentationlayer.NotificationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public void createNotification(String recipientId, String title, String message, String relatedTransactionId) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .isRead(false)
                .relatedTransactionId(relatedTransactionId)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationResponseDTO> getUserNotifications(String userId) {
        List<Notification> notifications = notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId);
        return notificationMapper.toResponseList(notifications);
    }

    @Override
    public NotificationResponseDTO markAsRead(String publicId) {
        Notification notification = notificationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("Notification not found for id: " + publicId));

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponseDTO(saved);
    }
}
