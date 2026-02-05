package com.example.courtierprobackend.documents.datalayer;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentFlowEnum;
import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage;
import com.example.courtierprobackend.transactions.datalayer.enums.SellerStage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Document {
    public boolean isVisibleToClient() {
        return visibleToClient;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private UUID documentId; // Public ID

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

    @Enumerated(EnumType.STRING)
    private StageEnum stage;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentFlowEnum flow = DocumentFlowEnum.REQUEST;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentVersion> versions = new ArrayList<>();

    private String brokerNotes;

    private LocalDateTime lastUpdatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime dueDate;

    private boolean visibleToClient;

    @Builder.Default
    private boolean requiresSignature = false;

    public boolean getVisibleToClient() {
        return visibleToClient;
    }

    // Soft delete fields
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
