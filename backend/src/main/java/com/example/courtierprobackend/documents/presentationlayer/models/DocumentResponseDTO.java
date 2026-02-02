package com.example.courtierprobackend.documents.presentationlayer.models;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDTO {
    private UUID documentId;
    private TransactionRef transactionRef;
    private DocumentTypeEnum docType;
    private String customTitle;
    private DocumentStatusEnum status;
    private DocumentPartyEnum expectedFrom;
    private List<DocumentVersionResponseDTO> versions;
    private String brokerNotes;
    private LocalDateTime lastUpdatedAt;
    private boolean visibleToClient;
    private StageEnum stage;
}
