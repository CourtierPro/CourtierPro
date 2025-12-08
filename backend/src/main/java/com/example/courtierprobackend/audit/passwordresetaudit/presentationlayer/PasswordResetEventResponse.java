package com.example.courtierprobackend.audit.passwordresetaudit.presentationlayer;

import com.example.courtierprobackend.audit.passwordresetaudit.dataaccesslayer.PasswordResetEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for password reset audit events
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordResetEventResponse {

    UUID id;
    String userId;
    String email;
    String eventType; // "REQUESTED" or "COMPLETED"
    Instant timestamp;
    String ipAddress;
    String userAgent;

    public static PasswordResetEventResponse from(PasswordResetEvent event) {
        return PasswordResetEventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .email(event.getEmail())
                .eventType(event.getEventType().name())
                .timestamp(event.getTimestamp())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .build();
    }
}
