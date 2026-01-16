package com.example.courtierprobackend.documents.presentationlayer.models;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequestRequestDTO {
    private DocumentTypeEnum docType;
    private String customTitle;
    private DocumentPartyEnum expectedFrom;
    private Boolean visibleToClient;
    private String brokerNotes;
    private StageEnum stage;
    
    /**
     * List of condition IDs to link to this document request.
     */
    private List<UUID> conditionIds;
}
