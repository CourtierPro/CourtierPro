package com.example.courtierprobackend.documents.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {

    Optional<DocumentRequest> findByRequestId(String requestId);

    List<DocumentRequest> findByTransactionRef_TransactionId(String transactionId);
}
