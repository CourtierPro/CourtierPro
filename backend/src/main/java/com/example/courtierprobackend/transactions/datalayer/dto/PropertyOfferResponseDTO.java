package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.BuyerOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.CounterpartyResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PropertyOfferResponseDTO {

    private UUID propertyOfferId;
    private UUID propertyId;
    private Integer offerRound;
    private BigDecimal offerAmount;
    private BuyerOfferStatus status;
    private CounterpartyResponse counterpartyResponse;
    private LocalDate expiryDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OfferDocumentResponseDTO> documents;
    private List<ConditionResponseDTO> conditions;
}
