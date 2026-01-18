package com.example.courtierprobackend.dashboard.presentationlayer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for conditions approaching their deadline.
 * Used in the broker dashboard to highlight risk indicators.
 */
@Data
@Builder
public class ApproachingConditionDTO {
    private UUID conditionId;
    private UUID transactionId;
    private String propertyAddress;
    private String clientName;
    private String conditionType;
    private String customTitle;
    private String description;
    private LocalDate deadlineDate;
    private int daysUntilDeadline;
    private String status;
    private String transactionSide;
}
