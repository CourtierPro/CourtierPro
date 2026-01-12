package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.ConditionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a conditional clause in a transaction.
 * Conditions have deadlines and track their fulfillment status.
 */
@Entity
@Table(name = "conditions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condition_id", nullable = false, unique = true)
    private UUID conditionId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConditionType type;

    /**
     * Custom title for conditions of type OTHER.
     * For standard types, this can be null.
     */
    @Column(name = "custom_title")
    private String customTitle;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConditionStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "satisfied_at")
    private LocalDateTime satisfiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (conditionId == null) {
            conditionId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ConditionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
