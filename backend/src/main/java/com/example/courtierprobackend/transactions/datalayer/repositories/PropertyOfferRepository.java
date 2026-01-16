package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.PropertyOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyOfferRepository extends JpaRepository<PropertyOffer, Long> {

    List<PropertyOffer> findByPropertyIdOrderByOfferRoundDesc(UUID propertyId);

    Optional<PropertyOffer> findByPropertyOfferId(UUID propertyOfferId);

    void deleteByPropertyOfferId(UUID propertyOfferId);

    @Query("SELECT COALESCE(MAX(po.offerRound), 0) FROM PropertyOffer po WHERE po.propertyId = :propertyId")
    Integer findMaxOfferRoundByPropertyId(UUID propertyId);

    Optional<PropertyOffer> findTopByPropertyIdOrderByOfferRoundDesc(UUID propertyId);
}
