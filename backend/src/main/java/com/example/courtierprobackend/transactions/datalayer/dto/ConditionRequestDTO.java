package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.ConditionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConditionRequestDTO {

    @NotNull(message = "Condition type is required")
    private ConditionType type;

    /**
     * Custom title for conditions of type OTHER.
     * Required when type is OTHER.
     */
    private String customTitle;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Deadline date is required")
    private LocalDate deadlineDate;

    private ConditionStatus status;

    private String notes;
}
