package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import lombok.Data;

@Data
public class TransactionRequestDTO {
    private String clientId;
    private String brokerId;
    private TransactionSide side;
    private PropertyAddress propertyAddress;
}
