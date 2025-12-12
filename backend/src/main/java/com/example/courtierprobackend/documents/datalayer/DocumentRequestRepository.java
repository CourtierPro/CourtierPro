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
            "(d.transactionRef.clientId = :userId OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId = :userId)) AND " +
            "(LOWER(COALESCE(d.customTitle, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(CAST(d.docType AS string)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(d.brokerNotes, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<DocumentRequest> searchDocuments(@Param("userId") UUID userId, @Param("query") String query);
    List<DocumentRequest> findByTransactionRefClientIdIn(java.util.List<UUID> clientIds);

    @Query("SELECT d FROM DocumentRequest d WHERE " +
            "(d.transactionRef.clientId IN :userIds OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId IN :userIds)) AND " +
            "(d.transactionRef.clientId = :requesterId OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId = :requesterId))")
    List<DocumentRequest> findLinkedToUsers(@Param("userIds") java.util.List<UUID> userIds, @Param("requesterId") UUID requesterId);
}
