package com.example.courtierprobackend.transactions.businesslayer;


import com.example.courtierprobackend.transactions.datalayer.dto.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TransactionService {

    TransactionResponseDTO createTransaction(TransactionRequestDTO dto);

    java.util.List<TimelineEntryDTO> getNotes(UUID transactionId, UUID brokerId);

    TimelineEntryDTO createNote(UUID transactionId, NoteRequestDTO note, UUID brokerId);

    List<TransactionResponseDTO> getBrokerTransactions(UUID brokerId, String status, String stage, String side);

    List<TransactionResponseDTO> getClientTransactions(UUID clientId);
    
    TransactionResponseDTO updateTransactionStage(UUID transactionId, StageUpdateRequestDTO dto, UUID brokerId);

    TransactionResponseDTO getByTransactionId(UUID transactionId, UUID userId);

    void pinTransaction(UUID transactionId, UUID brokerId);

    void unpinTransaction(UUID transactionId, UUID brokerId);

    Set<UUID> getPinnedTransactionIds(UUID brokerId);
}

