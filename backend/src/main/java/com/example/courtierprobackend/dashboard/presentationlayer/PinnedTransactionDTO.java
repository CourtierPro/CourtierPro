package com.example.courtierprobackend.dashboard.presentationlayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for pinned transaction summary in the broker dashboard.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PinnedTransactionDTO {
    private UUID transactionId;
    private String clientName;
    private String propertyAddress;
    private String side;
    private String status;
    private String currentStage;
}
