package com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class TransactionInfo {
    private String clientName;
    private String address;
    private String actorName;
    private String stage;

    private String previousStage;
    private String newStage;

    // Offer-related fields
    private String buyerName;
    private BigDecimal offerAmount;
    private String offerStatus;

    // Condition-related fields
    private String conditionType;
    private String conditionDescription;
    private String conditionDeadline;
    private String conditionPreviousStatus;
    private String conditionNewStatus;
}
