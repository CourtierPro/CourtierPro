package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.businesslayer.TransactionServiceImpl;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository repo;

    @InjectMocks
    private TransactionServiceImpl service;

    // ------------------------------------------------
    // 1) SUCCESS CASE
    // ------------------------------------------------
    @Test
    void createTransaction_Success() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");
        dto.setPropertyAddress(new PropertyAddress("123 Main", "Montreal", "QC", "H1H1H1"));

        when(repo.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        var result = service.createTransaction(dto);

        assertThat(result.getClientId()).isEqualTo("CLIENT1");
        assertThat(result.getBrokerId()).isEqualTo("BROKER1");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
    }

    // ------------------------------------------------
    // 2) MISSING CLIENT ID → should throw InvalidInputException
    // ------------------------------------------------
    @Test
    void createTransaction_MissingClientId_Throws() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class);
    }

    // ------------------------------------------------
    // 3) MISSING SIDE → should throw InvalidInputException
    // ------------------------------------------------
    @Test
    void createTransaction_MissingSide_Throws() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setBrokerId("BROKER1");

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class);
    }

    // ------------------------------------------------
    // 4) DUPLICATE TRANSACTION → should throw InvalidInputException
    // ------------------------------------------------
    @Test
    void createTransaction_Duplicate_Throws() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setBrokerId("BROKER1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setPropertyAddress(new PropertyAddress("123 Main", "Montreal", "QC", "H1H1H1"));

        when(repo.findByClientIdAndPropertyAddress_StreetAndStatus(
                "CLIENT1",
                "123 Main",
                TransactionStatus.ACTIVE
        )).thenReturn(Optional.of(new Transaction()));

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("duplicate");
    }


    // ------------------------------------------------
    // 5) Repo save throws → bubble up exception
    // ------------------------------------------------
    @Test
    void createTransaction_RepoFails_ThrowsException() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setBrokerId("BROKER1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setPropertyAddress(new PropertyAddress("123 Main", "Montreal", "QC", "H1H1H1"));

        when(repo.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }

    // ------------------------------------------------
    // 6) Verify repo.save() is called exactly once
    // ------------------------------------------------
    @Test
    void createTransaction_VerifyRepoSaveCalledOnce() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");
        dto.setPropertyAddress(new PropertyAddress("123 Main", "MTL", "QC", "H1H1H1"));

        when(repo.save(any(Transaction.class))).thenReturn(new Transaction());

        service.createTransaction(dto);

        verify(repo, times(1)).save(any(Transaction.class));
    }

    // ------------------------------------------------
    // 7) Ensure fields & defaults are set correctly
    // ------------------------------------------------
    @Test
    void createTransaction_DefaultsCorrect() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");
        dto.setPropertyAddress(new PropertyAddress("123 Main", "MTL", "QC", "H1H1H1"));

        when(repo.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createTransaction(dto);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(result.getCurrentStage()).isEqualTo(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY.name());
        assertThat(result.getBrokerId()).isEqualTo("BROKER1");
    }
}
