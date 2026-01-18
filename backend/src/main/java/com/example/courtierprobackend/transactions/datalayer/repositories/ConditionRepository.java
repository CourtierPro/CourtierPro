package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, Long> {

    List<Condition> findByTransactionIdOrderByDeadlineDateAsc(UUID transactionId);

    Optional<Condition> findByConditionId(UUID conditionId);

    /**
     * Find conditions for given transactions with a specific status and deadline within a date range.
     * Used for dashboard risk indicators to find approaching condition deadlines.
     */
    List<Condition> findByTransactionIdInAndStatusAndDeadlineDateBetween(
            Collection<UUID> transactionIds,
            ConditionStatus status,
            LocalDate startDate,
            LocalDate endDate
    );
}
