package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTest {
    @Test
    void testOnCreate_setsFieldsIfNull() {
        Property p = new Property();
        assertNull(p.getPropertyId());
        assertNull(p.getCreatedAt());
        assertNull(p.getUpdatedAt());
        assertNull(p.getOfferStatus());

        p.onCreate();

        assertNotNull(p.getPropertyId());
        assertNotNull(p.getCreatedAt());
        assertNotNull(p.getUpdatedAt());
        assertEquals(PropertyOfferStatus.OFFER_TO_BE_MADE, p.getOfferStatus());
    }

    @Test
    void testOnCreate_doesNotOverrideExistingFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Property p = new Property();
        p.setPropertyId(id);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        p.setOfferStatus(PropertyOfferStatus.ACCEPTED);

        p.onCreate();

        assertEquals(id, p.getPropertyId());
        assertEquals(now, p.getCreatedAt());
        assertEquals(now, p.getUpdatedAt());
        assertEquals(PropertyOfferStatus.ACCEPTED, p.getOfferStatus());
    }

    @Test
    void testOnUpdate_setsUpdatedAt() {
        Property p = new Property();
        assertNull(p.getUpdatedAt());
        p.onUpdate();
        assertNotNull(p.getUpdatedAt());
    }
}
