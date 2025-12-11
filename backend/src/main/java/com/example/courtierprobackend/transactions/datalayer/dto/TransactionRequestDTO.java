package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TransactionRequestDTO {

    @NotNull(message = "clientId is required")
    private UUID clientId;

    // NOTE: brokerId is NOT part of request body in your controller — set by header
    private UUID brokerId;

    @NotNull(message = "side is required")
    private TransactionSide side;

    // Optional for now — per your DDD, address may be filled later
    private PropertyAddress propertyAddress;

    @NotBlank(message = "initialStage is required")
    private String initialStage;
}
