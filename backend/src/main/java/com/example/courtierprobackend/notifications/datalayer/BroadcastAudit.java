package com.example.courtierprobackend.notifications.datalayer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "broadcast_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastAudit {

    @Id
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private String adminId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "recipient_count", nullable = false)
    private Integer recipientCount;
}
