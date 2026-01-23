package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.TransactionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionParticipantRepository extends JpaRepository<TransactionParticipant, UUID> {
    List<TransactionParticipant> findByTransactionId(UUID transactionId);
    List<TransactionParticipant> findByEmail(String email);
    List<TransactionParticipant> findByEmailIgnoreCase(String email);
}
