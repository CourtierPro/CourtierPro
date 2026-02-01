package com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    private String reason;

    // Offer-related fields
    private String buyerName;
    private BigDecimal offerAmount;
    private String offerStatus;
    private String previousOfferStatus;

    // Condition-related fields
    private String conditionType;
    private String conditionCustomTitle;
    private String conditionDescription;
    private LocalDate conditionDeadline;
    private String conditionPreviousStatus;
    private String conditionNewStatus;

    // Appointment-related fields
    private String appointmentTitle;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private java.time.LocalDateTime appointmentDate;
}
