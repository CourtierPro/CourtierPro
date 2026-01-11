package com.example.courtierprobackend.notifications.presentationlayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private String publicId;
    private String title;
    private String message;
    
    // Translation keys for i18n support (optional)
    private String titleKey;
    private String messageKey;
    private String params; // JSON string with parameters
    
    private com.example.courtierprobackend.notifications.datalayer.enums.NotificationType type;
    private com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory category;

    private boolean read;

    private String relatedTransactionId;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
