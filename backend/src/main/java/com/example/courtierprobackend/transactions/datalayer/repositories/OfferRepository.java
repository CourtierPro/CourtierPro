package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByTransactionIdOrderByCreatedAtDesc(UUID transactionId);

    Optional<Offer> findByOfferId(UUID offerId);

    void deleteByOfferId(UUID offerId);
}
