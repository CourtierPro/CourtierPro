package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;

    private String clientId;
    private String brokerId;

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

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimelineEntry> timeline;
}
