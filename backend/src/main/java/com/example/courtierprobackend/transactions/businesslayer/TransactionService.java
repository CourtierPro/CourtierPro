package com.example.courtierprobackend.transactions.businesslayer;


import com.example.courtierprobackend.transactions.datalayer.dto.*;

import java.util.List;

public interface TransactionService {

    TransactionResponseDTO createTransaction(TransactionRequestDTO dto);

    java.util.List<TimelineEntryDTO> getNotes(String transactionId, String brokerId);

    TimelineEntryDTO createNote(String transactionId, NoteRequestDTO note, String brokerId);

    List<TransactionResponseDTO> getBrokerTransactions(String brokerId);

    TransactionResponseDTO getByTransactionId(String transactionId, String brokerId);
}

