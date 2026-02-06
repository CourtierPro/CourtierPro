package com.example.courtierprobackend.documents.datalayer;

import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_stage_checklist_state",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tx_stage_item",
                columnNames = {"transaction_id", "stage", "item_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStageChecklistState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private StageEnum stage;

    @Column(name = "item_key", nullable = false, length = 120)
    private String itemKey;

    @Column(name = "manual_checked")
    private Boolean manualChecked;

    @Column(name = "manual_checked_by")
    private UUID manualCheckedBy;

    @Column(name = "manual_checked_at")
    private LocalDateTime manualCheckedAt;

    @Builder.Default
    @Column(name = "auto_checked", nullable = false)
    private boolean autoChecked = false;

    @Column(name = "auto_checked_at")
    private LocalDateTime autoCheckedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
