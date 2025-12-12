package com.example.courtierprobackend.notifications.presentationlayer;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isRead")
    private boolean isRead;

    private String relatedTransactionId;
    private LocalDateTime createdAt;
}
