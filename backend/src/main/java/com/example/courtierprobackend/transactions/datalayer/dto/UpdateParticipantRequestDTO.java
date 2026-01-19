package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;
import com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission;

@Data
public class UpdateParticipantRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Role is required")
    private ParticipantRole role;

    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;
    private String phoneNumber;
    private Set<ParticipantPermission> permissions;
}
