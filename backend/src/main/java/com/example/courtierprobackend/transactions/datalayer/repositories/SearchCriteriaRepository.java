package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.SearchCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SearchCriteria entity.
 */
public interface SearchCriteriaRepository extends JpaRepository<SearchCriteria, Long> {

    Optional<SearchCriteria> findBySearchCriteriaId(UUID searchCriteriaId);

    Optional<SearchCriteria> findByTransactionId(UUID transactionId);

    void deleteByTransactionId(UUID transactionId);
}
