package com.example.courtierprobackend.transactions.datalayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TerminateRequestDTO {

    @NotBlank(message = "Reason is required for termination")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
}
