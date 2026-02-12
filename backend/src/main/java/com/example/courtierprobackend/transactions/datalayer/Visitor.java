package com.example.courtierprobackend.transactions.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a visitor (potential buyer) who visits a property
 * in a sell-side transaction via open houses or private showings.
 */
@Entity
@Table(name = "visitors")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Visitor {

    @Id
    @Column(name = "visitor_id", nullable = false, unique = true)
    private UUID visitorId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private String name;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (visitorId == null) {
            visitorId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
