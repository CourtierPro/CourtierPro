package com.example.courtierprobackend.transactions.datalayer.repositories;

import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage;
import com.example.courtierprobackend.transactions.datalayer.enums.SellerStage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @Test
    void searchTransactions_WithEnumFilters_ReturnsMatchingTransactions() {
        UUID brokerId = UUID.randomUUID();
        Transaction t1 = new Transaction();
        t1.setBrokerId(brokerId);
        t1.setStatus(TransactionStatus.ACTIVE);
        t1.setSide(TransactionSide.BUY_SIDE);
        t1.setBuyerStage(BuyerStage.BUYER_PROPERTY_SEARCH);
        // Ensure other required fields if any (Entity likely has defaults or nullables)
        
        Transaction t2 = new Transaction();
        t2.setBrokerId(brokerId);
        t2.setStatus(TransactionStatus.CLOSED_SUCCESSFULLY);
        t2.setSide(TransactionSide.SELL_SIDE);
        t2.setSellerStage(SellerStage.SELLER_OFFER_AND_NEGOTIATION);
        
        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();

        // Search for Active transactions (should matches t1)
        List<Transaction> resultsStatus = transactionRepository.searchTransactions(
            brokerId, "NonMatchingString", Set.of(TransactionStatus.ACTIVE), null, null, null
        );
        assertThat(resultsStatus).hasSize(1);
        assertThat(resultsStatus.get(0).getId()).isEqualTo(t1.getId());

        // Search for Buy Side (should match t1)
        List<Transaction> resultsSide = transactionRepository.searchTransactions(
            brokerId, "NonMatchingString", null, Set.of(TransactionSide.BUY_SIDE), null, null
        );
        assertThat(resultsSide).hasSize(1);
        assertThat(resultsSide.get(0).getId()).isEqualTo(t1.getId());
        
        // Search mixed (should match t2)
        List<Transaction> resultsStage = transactionRepository.searchTransactions(
            brokerId, "NonMatchingString", null, null, null, Set.of(SellerStage.SELLER_OFFER_AND_NEGOTIATION)
        );
        assertThat(resultsStage).hasSize(1);
        assertThat(resultsStage.get(0).getId()).isEqualTo(t2.getId());
    }
}
