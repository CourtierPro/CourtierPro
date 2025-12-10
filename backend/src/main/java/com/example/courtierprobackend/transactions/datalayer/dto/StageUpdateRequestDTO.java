package com.example.courtierprobackend.transactions.datalayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StageUpdateRequestDTO {

    @NotBlank(message = "stage is required")
    private String stage;

    private String note;
}
