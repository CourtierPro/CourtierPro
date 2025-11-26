package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    // Automatic derived query for duplicate check
    Optional<Transaction> findByClientIdAndPropertyAddress_StreetAndStatus(
            String clientId,
            String street,
            TransactionStatus status
    );

    List<Transaction> findAllByBrokerId(String brokerId);
}
