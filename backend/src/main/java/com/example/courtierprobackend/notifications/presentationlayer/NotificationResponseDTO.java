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
    private com.example.courtierprobackend.notifications.datalayer.enums.NotificationType type;

    private boolean read;

    private String relatedTransactionId;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
