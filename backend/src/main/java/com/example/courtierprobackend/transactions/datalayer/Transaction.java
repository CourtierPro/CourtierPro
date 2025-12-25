package com.example.courtierprobackend.transactions.datalayer;

// TimelineEntry import removed: timeline is now handled via audit/timeline module
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.List;
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

    @Enumerated(EnumType.STRING)
    private TransactionSide side;

    @Enumerated(EnumType.STRING)
    private BuyerStage buyerStage;

    @Enumerated(EnumType.STRING)
    private SellerStage sellerStage;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @Version
    private Long version;

    // Timeline is now handled via audit/timeline module, not as a direct relation

    // Soft delete fields
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
