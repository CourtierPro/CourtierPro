package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.OfferRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferRevisionRepository extends JpaRepository<OfferRevision, Long> {

    List<OfferRevision> findByOfferIdOrderByRevisionNumberAsc(UUID offerId);

    Optional<OfferRevision> findByRevisionId(UUID revisionId);

    @Query("SELECT COALESCE(MAX(r.revisionNumber), 0) FROM OfferRevision r WHERE r.offerId = :offerId")
    Integer findMaxRevisionNumberByOfferId(UUID offerId);
}
