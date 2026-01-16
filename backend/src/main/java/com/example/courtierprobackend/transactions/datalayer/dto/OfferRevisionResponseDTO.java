package com.example.courtierprobackend.transactions.datalayer.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferRevisionResponseDTO {

    private UUID revisionId;
    private UUID offerId;
    private Integer revisionNumber;
    private BigDecimal previousAmount;
    private BigDecimal newAmount;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime createdAt;
}
