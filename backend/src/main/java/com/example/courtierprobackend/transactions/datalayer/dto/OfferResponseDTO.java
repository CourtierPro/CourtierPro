package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ClientOfferDecision;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
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
public class OfferResponseDTO {

    private UUID offerId;
    private UUID transactionId;
    private String buyerName;
    private BigDecimal offerAmount;
    private ReceivedOfferStatus status;
    private LocalDate expiryDate;
    private String notes;
    
    // Client decision fields
    private ClientOfferDecision clientDecision;
    private LocalDateTime clientDecisionAt;
    private String clientDecisionNotes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OfferDocumentResponseDTO> documents;
    private List<ConditionResponseDTO> conditions;
}

