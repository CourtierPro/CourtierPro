package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.businesslayer.TransactionServiceImpl;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        sampleTransaction = new Transaction();
        sampleTransaction.setId(1L);
        sampleTransaction.setTransactionId("TX-12345678");
        sampleTransaction.setClientId("CLIENT1");
        sampleTransaction.setBrokerId("BROKER1");
        sampleTransaction.setSide(TransactionSide.BUY_SIDE);
        sampleTransaction.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        sampleTransaction.setStatus(TransactionStatus.ACTIVE);
        sampleTransaction.setPropertyAddress(new PropertyAddress("123 Main", "Montreal", "QC", "H1H1H1"));
        sampleTransaction.setOpenedAt(LocalDateTime.now());
        sampleTransaction.setTimeline(new ArrayList<>());
    }

    // ==================== createTransaction tests ====================

    @Test
    void createTransaction_success_returnsResponse() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");
        dto.setPropertyAddress(new PropertyAddress("123 Main", "Montreal", "QC", "H1H1H1"));
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(repo.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        var result = service.createTransaction(dto);

        assertThat(result.getClientId()).isEqualTo("CLIENT1");
        assertThat(result.getBrokerId()).isEqualTo("BROKER1");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(result.getOpenedDate()).isNotNull();
        assertThat(result.getCurrentStage()).isEqualTo("BUYER_PREQUALIFY_FINANCIALLY");
    }

    @Test
    void createTransaction_MissingClientId_Throws() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void createTransaction_MissingSide_Throws() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setBrokerId("BROKER1");

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void createTransaction_Duplicate_Throws() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setBrokerId("BROKER1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");
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

    @Test
    void createTransaction_RepoFails_ThrowsException() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setBrokerId("BROKER1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");
        dto.setPropertyAddress(new PropertyAddress("123 Main", "Montreal", "QC", "H1H1H1"));

        when(repo.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> service.createTransaction(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }

    @Test
    void createTransaction_VerifyRepoSaveCalledOnce() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");
        dto.setPropertyAddress(new PropertyAddress("123 Main", "MTL", "QC", "H1H1H1"));

        when(repo.save(any(Transaction.class))).thenReturn(new Transaction());

        service.createTransaction(dto);

        verify(repo, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_DefaultsCorrect() {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setBrokerId("BROKER1");
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");
        dto.setPropertyAddress(new PropertyAddress("123 Main", "MTL", "QC", "H1H1H1"));

        when(repo.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createTransaction(dto);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(result.getCurrentStage()).isEqualTo(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY.name());
        assertThat(result.getBrokerId()).isEqualTo("BROKER1");
    }

    // ==================== getNotes tests ====================

    @Test
    void getNotes_success_returnsNotes() {
        TimelineEntry noteEntry = TimelineEntry.builder()
                .type(TimelineEntryType.NOTE)
                .title("Test Note")
                .note("Note content")
                .visibleToClient(false)
                .occurredAt(LocalDateTime.now())
                .addedByBrokerId("BROKER1")
                .build();
        sampleTransaction.getTimeline().add(noteEntry);

        when(repo.findByTransactionId("TX-12345678")).thenReturn(Optional.of(sampleTransaction));

        List<TimelineEntryDTO> result = service.getNotes("TX-12345678", "BROKER1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Note");
    }

    @Test
    void getNotes_emptyTimeline_returnsEmptyList() {
        sampleTransaction.setTimeline(null);

        when(repo.findByTransactionId("TX-12345678")).thenReturn(Optional.of(sampleTransaction));

        List<TimelineEntryDTO> result = service.getNotes("TX-12345678", "BROKER1");

        assertThat(result).isEmpty();
    }

    @Test
    void getNotes_notOwner_throwsNotFoundException() {
        when(repo.findByTransactionId("TX-12345678")).thenReturn(Optional.of(sampleTransaction));

        assertThatThrownBy(() -> service.getNotes("TX-12345678", "OTHER_BROKER"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void getNotes_transactionNotFound_throwsNotFoundException() {
        when(repo.findByTransactionId("TX-UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNotes("TX-UNKNOWN", "BROKER1"))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== createNote tests ====================

    @Test
    void createNote_success_createsNote() {
        when(repo.findByTransactionId("TX-12345678")).thenReturn(Optional.of(sampleTransaction));
        when(repo.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteRequestDTO note = new NoteRequestDTO();
        note.setActorId("BROKER1");
        note.setTitle("New Note");
        note.setMessage("Note message");
        note.setVisibleToClient(true);

        TimelineEntryDTO result = service.createNote("TX-12345678", note, "BROKER1");

        assertThat(result.getTitle()).isEqualTo("New Note");
        assertThat(result.getNote()).isEqualTo("Note message");
        assertThat(result.getVisibleToClient()).isTrue();
        verify(repo).save(any(Transaction.class));
    }

    @Test
    void createNote_missingActorId_throwsInvalidInputException() {
        NoteRequestDTO note = new NoteRequestDTO();
        note.setTitle("Title");
        note.setMessage("Message");

        assertThatThrownBy(() -> service.createNote("TX-12345678", note, "BROKER1"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("actorId");
    }

    @Test
    void createNote_missingTitle_throwsInvalidInputException() {
        NoteRequestDTO note = new NoteRequestDTO();
        note.setActorId("BROKER1");
        note.setMessage("Message");

        assertThatThrownBy(() -> service.createNote("TX-12345678", note, "BROKER1"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("title");
    }

    @Test
    void createNote_missingMessage_throwsInvalidInputException() {
        NoteRequestDTO note = new NoteRequestDTO();
        note.setActorId("BROKER1");
        note.setTitle("Title");

        assertThatThrownBy(() -> service.createNote("TX-12345678", note, "BROKER1"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("message");
    }

    @Test
    void createNote_notOwner_throwsNotFoundException() {
        when(repo.findByTransactionId("TX-12345678")).thenReturn(Optional.of(sampleTransaction));

        NoteRequestDTO note = new NoteRequestDTO();
        note.setActorId("BROKER1");
        note.setTitle("Title");
        note.setMessage("Message");

        assertThatThrownBy(() -> service.createNote("TX-12345678", note, "OTHER_BROKER"))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== getBrokerTransactions tests ====================

    @Test
    void getBrokerTransactions_success_returnsList() {
        when(repo.findAllByBrokerId("BROKER1")).thenReturn(List.of(sampleTransaction));

        List<TransactionResponseDTO> result = service.getBrokerTransactions("BROKER1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo("CLIENT1");
    }

    @Test
    void getBrokerTransactions_empty_returnsEmptyList() {
        when(repo.findAllByBrokerId("NEW_BROKER")).thenReturn(List.of());

        List<TransactionResponseDTO> result = service.getBrokerTransactions("NEW_BROKER");

        assertThat(result).isEmpty();
    }

    // ==================== getByTransactionId tests ====================

    @Test
    void getByTransactionId_success_returnsTransaction() {
        when(repo.findByTransactionId("TX-12345678")).thenReturn(Optional.of(sampleTransaction));

        TransactionResponseDTO result = service.getByTransactionId("TX-12345678", "BROKER1");

        assertThat(result.getClientId()).isEqualTo("CLIENT1");
        assertThat(result.getBrokerId()).isEqualTo("BROKER1");
    }

    @Test
    void getByTransactionId_notFound_throwsNotFoundException() {
        when(repo.findByTransactionId("TX-UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByTransactionId("TX-UNKNOWN", "BROKER1"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByTransactionId_notOwner_throwsNotFoundException() {
        when(repo.findByTransactionId("TX-12345678")).thenReturn(Optional.of(sampleTransaction));

        assertThatThrownBy(() -> service.getByTransactionId("TX-12345678", "OTHER_BROKER"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("do not have access");
    }
}

