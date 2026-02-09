package com.example.courtierprobackend.transactions.datalayer;

// TimelineEntry import removed: timeline is now handled via audit/timeline module
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID transactionId;

    private UUID clientId;
    private UUID brokerId;

    @Embedded
    private PropertyAddress propertyAddress;

    // Centris Number for sell-side transactions (the property being sold)
    @Column(name = "centris_number", length = 50)
    private String centrisNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TransactionSide side;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BuyerStage buyerStage;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SellerStage sellerStage;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TransactionStatus status;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    // Internal notes for brokers
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Version
    private Long version;

    // Timeline is now handled via audit/timeline module, not as a direct relation

    // Archive fields - allows brokers to hide completed transactions from default views
    @Column(name = "archived", nullable = false)
    @Builder.Default
    private Boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    // Soft delete fields
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
