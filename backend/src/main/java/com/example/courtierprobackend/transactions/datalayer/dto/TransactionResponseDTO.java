package com.example.courtierprobackend.transactions.datalayer.dto;


import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponseDTO {
    private String transactionId;
    private String clientId;
    private String clientName;
    private String brokerId;
    private TransactionSide side;
    private PropertyAddress propertyAddress;
    private String currentStage;
    private TransactionStatus status;
    private String openedDate;
}

