package com.example.courtierprobackend.notifications.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String publicId; // UUID as String

    @Column(nullable = false)
    private String recipientId; // UserAccount public ID (String)

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    // Translation keys for i18n support (optional - frontend uses these if present)
    private String titleKey;
    private String messageKey;
    
    @Column(columnDefinition = "TEXT")
    private String params; // JSON string with parameters for translation interpolation

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private com.example.courtierprobackend.notifications.datalayer.enums.NotificationType type; // GENERAL, BROADCAST

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory category;

    @Column(nullable = false)
    private boolean isRead;

    private String relatedTransactionId; // Optional link

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
