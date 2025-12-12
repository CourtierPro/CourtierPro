package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(UUID transactionId);

    // Automatic derived query for duplicate check
    Optional<Transaction> findByClientIdAndPropertyAddress_StreetAndStatus(
            UUID clientId,
            String street,
            TransactionStatus status
    );

    List<Transaction> findAllByBrokerId(UUID brokerId);

    List<Transaction> findAllByClientId(UUID clientId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Transaction t WHERE t.brokerId = :brokerId " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:side IS NULL OR t.side = :side) " +
            "AND (:stage IS NULL OR t.buyerStage = :stage OR t.sellerStage = :stage)")
    List<Transaction> findAllByFilters(
            @org.springframework.data.repository.query.Param("brokerId") UUID brokerId,
            @org.springframework.data.repository.query.Param("status") TransactionStatus status,
            @org.springframework.data.repository.query.Param("side") com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide side,
            @org.springframework.data.repository.query.Param("stage") Enum<?> stage
    );

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Transaction t WHERE " +
            "(t.brokerId = :userId OR t.clientId = :userId) AND " +
            "(LOWER(t.propertyAddress.street) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.propertyAddress.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.propertyAddress.postalCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Transaction> searchTransactions(@org.springframework.data.repository.query.Param("userId") UUID userId,
                                         @org.springframework.data.repository.query.Param("query") String query);
    List<Transaction> findByClientIdIn(java.util.List<UUID> clientIds);

    List<Transaction> findByTransactionIdIn(java.util.List<UUID> transactionIds);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Transaction t WHERE " +
            "(t.clientId IN :userIds OR t.brokerId IN :userIds) AND " +
            "(t.brokerId = :requesterId OR t.clientId = :requesterId)")
    List<Transaction> findLinkedToUsers(@org.springframework.data.repository.query.Param("userIds") java.util.List<UUID> userIds,
                                        @org.springframework.data.repository.query.Param("requesterId") UUID requesterId);
}

