package com.example.courtierprobackend.documents.presentationlayer.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StageChecklistResponseDTO {
    private String stage;
    private List<StageChecklistItemDTO> items;
}
