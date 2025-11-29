package com.example.courtierprobackend.transactions.datalayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NoteRequestDTO {

    @NotBlank(message = "actorId is required")
    private String actorId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "visibleToClient is required")
    private Boolean visibleToClient;
}
