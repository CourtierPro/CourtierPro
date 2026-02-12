package com.example.courtierprobackend.transactions.datalayer;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testOnCreate_SetsFieldsIfNull() {
        Transaction t = new Transaction();
        assertNull(t.getTransactionId());
        assertNull(t.getLastUpdated());

        t.onCreate();

        assertNotNull(t.getTransactionId());
        assertNotNull(t.getLastUpdated());
    }

    @Test
    void testOnCreate_DoesNotOverrideExistingFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        
        Transaction t = new Transaction();
        t.setTransactionId(id);
        t.setLastUpdated(past);

        t.onCreate();

        assertEquals(id, t.getTransactionId());
        assertEquals(past, t.getLastUpdated());
    }

    @Test
    void testOnUpdate_SetsLastUpdated() throws InterruptedException {
        Transaction t = new Transaction();
        LocalDateTime initial = LocalDateTime.now().minusSeconds(1);
        t.setLastUpdated(initial);

        // Ensure clear time difference if fast execution
        Thread.sleep(10); 
        
        t.onUpdate();

        assertNotNull(t.getLastUpdated());
        assertTrue(t.getLastUpdated().isAfter(initial), "lastUpdated should be after the initial time");
    }
}
