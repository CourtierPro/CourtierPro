package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.PinnedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PinnedTransactionRepository extends JpaRepository<PinnedTransaction, Long> {

    List<PinnedTransaction> findAllByBrokerId(UUID brokerId);

    Optional<PinnedTransaction> findByBrokerIdAndTransactionId(UUID brokerId, UUID transactionId);

    void deleteByBrokerIdAndTransactionId(UUID brokerId, UUID transactionId);

    boolean existsByBrokerIdAndTransactionId(UUID brokerId, UUID transactionId);
}
