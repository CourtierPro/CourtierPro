package com.example.courtierprobackend.transactions.datalayer.dto;


import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TransactionResponseDTO {
    private UUID transactionId;
    private UUID clientId;
    private String clientName;
    private UUID brokerId;
    private String brokerName;
    private TransactionSide side;
    private PropertyAddress propertyAddress;
    private String centrisNumber;
    private String currentStage;
    private TransactionStatus status;
    private String openedDate;
    private String openedAt;
    private String lastUpdated;
    private Boolean archived;
    private String archivedAt;
    private String notes;
    private Integer houseVisitCount;
}


