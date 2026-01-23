package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRequestDTO {

    @NotNull(message = "Property address is required")
    @Valid
    private PropertyAddress address;

    @PositiveOrZero(message = "Asking price must be zero or positive")
    private BigDecimal askingPrice;

    private PropertyOfferStatus offerStatus;

    @PositiveOrZero(message = "Offer amount must be zero or positive")
    private BigDecimal offerAmount;

    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{8}$", message = "Centris number must be exactly 8 digits")
    private String centrisNumber;

    private String notes;

    private com.example.courtierprobackend.transactions.datalayer.enums.PropertyStatus status;
}
