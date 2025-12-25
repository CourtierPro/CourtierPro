package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddParticipantRequestDTO {
    @NotNull(message = "Name is required")
    private String name;

    @NotNull(message = "Role is required")
    private ParticipantRole role;

    private String email;
    private String phoneNumber;
}
