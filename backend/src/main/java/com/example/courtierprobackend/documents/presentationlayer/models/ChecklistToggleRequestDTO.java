package com.example.courtierprobackend.documents.presentationlayer.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChecklistToggleRequestDTO {

    @NotBlank(message = "stage is required")
    private String stage;

    @NotNull(message = "checked is required")
    private Boolean checked;
}
