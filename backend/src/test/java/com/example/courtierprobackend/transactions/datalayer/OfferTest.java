package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OfferTest {
    @Test
    void testOnCreate_setsFieldsIfNull() {
        Offer o = new Offer();
        assertNull(o.getOfferId());
        assertNull(o.getCreatedAt());
        assertNull(o.getUpdatedAt());
        assertNull(o.getStatus());

        o.onCreate();

        assertNotNull(o.getOfferId());
        assertNotNull(o.getCreatedAt());
        assertNotNull(o.getUpdatedAt());
        assertEquals(ReceivedOfferStatus.PENDING, o.getStatus());
    }

    @Test
    void testOnCreate_doesNotOverrideExistingFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Offer o = new Offer();
        o.setOfferId(id);
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        o.setStatus(ReceivedOfferStatus.ACCEPTED);

        o.onCreate();

        assertEquals(id, o.getOfferId());
        assertEquals(now, o.getCreatedAt());
        assertEquals(now, o.getUpdatedAt());
        assertEquals(ReceivedOfferStatus.ACCEPTED, o.getStatus());
    }

    @Test
    void testOnUpdate_setsUpdatedAt() {
        Offer o = new Offer();
        assertNull(o.getUpdatedAt());
        o.onUpdate();
        assertNotNull(o.getUpdatedAt());
    }
}
