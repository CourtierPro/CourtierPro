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

    List<Transaction> findAllByClientId(String clientId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Transaction t WHERE t.brokerId = :brokerId " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:side IS NULL OR t.side = :side) " +
            "AND (:stage IS NULL OR t.buyerStage = :stage OR t.sellerStage = :stage)")
    List<Transaction> findAllByFilters(
            @org.springframework.data.repository.query.Param("brokerId") String brokerId,
            @org.springframework.data.repository.query.Param("status") TransactionStatus status,
            @org.springframework.data.repository.query.Param("side") com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide side,
            @org.springframework.data.repository.query.Param("stage") Enum<?> stage
    );
}
