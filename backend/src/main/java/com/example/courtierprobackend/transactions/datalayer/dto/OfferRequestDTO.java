package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferRequestDTO {

    @NotBlank(message = "Buyer name is required")
    private String buyerName;

    @NotNull(message = "Offer amount is required")
    @DecimalMin(value = "0.01", message = "Offer amount must be greater than zero")
    private BigDecimal offerAmount;

    private ReceivedOfferStatus status;

    private String notes;
}
