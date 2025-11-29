package com.example.courtierprobackend.transactions.datalayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoteRequestDTO {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotBlank(message = "actorId is required")
    private String actorId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    private Boolean visibleToClient = false;
}
