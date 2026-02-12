package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testPrePersist_SetsLastUpdated() {
        Transaction t = new Transaction();
        t.setStatus(TransactionStatus.ACTIVE);
        
        // Persist using repository
        Transaction saved = transactionRepository.save(t);
        // Flush to force DB write (triggers PrePersist)
        entityManager.flush(); 
        // Refresh to get any DB-generated values
        entityManager.refresh(saved);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransactionId()).isNotNull(); // Check UUID gen
        assertThat(saved.getLastUpdated()).isNotNull();
        assertThat(saved.getArchived()).isFalse(); // Check default value
    }

    @Test
    void testPreUpdate_UpdatesLastUpdated() throws InterruptedException {
        Transaction t = new Transaction();
        t.setStatus(TransactionStatus.ACTIVE);
        Transaction saved = transactionRepository.save(t);
        entityManager.flush();
        
        LocalDateTime firstUpdate = saved.getLastUpdated();
        assertThat(firstUpdate).isNotNull();

        // Ensure measurable time difference
        Thread.sleep(50);

        saved.setStatus(TransactionStatus.CLOSED_SUCCESSFULLY);
        saved.setNotes("Updating transaction status");
        
        saved = transactionRepository.save(saved);
        entityManager.flush();
        entityManager.refresh(saved);

        assertThat(saved.getLastUpdated()).isNotNull();
        assertThat(saved.getLastUpdated()).isAfter(firstUpdate);
    }
}
