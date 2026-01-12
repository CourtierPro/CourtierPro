package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, Long> {

    List<Condition> findByTransactionIdOrderByDeadlineDateAsc(UUID transactionId);

    Optional<Condition> findByConditionId(UUID conditionId);
}
