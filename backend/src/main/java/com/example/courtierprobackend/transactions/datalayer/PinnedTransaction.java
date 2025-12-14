package com.example.courtierprobackend.transactions.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a broker's pinned transaction for quick access.
 * Pins persist across sessions and are broker-specific.
 */
@Entity
@Table(name = "pinned_transactions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"broker_id", "transaction_id"})
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PinnedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "broker_id", nullable = false)
    private UUID brokerId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;
}
