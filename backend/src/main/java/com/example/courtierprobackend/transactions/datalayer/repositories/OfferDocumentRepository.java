package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.OfferDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferDocumentRepository extends JpaRepository<OfferDocument, Long> {

    List<OfferDocument> findByOfferIdOrderByCreatedAtDesc(UUID offerId);

    List<OfferDocument> findByPropertyOfferIdOrderByCreatedAtDesc(UUID propertyOfferId);

    Optional<OfferDocument> findByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
