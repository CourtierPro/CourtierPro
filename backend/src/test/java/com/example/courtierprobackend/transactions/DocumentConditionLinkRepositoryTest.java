package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.datalayer.DocumentConditionLink;
import com.example.courtierprobackend.transactions.datalayer.repositories.DocumentConditionLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for DocumentConditionLinkRepository.
 * Tests the custom finder and delete methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class DocumentConditionLinkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DocumentConditionLinkRepository repository;

    private UUID conditionId1;
    private UUID conditionId2;
    private UUID offerId;
    private UUID propertyOfferId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        conditionId1 = UUID.randomUUID();
        conditionId2 = UUID.randomUUID();
        offerId = UUID.randomUUID();
        propertyOfferId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("should find links by offerId")
    void findByOfferId_returnsLinks() {
        // Create test data
        DocumentConditionLink link1 = DocumentConditionLink.builder()
                .conditionId(conditionId1)
                .offerId(offerId)
                .createdAt(LocalDateTime.now())
                .build();
        DocumentConditionLink link2 = DocumentConditionLink.builder()
                .conditionId(conditionId2)
                .offerId(offerId)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(link1);
        entityManager.persist(link2);
        entityManager.flush();

        List<DocumentConditionLink> result = repository.findByOfferId(offerId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("conditionId")
                .containsExactlyInAnyOrder(conditionId1, conditionId2);
    }

    @Test
    @DisplayName("should find links by propertyOfferId")
    void findByPropertyOfferId_returnsLinks() {
        DocumentConditionLink link = DocumentConditionLink.builder()
                .conditionId(conditionId1)
                .propertyOfferId(propertyOfferId)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(link);
        entityManager.flush();

        List<DocumentConditionLink> result = repository.findByPropertyOfferId(propertyOfferId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConditionId()).isEqualTo(conditionId1);
    }

    @Test
    @DisplayName("should find links by documentId")
    void findByDocumentId_returnsLinks() {
        DocumentConditionLink link = DocumentConditionLink.builder()
                .conditionId(conditionId1)
                .documentId(documentId)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(link);
        entityManager.flush();

        List<DocumentConditionLink> result = repository.findByDocumentId(documentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConditionId()).isEqualTo(conditionId1);
    }

    @Test
    @DisplayName("should return empty list when no links exist")
    void findByOfferId_noLinks_returnsEmptyList() {
        List<DocumentConditionLink> result = repository.findByOfferId(UUID.randomUUID());
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should delete links by offerId")
    void deleteByOfferId_removesLinks() {
        DocumentConditionLink link = DocumentConditionLink.builder()
                .conditionId(conditionId1)
                .offerId(offerId)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(link);
        entityManager.flush();

        repository.deleteByOfferId(offerId);
        entityManager.flush();

        List<DocumentConditionLink> result = repository.findByOfferId(offerId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should delete links by propertyOfferId")
    void deleteByPropertyOfferId_removesLinks() {
        DocumentConditionLink link = DocumentConditionLink.builder()
                .conditionId(conditionId1)
                .propertyOfferId(propertyOfferId)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(link);
        entityManager.flush();

        repository.deleteByPropertyOfferId(propertyOfferId);
        entityManager.flush();

        List<DocumentConditionLink> result = repository.findByPropertyOfferId(propertyOfferId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should delete links by documentId")
    void deleteByDocumentId_removesLinks() {
        DocumentConditionLink link = DocumentConditionLink.builder()
                .conditionId(conditionId1)
                .documentId(documentId)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(link);
        entityManager.flush();

        repository.deleteByDocumentId(documentId);
        entityManager.flush();

        List<DocumentConditionLink> result = repository.findByDocumentId(documentId);
        assertThat(result).isEmpty();
    }
}
