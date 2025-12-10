package com.example.courtierprobackend.transactions.businesslayer;


import com.example.courtierprobackend.transactions.datalayer.dto.*;

import java.util.List;

public interface TransactionService {

    TransactionResponseDTO createTransaction(TransactionRequestDTO dto);

    java.util.List<TimelineEntryDTO> getNotes(String transactionId, String brokerId);

    TimelineEntryDTO createNote(String transactionId, NoteRequestDTO note, String brokerId);

    java.util.List<TimelineEntryDTO> getClientTransactionTimeline(String transactionId, String clientId);

    List<TransactionResponseDTO> getBrokerTransactions(String brokerId, String status, String stage, String side);

    List<TransactionResponseDTO> getClientTransactions(String clientId);
    
    TransactionResponseDTO updateTransactionStage(String transactionId, StageUpdateRequestDTO dto, String brokerId);

    TransactionResponseDTO getByTransactionId(String transactionId, String userId);
}

