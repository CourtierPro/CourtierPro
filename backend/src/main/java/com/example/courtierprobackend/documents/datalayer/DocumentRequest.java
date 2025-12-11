package com.example.courtierprobackend.documents.datalayer;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage;
import com.example.courtierprobackend.transactions.datalayer.enums.SellerStage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_requests")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private UUID requestId; // Public ID

    @Embedded
    private TransactionRef transactionRef;

    @Enumerated(EnumType.STRING)
    private DocumentTypeEnum docType;

    private String customTitle; // For "OTHER" or specific naming

    @Enumerated(EnumType.STRING)
    private DocumentStatusEnum status;

    @Enumerated(EnumType.STRING)
    private DocumentPartyEnum expectedFrom;

    @Enumerated(EnumType.STRING)
    private BuyerStage relatedBuyerStage;

    @Enumerated(EnumType.STRING)
    private SellerStage relatedSellerStage;

    @OneToMany(mappedBy = "documentRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubmittedDocument> submittedDocuments = new ArrayList<>();

    private String brokerNotes;

    private LocalDateTime lastUpdatedAt;

    private boolean visibleToClient;
}
