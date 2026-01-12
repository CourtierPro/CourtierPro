package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.ConditionType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConditionResponseDTO {

    private UUID conditionId;
    private UUID transactionId;
    private ConditionType type;
    private String customTitle;
    private String description;
    private LocalDate deadlineDate;
    private ConditionStatus status;
    private LocalDateTime satisfiedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
