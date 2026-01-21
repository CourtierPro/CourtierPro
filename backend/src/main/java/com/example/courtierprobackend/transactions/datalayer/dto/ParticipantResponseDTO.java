package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole;
import lombok.Builder;
import lombok.Data;
import java.util.Set;
import com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission;

import java.util.UUID;

@Data
@Builder
public class ParticipantResponseDTO {
    private UUID id;
    private UUID transactionId;
    private String name;
    private ParticipantRole role;
    private String email;
    private String phoneNumber;
    private Set<ParticipantPermission> permissions;
}
