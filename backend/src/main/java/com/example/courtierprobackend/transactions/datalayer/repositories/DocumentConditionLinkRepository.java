package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.DocumentConditionLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentConditionLinkRepository extends JpaRepository<DocumentConditionLink, Long> {

    List<DocumentConditionLink> findByOfferId(UUID offerId);

    List<DocumentConditionLink> findByPropertyOfferId(UUID propertyOfferId);

    List<DocumentConditionLink> findByDocumentId(UUID documentId);

    void deleteByOfferId(UUID offerId);

    void deleteByPropertyOfferId(UUID propertyOfferId);

    void deleteByDocumentId(UUID documentId);

    void deleteByConditionIdAndOfferId(UUID conditionId, UUID offerId);

    void deleteByConditionIdAndPropertyOfferId(UUID conditionId, UUID propertyOfferId);

    void deleteByConditionIdAndDocumentId(UUID conditionId, UUID documentId);
}
