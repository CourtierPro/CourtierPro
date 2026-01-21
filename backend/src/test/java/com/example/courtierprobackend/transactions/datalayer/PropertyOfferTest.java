package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.BuyerOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.CounterpartyResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PropertyOffer entity.
 * Covers lifecycle callbacks and builder patterns.
 */
class PropertyOfferTest {

    @Test
    void onCreate_setsPropertyOfferIdWhenNull() {
        PropertyOffer offer = PropertyOffer.builder().build();
        assertThat(offer.getPropertyOfferId()).isNull();

        offer.onCreate();

        assertThat(offer.getPropertyOfferId()).isNotNull();
    }

    @Test
    void onCreate_preservesExistingPropertyOfferId() {
        UUID existingId = UUID.randomUUID();
        PropertyOffer offer = PropertyOffer.builder()
                .propertyOfferId(existingId)
                .build();

        offer.onCreate();

        assertThat(offer.getPropertyOfferId()).isEqualTo(existingId);
    }

    @Test
    void onCreate_setsCreatedAtWhenNull() {
        PropertyOffer offer = PropertyOffer.builder().build();
        assertThat(offer.getCreatedAt()).isNull();

        offer.onCreate();

        assertThat(offer.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_setsUpdatedAtWhenNull() {
        PropertyOffer offer = PropertyOffer.builder().build();
        assertThat(offer.getUpdatedAt()).isNull();

        offer.onCreate();

        assertThat(offer.getUpdatedAt()).isNotNull();
    }

    @Test
    void onCreate_setsDefaultStatus() {
        PropertyOffer offer = PropertyOffer.builder().build();
        assertThat(offer.getStatus()).isNull();

        offer.onCreate();

        assertThat(offer.getStatus()).isEqualTo(BuyerOfferStatus.OFFER_MADE);
    }

    @Test
    void onCreate_preservesExistingStatus() {
        PropertyOffer offer = PropertyOffer.builder()
                .status(BuyerOfferStatus.ACCEPTED)
                .build();

        offer.onCreate();

        assertThat(offer.getStatus()).isEqualTo(BuyerOfferStatus.ACCEPTED);
    }

    @Test
    void onCreate_setsDefaultOfferRound() {
        PropertyOffer offer = PropertyOffer.builder().build();
        assertThat(offer.getOfferRound()).isNull();

        offer.onCreate();

        assertThat(offer.getOfferRound()).isEqualTo(1);
    }

    @Test
    void onCreate_preservesExistingOfferRound() {
        PropertyOffer offer = PropertyOffer.builder()
                .offerRound(3)
                .build();

        offer.onCreate();

        assertThat(offer.getOfferRound()).isEqualTo(3);
    }

    @Test
    void onUpdate_updatesTimestamp() {
        LocalDateTime originalTime = LocalDateTime.now().minusHours(1);
        PropertyOffer offer = PropertyOffer.builder()
                .updatedAt(originalTime)
                .build();

        offer.onUpdate();

        assertThat(offer.getUpdatedAt()).isAfter(originalTime);
    }

    @Test
    void builder_createsFullyPopulatedOffer() {
        UUID propertyOfferId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PropertyOffer offer = PropertyOffer.builder()
                .id(1L)
                .propertyOfferId(propertyOfferId)
                .propertyId(propertyId)
                .offerRound(2)
                .offerAmount(new BigDecimal("500000.00"))
                .status(BuyerOfferStatus.COUNTERED)
                .counterpartyResponse(CounterpartyResponse.ACCEPTED)
                .expiryDate(LocalDate.now().plusDays(7))
                .notes("Test notes")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(offer.getId()).isEqualTo(1L);
        assertThat(offer.getPropertyOfferId()).isEqualTo(propertyOfferId);
        assertThat(offer.getPropertyId()).isEqualTo(propertyId);
        assertThat(offer.getOfferRound()).isEqualTo(2);
        assertThat(offer.getOfferAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(offer.getStatus()).isEqualTo(BuyerOfferStatus.COUNTERED);
        assertThat(offer.getCounterpartyResponse()).isEqualTo(CounterpartyResponse.ACCEPTED);
        assertThat(offer.getExpiryDate()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(offer.getNotes()).isEqualTo("Test notes");
    }
}
