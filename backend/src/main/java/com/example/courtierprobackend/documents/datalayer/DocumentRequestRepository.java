package com.example.courtierprobackend.documents.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {

    Optional<DocumentRequest> findByRequestId(String requestId);

    List<DocumentRequest> findByTransactionRef_TransactionId(String transactionId);

    @Query("SELECT d FROM DocumentRequest d WHERE d.transactionRef.clientId = :userId OR d.transactionRef.transactionId IN (SELECT t.transactionId FROM Transaction t WHERE t.brokerId = :userId)")
    List<DocumentRequest> findByUserId(@Param("userId") String userId);
}
