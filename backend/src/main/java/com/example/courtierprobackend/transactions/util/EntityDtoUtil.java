package com.example.courtierprobackend.transactions.util;

import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;

public class EntityDtoUtil {

    public static TransactionResponseDTO toResponse(Transaction t) {

        return TransactionResponseDTO.builder()
                .transactionId(t.getTransactionId())
                .clientId(t.getClientId())
                .brokerId(t.getBrokerId())
                .side(t.getSide())
                .propertyAddress(t.getPropertyAddress())
                .currentStage(determineStage(t))
                .status(t.getStatus())
                .build();
    }

    private static String determineStage(Transaction t) {
        return t.getSide() == TransactionSide.BUY_SIDE
                ? t.getBuyerStage().name()
                : t.getSellerStage().name();
    }
}
