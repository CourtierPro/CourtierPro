package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentFlowEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MissingAutoDraftItemDTO {
    private String itemKey;
    private String label;
    private DocumentTypeEnum docType;
    private DocumentFlowEnum flow;
    private boolean requiresSignature;
}
