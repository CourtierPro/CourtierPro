package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        Optional<Transaction> findByTransactionId(UUID transactionId);

        // Automatic derived query for duplicate check
        Optional<Transaction> findByClientIdAndPropertyAddress_StreetAndStatus(
                        UUID clientId,
                        String street,
                        TransactionStatus status);

        List<Transaction> findAllByBrokerId(UUID brokerId);

        List<Transaction> findAllByClientId(UUID clientId);

        @Query("SELECT t FROM Transaction t WHERE t.brokerId = :brokerId " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND (:side IS NULL OR t.side = :side) " +
                        "AND (:stage IS NULL OR t.buyerStage = :stage OR t.sellerStage = :stage) " +
                        "AND (:includeArchived = true OR t.archived = false)")
        List<Transaction> findAllByFilters(
                        @Param("brokerId") UUID brokerId,
                        @Param("status") TransactionStatus status,
                        @Param("side") com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide side,
                        @Param("stage") Enum<?> stage,
                        @Param("includeArchived") boolean includeArchived);

        // Query to get only archived transactions for a broker
        @Query("SELECT t FROM Transaction t WHERE t.brokerId = :brokerId AND t.archived = true")
        List<Transaction> findArchivedByBrokerId(@Param("brokerId") UUID brokerId);

        // Query to get non-archived transactions for a broker
        List<Transaction> findAllByBrokerIdAndArchivedFalse(UUID brokerId);

        @Query("SELECT t FROM Transaction t WHERE " +
                        "(t.brokerId = :userId OR t.clientId = :userId) AND " +
                        "(LOWER(COALESCE(t.propertyAddress.street, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(COALESCE(t.propertyAddress.city, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(COALESCE(t.propertyAddress.postalCode, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "EXISTS (SELECT p FROM Property p WHERE p.transactionId = t.transactionId AND p.centrisNumber LIKE CONCAT('%', :query, '%')))")
        List<Transaction> searchTransactions(@Param("userId") UUID userId,
                        @Param("query") String query);

        List<Transaction> findByClientIdIn(java.util.List<UUID> clientIds);

        List<Transaction> findByTransactionIdIn(java.util.List<UUID> transactionIds);

        @Query("SELECT t FROM Transaction t WHERE " +
                        "(t.clientId IN :userIds OR t.brokerId IN :userIds) AND " +
                        "(t.brokerId = :requesterId OR t.clientId = :requesterId)")
        List<Transaction> findLinkedToUsers(@Param("userIds") java.util.List<UUID> userIds,
                        @Param("requesterId") UUID requesterId);

        @Query("SELECT t FROM Transaction t WHERE t.transactionId IN (SELECT p.transactionId FROM TransactionParticipant p WHERE p.email = :email) AND t.archived = false")
        List<Transaction> findAllByParticipantEmail(@Param("email") String email);

        // Admin queries - bypass @Where filter to see all records including
        // soft-deleted
        @Query(value = "SELECT * FROM transactions ORDER BY opened_at DESC", nativeQuery = true)
        List<Transaction> findAllIncludingDeleted();

        @Query(value = "SELECT * FROM transactions WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
        List<Transaction> findAllDeleted();

        @Query(value = "SELECT * FROM transactions WHERE transaction_id = :transactionId", nativeQuery = true)
        Optional<Transaction> findByTransactionIdIncludingDeleted(@Param("transactionId") UUID transactionId);
}
