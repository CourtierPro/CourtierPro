package com.example.courtierprobackend.transactions.datalayer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OfferRevision entity.
 * Covers lifecycle callbacks and builder patterns.
 */
class OfferRevisionTest {

    @Test
    void onCreate_setsRevisionIdWhenNull() {
        OfferRevision revision = OfferRevision.builder().build();
        assertThat(revision.getRevisionId()).isNull();

        revision.onCreate();

        assertThat(revision.getRevisionId()).isNotNull();
    }

    @Test
    void onCreate_preservesExistingRevisionId() {
        UUID existingId = UUID.randomUUID();
        OfferRevision revision = OfferRevision.builder()
                .revisionId(existingId)
                .build();

        revision.onCreate();

        assertThat(revision.getRevisionId()).isEqualTo(existingId);
    }

    @Test
    void onCreate_setsCreatedAtWhenNull() {
        OfferRevision revision = OfferRevision.builder().build();
        assertThat(revision.getCreatedAt()).isNull();

        revision.onCreate();

        assertThat(revision.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_preservesExistingCreatedAt() {
        LocalDateTime existingTime = LocalDateTime.now().minusDays(1);
        OfferRevision revision = OfferRevision.builder()
                .createdAt(existingTime)
                .build();

        revision.onCreate();

        assertThat(revision.getCreatedAt()).isEqualTo(existingTime);
    }

    @Test
    void builder_createsFullyPopulatedRevision() {
        UUID revisionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        OfferRevision revision = OfferRevision.builder()
                .id(1L)
                .revisionId(revisionId)
                .offerId(offerId)
                .revisionNumber(2)
                .previousAmount(new BigDecimal("450000.00"))
                .newAmount(new BigDecimal("475000.00"))
                .previousStatus("PENDING")
                .newStatus("COUNTER_OFFER_MADE")
                .changedBy(changedBy)
                .createdAt(now)
                .build();

        assertThat(revision.getId()).isEqualTo(1L);
        assertThat(revision.getRevisionId()).isEqualTo(revisionId);
        assertThat(revision.getOfferId()).isEqualTo(offerId);
        assertThat(revision.getRevisionNumber()).isEqualTo(2);
        assertThat(revision.getPreviousAmount()).isEqualByComparingTo(new BigDecimal("450000.00"));
        assertThat(revision.getNewAmount()).isEqualByComparingTo(new BigDecimal("475000.00"));
        assertThat(revision.getPreviousStatus()).isEqualTo("PENDING");
        assertThat(revision.getNewStatus()).isEqualTo("COUNTER_OFFER_MADE");
        assertThat(revision.getChangedBy()).isEqualTo(changedBy);
        assertThat(revision.getCreatedAt()).isEqualTo(now);
    }
}
