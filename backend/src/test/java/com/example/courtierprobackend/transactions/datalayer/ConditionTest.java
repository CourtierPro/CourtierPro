package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConditionTest {
    @Test
    void testOnCreate_setsFieldsIfNull() {
        Condition c = new Condition();
        assertNull(c.getConditionId());
        assertNull(c.getCreatedAt());
        assertNull(c.getUpdatedAt());
        assertNull(c.getStatus());

        c.onCreate();

        assertNotNull(c.getConditionId());
        assertNotNull(c.getCreatedAt());
        assertNotNull(c.getUpdatedAt());
        assertEquals(ConditionStatus.PENDING, c.getStatus());
    }

    @Test
    void testOnCreate_doesNotOverrideExistingFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Condition c = new Condition();
        c.setConditionId(id);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        c.setStatus(ConditionStatus.SATISFIED);

        c.onCreate();

        assertEquals(id, c.getConditionId());
        assertEquals(now, c.getCreatedAt());
        assertEquals(now, c.getUpdatedAt());
        assertEquals(ConditionStatus.SATISFIED, c.getStatus());
    }

    @Test
    void testOnUpdate_setsUpdatedAt() {
        Condition c = new Condition();
        assertNull(c.getUpdatedAt());
        c.onUpdate();
        assertNotNull(c.getUpdatedAt());
    }
}
