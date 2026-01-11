package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String notes; // Only populated for brokers
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
