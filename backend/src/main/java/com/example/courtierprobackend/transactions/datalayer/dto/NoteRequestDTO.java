package com.example.courtierprobackend.transactions.datalayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Data
public class NoteRequestDTO {

    @NotNull(message = "transactionId is required")
    private UUID transactionId;

    @NotNull(message = "actorId is required")
    private UUID actorId;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    private Boolean visibleToClient = false;
}
