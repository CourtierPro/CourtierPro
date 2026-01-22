package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponseDTO {

    private UUID propertyId;
    private UUID transactionId;
    private PropertyAddressDTO address;
    private BigDecimal askingPrice;
    private PropertyOfferStatus offerStatus;
    private BigDecimal offerAmount;
    private String centrisNumber;
    private String notes;
    private com.example.courtierprobackend.transactions.datalayer.enums.PropertyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
