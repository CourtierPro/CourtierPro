package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.exceptions.DuplicateTransactionException;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionServiceImpl.
 * Tests business logic for creating transactions, managing notes, and accessing transaction data.
 * Uses mocked TransactionRepository to isolate business layer logic.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setup() {
        // Explicitly close mocks if open, though usually not needed with Extension, 
        // but explicit open helps if Extension context is weird.
        // Actually, just relying on constructor injection should be enough if fields are mocked.
        // But let's try just fixing the logic first.
        transactionService = new TransactionServiceImpl(transactionRepository, userAccountRepository);
        lenient().when(userAccountRepository.findByAuth0UserId(any())).thenReturn(Optional.empty());
    }

    // ========== createTransaction Tests ==========

    @Test
    void createTransaction_withValidBuyerSideData_createsTransaction() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                anyString(), anyString(), any()
        )).thenReturn(Optional.empty());

        Transaction expectedTx = new Transaction();
        expectedTx.setTransactionId("TX-12345678");
        expectedTx.setClientId(dto.getClientId());
        expectedTx.setBrokerId(dto.getBrokerId());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTx);

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo(dto.getClientId());
        assertThat(result.getBrokerId()).isEqualTo(dto.getBrokerId());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_withValidSellerSideData_createsTransaction() {
        // Arrange
        TransactionRequestDTO dto = createValidSellerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                anyString(), anyString(), any()
        )).thenReturn(Optional.empty());

        Transaction expectedTx = new Transaction();
        expectedTx.setTransactionId("TX-seller123");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTx);

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_withMissingClientId_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setClientId(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("clientId is required");
    }

    @Test
    void createTransaction_withMissingBrokerId_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setBrokerId(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("brokerId is required");
    }

    @Test
    void createTransaction_withMissingSide_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setSide(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("side is required");
    }

    @Test
    void createTransaction_withMissingPropertyStreet_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.getPropertyAddress().setStreet(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("propertyAddress.street is required");
    }

    @Test
    void createTransaction_withMissingInitialStage_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setInitialStage(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("initialStage is required");
    }

    @Test
    void createTransaction_withInvalidBuyerStage_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setInitialStage("INVALID_STAGE");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("not a valid buyer stage");
    }

    @Test
    void createTransaction_withInvalidSellerStage_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidSellerTransactionDTO();
        dto.setInitialStage("INVALID_STAGE");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("not a valid seller stage");
    }

    @Test
    void createTransaction_withDuplicateActiveTransaction_throwsInvalidInputException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        Transaction existingTx = new Transaction();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                dto.getClientId(), dto.getPropertyAddress().getStreet(), TransactionStatus.ACTIVE
        )).thenReturn(Optional.of(existingTx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void createTransaction_setsTransactionIdUUID() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                anyString(), anyString(), any()
        )).thenReturn(Optional.empty());

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId("TX-abcd1234");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result.getTransactionId()).startsWith("TX-");
    }

    // ========== getNotes Tests ==========

    @Test
    void getNotes_withValidTransactionAndBrokerId_returnsNotes() {
        // Arrange
        String transactionId = "TX-123";
        String brokerId = "broker-1";
        
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        
        TimelineEntry note = TimelineEntry.builder()
                .type(TimelineEntryType.NOTE)
                .title("Test Note")
                .note("Test message")
                .build();
        tx.setTimeline(List.of(note));

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        var result = transactionService.getNotes(transactionId, brokerId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Note");
    }

    @Test
    void getNotes_withWrongBrokerId_throwsNotFoundException() {
        // Arrange
        String transactionId = "TX-123";
        String brokerId = "wrong-broker";
        
        Transaction tx = new Transaction();
        tx.setBrokerId("correct-broker");
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getNotes(transactionId, brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("You do not have access");
    }

    @Test
    void getNotes_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getNotes("TX-999", "broker-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void getNotes_withNullTimeline_returnsEmptyList() {
        // Arrange
        String transactionId = "TX-123";
        String brokerId = "broker-1";
        
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setTimeline(null);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        var result = transactionService.getNotes(transactionId, brokerId);

        // Assert
        assertThat(result).isEmpty();
    }

    // ========== createNote Tests ==========

    @Test
    void createNote_withValidData_createsNote() {
        // Arrange
        String transactionId = "TX-123";
        String brokerId = "broker-1";
        
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId("actor-1");
        noteDTO.setTitle("New Note");
        noteDTO.setMessage("Note content");
        noteDTO.setVisibleToClient(true);

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setTimeline(new ArrayList<>());

        Transaction savedTx = new Transaction();
        savedTx.setTimeline(new ArrayList<>());
        TimelineEntry savedNote = TimelineEntry.builder()
                .type(TimelineEntryType.NOTE)
                .title("New Note")
                .note("Note content")
                .build();
        savedTx.getTimeline().add(savedNote);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        // Act
        var result = transactionService.createNote(transactionId, noteDTO, brokerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Note");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createNote_withMissingActorId_throwsInvalidInputException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId(null);
        noteDTO.setTitle("Note");
        noteDTO.setMessage("Message");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote("TX-123", noteDTO, "broker-1"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("actorId is required");
    }

    @Test
    void createNote_withMissingTitle_throwsInvalidInputException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId("actor-1");
        noteDTO.setTitle(null);
        noteDTO.setMessage("Message");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote("TX-123", noteDTO, "broker-1"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("title is required");
    }

    @Test
    void createNote_withMissingMessage_throwsInvalidInputException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId("actor-1");
        noteDTO.setTitle("Title");
        noteDTO.setMessage(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote("TX-123", noteDTO, "broker-1"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("message is required");
    }

    @Test
    void createNote_withWrongBrokerId_throwsNotFoundException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId("actor-1");
        noteDTO.setTitle("Title");
        noteDTO.setMessage("Message");

        Transaction tx = new Transaction();
        tx.setBrokerId("different-broker");
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote("TX-123", noteDTO, "broker-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("You do not have access");
    }

    // ========== getBrokerTransactions Tests ==========

    @Test
    void getBrokerTransactions_withValidBrokerId_returnsList() {
        // Arrange
        String brokerId = "broker-1";
        List<Transaction> transactions = List.of(new Transaction(), new Transaction());
        when(transactionRepository.findAllByFilters(eq(brokerId), any(), any(), any())).thenReturn(transactions);

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerId, null, null, null);

        // Assert
        assertThat(result).hasSize(2);
        verify(transactionRepository).findAllByFilters(eq(brokerId), any(), any(), any());
    }

    @Test
    void getBrokerTransactions_withNoBrokerTransactions_returnsEmptyList() {
        // Arrange
        when(transactionRepository.findAllByFilters(anyString(), any(), any(), any())).thenReturn(List.of());

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerTransactions("broker-1", null, null, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getBrokerTransactions_withFilters_passesCorrectFilters() {
        // Arrange
        String brokerId = "broker-1";
        
        // Act
        transactionService.getBrokerTransactions(brokerId, "ACTIVE", "BUY", "BUYER_PREQUALIFY_FINANCIALLY");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerId), eq(TransactionStatus.ACTIVE), any(), any());
    }

    @Test
    void getBrokerTransactions_withInvalidStatus_passesNull() {
        // Arrange
        String brokerId = "broker-1";

        // Act
        transactionService.getBrokerTransactions(brokerId, "INVALID_STATUS", null, null);

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerId), isNull(), isNull(), isNull());
    }
    
    @Test
    void getBrokerTransactions_withSellSideFilter_passesCorrectFilters() {
        // Arrange
        String brokerId = "broker-1";

        // Act
        transactionService.getBrokerTransactions(brokerId, null, "sell", "SELLER_INITIAL_CONSULTATION");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerId), isNull(), any(), any());
    }
    
    @Test
    void getBrokerTransactions_withInvalidFilters_passesNulls() {
        // Arrange
        String brokerId = "broker-1";

        // Act
        transactionService.getBrokerTransactions(brokerId, "invalid", "invalid", "invalid");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerId), isNull(), isNull(), isNull());
    }

    // ========== getByTransactionId Tests ==========

    @Test
    void getByTransactionId_withValidTransactionAndBrokerId_returnsTransaction() {
        // Arrange
        String transactionId = "TX-123";
        String brokerId = "broker-1";
        
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setClientId("client-1");
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        TransactionResponseDTO result = transactionService.getByTransactionId(transactionId, brokerId);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void getByTransactionId_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getByTransactionId("TX-999", "broker-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void getByTransactionId_withWrongBrokerId_throwsNotFoundException() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setBrokerId("different-broker");
        tx.setClientId("client-1");
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getByTransactionId("TX-123", "broker-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("You do not have access");
    }

    // ========== Helper Methods ==========

    private TransactionRequestDTO createValidBuyerTransactionDTO() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("client-1");
        dto.setBrokerId("broker-1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");
        
        var address = new com.example.courtierprobackend.transactions.datalayer.PropertyAddress();
        address.setStreet("123 Main St");
        dto.setPropertyAddress(address);
        
        return dto;
    }

    private TransactionRequestDTO createValidSellerTransactionDTO() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("client-2");
        dto.setBrokerId("broker-1");
        dto.setSide(TransactionSide.SELL_SIDE);
        dto.setInitialStage("SELLER_INITIAL_CONSULTATION");
        
        var address = new com.example.courtierprobackend.transactions.datalayer.PropertyAddress();
        address.setStreet("456 Oak Ave");
        dto.setPropertyAddress(address);
        
        return dto;
    }
}
