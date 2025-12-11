package com.example.courtierprobackend.documents.datalayer.valueobjects;

import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRef {

    private UUID transactionId;
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    private TransactionSide side;
}
