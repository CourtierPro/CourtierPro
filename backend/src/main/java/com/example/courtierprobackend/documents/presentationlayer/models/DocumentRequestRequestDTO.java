package com.example.courtierprobackend.documents.presentationlayer.models;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
