package com.example.courtierprobackend.transactions.datalayer.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MissingAutoDraftsResponseDTO {
    private String stage;
    private List<MissingAutoDraftItemDTO> missingItems;
}
