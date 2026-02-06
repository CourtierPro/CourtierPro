package com.example.courtierprobackend.documents.presentationlayer.models;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentFlowEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StageChecklistItemDTO {
    private String itemKey;
    private String label;
    private DocumentTypeEnum docType;
    private DocumentFlowEnum flow;
    private boolean requiresSignature;

    private boolean checked;
    private String source; // AUTO | MANUAL | NONE

    private UUID documentId;
    private DocumentStatusEnum documentStatus;
}
