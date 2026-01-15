package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ClientOfferDecision;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for submitting a client's decision on a received offer.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientOfferDecisionDTO {

    @NotNull(message = "Decision is required")
    private ClientOfferDecision decision;

    private String notes;
}
