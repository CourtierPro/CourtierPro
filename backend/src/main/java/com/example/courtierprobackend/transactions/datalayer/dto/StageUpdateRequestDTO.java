package com.example.courtierprobackend.transactions.datalayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StageUpdateRequestDTO {

    @NotBlank(message = "stage is required")
    private String stage;

    private String note;

    // reason is validated in service layer - only required for rollbacks with min
    // 10 chars
    private String reason;
}
