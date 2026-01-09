package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByTransactionIdOrderByCreatedAtDesc(UUID transactionId);

    Optional<Property> findByPropertyId(UUID propertyId);

    void deleteByPropertyId(UUID propertyId);

    boolean existsByPropertyId(UUID propertyId);
}
