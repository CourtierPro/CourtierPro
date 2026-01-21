package com.example.courtierprobackend.transactions.datalayer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OfferDocument entity.
 * Covers lifecycle callbacks and builder patterns.
 */
class OfferDocumentTest {

    @Test
    void onCreate_setsDocumentIdWhenNull() {
        OfferDocument document = OfferDocument.builder().build();
        assertThat(document.getDocumentId()).isNull();

        document.onCreate();

        assertThat(document.getDocumentId()).isNotNull();
    }

    @Test
    void onCreate_preservesExistingDocumentId() {
        UUID existingId = UUID.randomUUID();
        OfferDocument document = OfferDocument.builder()
                .documentId(existingId)
                .build();

        document.onCreate();

        assertThat(document.getDocumentId()).isEqualTo(existingId);
    }

    @Test
    void onCreate_setsCreatedAtWhenNull() {
        OfferDocument document = OfferDocument.builder().build();
        assertThat(document.getCreatedAt()).isNull();

        document.onCreate();

        assertThat(document.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_preservesExistingCreatedAt() {
        LocalDateTime existingTime = LocalDateTime.now().minusDays(1);
        OfferDocument document = OfferDocument.builder()
                .createdAt(existingTime)
                .build();

        document.onCreate();

        assertThat(document.getCreatedAt()).isEqualTo(existingTime);
    }

    @Test
    void builder_createsFullyPopulatedDocument() {
        UUID documentId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID propertyOfferId = UUID.randomUUID();
        UUID uploadedBy = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        OfferDocument document = OfferDocument.builder()
                .id(1L)
                .documentId(documentId)
                .offerId(offerId)
                .propertyOfferId(propertyOfferId)
                .s3Key("documents/offer/doc.pdf")
                .fileName("Promise_to_Purchase.pdf")
                .mimeType("application/pdf")
                .sizeBytes(1024000L)
                .uploadedBy(uploadedBy)
                .createdAt(now)
                .build();

        assertThat(document.getId()).isEqualTo(1L);
        assertThat(document.getDocumentId()).isEqualTo(documentId);
        assertThat(document.getOfferId()).isEqualTo(offerId);
        assertThat(document.getPropertyOfferId()).isEqualTo(propertyOfferId);
        assertThat(document.getS3Key()).isEqualTo("documents/offer/doc.pdf");
        assertThat(document.getFileName()).isEqualTo("Promise_to_Purchase.pdf");
        assertThat(document.getMimeType()).isEqualTo("application/pdf");
        assertThat(document.getSizeBytes()).isEqualTo(1024000L);
        assertThat(document.getUploadedBy()).isEqualTo(uploadedBy);
        assertThat(document.getCreatedAt()).isEqualTo(now);
    }
}
