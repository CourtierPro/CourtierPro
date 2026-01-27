package com.example.courtierprobackend.documents.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {

        Optional<DocumentRequest> findByRequestId(UUID requestId);

        List<DocumentRequest> findByTransactionRef_TransactionId(UUID transactionId);

        @Query("SELECT d FROM DocumentRequest d WHERE d.transactionRef.clientId = :userId OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId = :userId)")
        List<DocumentRequest> findByUserId(@Param("userId") UUID userId);

        @Query("SELECT d FROM DocumentRequest d WHERE " +
                        "(d.transactionRef.clientId = :userId OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId = :userId)) AND "
                        +
                        "(LOWER(COALESCE(d.customTitle, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(CONCAT('', d.docType)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(COALESCE(d.brokerNotes, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
        List<DocumentRequest> searchDocuments(@Param("userId") UUID userId, @Param("query") String query);

        // Derived query method used for optimizations where explicit JOINs are
        // difficult
        List<DocumentRequest> findByTransactionRefClientIdIn(java.util.List<UUID> clientIds);

        @Query("SELECT d FROM DocumentRequest d WHERE " +
                        "(d.transactionRef.clientId IN :userIds OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId IN :userIds)) AND "
                        +
                        "(d.transactionRef.clientId = :requesterId OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId = :requesterId))")
        List<DocumentRequest> findLinkedToUsers(@Param("userIds") java.util.List<UUID> userIds,
                        @Param("requesterId") UUID requesterId);

        // Admin queries - bypass @Where filter to see all records including
        // soft-deleted
        @Query(value = "SELECT * FROM document_requests ORDER BY last_updated_at DESC NULLS LAST", nativeQuery = true)
        List<DocumentRequest> findAllIncludingDeleted();

        @Query(value = "SELECT * FROM document_requests WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
        List<DocumentRequest> findAllDeleted();

        @Query(value = "SELECT * FROM document_requests WHERE request_id = :requestId", nativeQuery = true)
        Optional<DocumentRequest> findByRequestIdIncludingDeleted(@Param("requestId") UUID requestId);

        @Query(value = "SELECT * FROM document_requests WHERE transaction_id = :transactionId", nativeQuery = true)
        List<DocumentRequest> findByTransactionIdIncludingDeleted(@Param("transactionId") UUID transactionId);

        @Query("SELECT d FROM DocumentRequest d " +
                        "JOIN Transaction t ON d.transactionRef.transactionId = t.transactionId " +
                        "WHERE t.brokerId = :brokerId " +
                        "AND (d.status = 'REQUESTED' OR d.status = 'NEEDS_REVISION') " +
                        "ORDER BY d.dueDate ASC")
        List<DocumentRequest> findOutstandingDocumentsForBroker(@Param("brokerId") UUID brokerId);
}
