package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.PinnedTransactionRepository;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for TransactionServiceImpl.
 * Tests business logic for creating transactions, managing notes, and accessing
 * transaction data.
 * Uses mocked TransactionRepository to isolate business layer logic.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PinnedTransactionRepository pinnedTransactionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private com.example.courtierprobackend.email.EmailService emailService;

    @Mock
    private com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setup() {
        transactionService = new TransactionServiceImpl(transactionRepository, pinnedTransactionRepository, userAccountRepository, emailService,
                notificationService);
        lenient().when(userAccountRepository.findByAuth0UserId(any())).thenReturn(Optional.empty());
    }

    // ========== createTransaction Tests ==========

    @Test
    void createTransaction_withValidBuyerSideData_createsTransaction() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

        Transaction expectedTx = new Transaction();
        expectedTx.setTransactionId(UUID.randomUUID());
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
                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

        Transaction expectedTx = new Transaction();
        expectedTx.setTransactionId(UUID.randomUUID());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTx);

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_withMissingClientId_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setClientId(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("clientId is required");
    }

    @Test
    void createTransaction_withMissingBrokerId_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setBrokerId(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("brokerId is required");
    }

    @Test
    void createTransaction_withMissingSide_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setSide(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("side is required");
    }

    @Test
    void createTransaction_withMissingPropertyStreet_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.getPropertyAddress().setStreet(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("propertyAddress.street is required");
    }

    @Test
    void createTransaction_withMissingInitialStage_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setInitialStage(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("initialStage is required");
    }

    @Test
    void createTransaction_withInvalidBuyerStage_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        dto.setInitialStage("INVALID_STAGE");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid buyer stage");
    }

    @Test
    void createTransaction_withInvalidSellerStage_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidSellerTransactionDTO();
        dto.setInitialStage("INVALID_STAGE");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid seller stage");
    }

    @Test
    void createTransaction_withDuplicateActiveTransaction_throwsBadRequestException() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        Transaction existingTx = new Transaction();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                dto.getClientId(), dto.getPropertyAddress().getStreet(), TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(existingTx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void createTransaction_setsTransactionIdUUID() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(UUID.randomUUID());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result.getTransactionId()).isNotNull();
    }

    // ========== getNotes Tests ==========

    @Test
    void getNotes_withValidTransactionAndBrokerId_returnsNotes() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);

        TimelineEntry note = TimelineEntry.builder()
                .type(TimelineEntryType.NOTE)
                .title("Test Note")
                .note("Test message")
                .build();
        tx.setTimeline(List.of(note));

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        var result = transactionService.getNotes(transactionId, brokerUuid);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Note");
    }

    @Test
    void getNotes_withWrongBrokerId_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setBrokerId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getNotes(transactionId, brokerUuid))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have access");
    }

    @Test
    void getNotes_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        when(transactionRepository.findByTransactionId(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getNotes(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void getNotes_withNullTimeline_returnsEmptyList() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setTimeline(null);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        var result = transactionService.getNotes(transactionId, brokerUuid);

        // Assert
        assertThat(result).isEmpty();
    }

    // ========== createNote Tests ==========

    @Test
    void createNote_withValidData_createsNote() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId(UUID.randomUUID());
        noteDTO.setTitle("New Note");
        noteDTO.setMessage("Note content");
        noteDTO.setVisibleToClient(true);

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
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
        var result = transactionService.createNote(transactionId, noteDTO, brokerUuid);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Note");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createNote_withMissingActorId_throwsBadRequestException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId(null);
        noteDTO.setTitle("Note");
        noteDTO.setMessage("Message");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote(UUID.randomUUID(), noteDTO, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("actorId is required");
    }

    @Test
    void createNote_withMissingTitle_throwsBadRequestException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId(UUID.randomUUID());
        noteDTO.setTitle(null);
        noteDTO.setMessage("Message");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote(UUID.randomUUID(), noteDTO, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("title is required");
    }

    @Test
    void createNote_withMissingMessage_throwsBadRequestException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId(UUID.randomUUID());
        noteDTO.setTitle("Title");
        noteDTO.setMessage(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote(UUID.randomUUID(), noteDTO, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("message is required");
    }

    @Test
    void createNote_withWrongBrokerId_throwsForbiddenException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId(UUID.randomUUID());
        noteDTO.setTitle("Title");
        noteDTO.setMessage("Message");

        Transaction tx = new Transaction();
        tx.setBrokerId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(any(UUID.class))).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createNote(UUID.randomUUID(), noteDTO, UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have access");
    }

    // ========== getBrokerTransactions Tests ==========

    @Test
    void getBrokerTransactions_withValidBrokerId_returnsList() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        List<Transaction> transactions = List.of(new Transaction(), new Transaction());
        when(transactionRepository.findAllByFilters(eq(brokerUuid), any(), any(), any())).thenReturn(transactions);

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerUuid, null, null, null);

        // Assert
        assertThat(result).hasSize(2);
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), any(), any(), any());
    }

    @Test
    void getBrokerTransactions_withNoBrokerTransactions_returnsEmptyList() {
        // Arrange
        when(transactionRepository.findAllByFilters(any(UUID.class), any(), any(), any())).thenReturn(List.of());

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(UUID.randomUUID(), null, null,
                null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getBrokerTransactions_withFilters_passesCorrectFilters() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();

        // Act
        transactionService.getBrokerTransactions(brokerUuid, "ACTIVE", "BUY", "BUYER_PREQUALIFY_FINANCIALLY");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), eq(TransactionStatus.ACTIVE), any(), any());
    }

    @Test
    void getBrokerTransactions_withInvalidStatus_passesNull() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();

        // Act
        transactionService.getBrokerTransactions(brokerUuid, "INVALID_STATUS", null, null);

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), isNull(), isNull());
    }

    @Test
    void getBrokerTransactions_withSellSideFilter_passesCorrectFilters() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();

        // Act
        transactionService.getBrokerTransactions(brokerUuid, null, "sell", "SELLER_INITIAL_CONSULTATION");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), any(), any());
    }

    @Test
    void getBrokerTransactions_withInvalidFilters_passesNulls() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();

        // Act
        transactionService.getBrokerTransactions(brokerUuid, "invalid", "invalid", "invalid");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), isNull(), isNull());
    }

    // ========== getByTransactionId Tests ==========

    @Test
    void getByTransactionId_withValidTransactionAndBrokerId_returnsTransaction() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        TransactionResponseDTO result = transactionService.getByTransactionId(transactionId, brokerUuid);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void getByTransactionId_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        when(transactionRepository.findByTransactionId(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getByTransactionId(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void getByTransactionId_withWrongBrokerId_throwsForbiddenException() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setBrokerId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(any(UUID.class))).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getByTransactionId(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have access");
    }

    // ========== updateTransactionStage Tests ==========

    @Test
    void updateTransactionStage_BuySide_Success() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setTimeline(new ArrayList<>());
        tx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());
        tx.getPropertyAddress().setStreet("123 Main St");

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setBrokerId(brokerUuid);
        savedTx.setSide(TransactionSide.BUY_SIDE);
        savedTx.setBuyerStage(BuyerStage.BUYER_OFFER_ACCEPTED);
        savedTx.setTimeline(new ArrayList<>());
        savedTx.setPropertyAddress(tx.getPropertyAddress());

        // timeline entry will be the last element
        savedTx.getTimeline().add(TimelineEntry.builder().type(TimelineEntryType.STAGE_CHANGE)
                .title("Stage updated to BUYER_OFFER_ACCEPTED").note("note").visibleToClient(true).build());

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");
        dto.setNote("note");

        // Act
        TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto, brokerUuid);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCurrentStage()).isEqualTo("BUYER_OFFER_ACCEPTED");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransactionStage_shouldSendEmailAndNotification_whenSuccessful_English() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        // Mock Transaction
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setClientId(clientId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);

        com.example.courtierprobackend.transactions.datalayer.PropertyAddress addr = new com.example.courtierprobackend.transactions.datalayer.PropertyAddress();
        addr.setStreet("123 Test St");
        tx.setPropertyAddress(addr);

        // Saved Transaction (after update)
        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setClientId(clientId);
        savedTx.setBrokerId(brokerId);
        savedTx.setSide(TransactionSide.BUY_SIDE);
        savedTx.setBuyerStage(BuyerStage.BUYER_OFFER_ACCEPTED);
        savedTx.setPropertyAddress(addr);
        savedTx.setTimeline(new ArrayList<>());

        // Mock UserAccounts
        var client = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        client.setId(clientId);
        client.setFirstName("Client");
        client.setLastName("User");
        client.setEmail("client@example.com");
        client.setPreferredLanguage("en");

        var broker = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        broker.setId(brokerId);
        broker.setFirstName("Broker");
        broker.setLastName("Agent");

        // Repositories
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
        when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");

        // Act
        transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        // 1. Verify Email sent
        verify(emailService, times(1)).sendStageUpdateEmail(
                eq("client@example.com"),
                eq("Client User"),
                eq("Broker Agent"),
                eq("123 Test St"),
                eq("BUYER_OFFER_ACCEPTED"),
                eq("en"));

        // 2. Verify In-App Notification created with localized message
        // Stage: BUYER_OFFER_ACCEPTED -> Buyer Offer Accepted
        verify(notificationService, times(1)).createNotification(
                eq(clientId.toString()), // Internal UUID
                eq("Stage Update"),
                eq("Stage updated to Buyer Offer Accepted by Broker Agent for 123 Test St"),
                eq(transactionId.toString()));
    }

    @Test
    void updateTransactionStage_shouldSendEmailAndNotification_whenSuccessful_French() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setClientId(clientId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        com.example.courtierprobackend.transactions.datalayer.PropertyAddress addr = new com.example.courtierprobackend.transactions.datalayer.PropertyAddress();
        addr.setStreet("123 Rue Test");
        tx.setPropertyAddress(addr);

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setClientId(clientId);
        savedTx.setBrokerId(brokerId);
        savedTx.setSide(TransactionSide.BUY_SIDE);
        savedTx.setBuyerStage(BuyerStage.BUYER_OFFER_ACCEPTED);
        savedTx.setPropertyAddress(addr);
        savedTx.setTimeline(new ArrayList<>());

        var client = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        client.setId(clientId);
        client.setFirstName("Client");
        client.setLastName("User");
        client.setPreferredLanguage("fr");
        client.setEmail("albert@example.com");

        var broker = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        broker.setId(brokerId);
        broker.setFirstName("Courtier");
        broker.setLastName("Pro");

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
        when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");

        // Act
        transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        verify(emailService, times(1)).sendStageUpdateEmail(
                eq("albert@example.com"),
                eq("Client User"),
                eq("Courtier Pro"),
                eq("123 Rue Test"),
                eq("BUYER_OFFER_ACCEPTED"),
                eq("fr"));

        // Verify French message: "Le stade a été mis à jour à Offre Acceptée par
        // Courtier Pro pour 123 Rue Test"
        verify(notificationService, times(1)).createNotification(
                eq(clientId.toString()),
                eq("Stage Update"),
                eq("Le stade a été mis à jour à Offre Acceptée par Courtier Pro pour 123 Rue Test"),
                eq(transactionId.toString()));
    }

    @Test
    void updateTransactionStage_withNullPropertyAddress_handlesGracefully() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setClientId(clientId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setPropertyAddress(null); // NULL ADDRESS

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setClientId(clientId);
        savedTx.setBrokerId(brokerId);
        savedTx.setSide(TransactionSide.BUY_SIDE);
        savedTx.setBuyerStage(BuyerStage.BUYER_OFFER_ACCEPTED);
        savedTx.setPropertyAddress(null); // NULL ADDRESS
        savedTx.setTimeline(new ArrayList<>());

        var client = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        client.setId(clientId);
        client.setFirstName("Client");
        client.setLastName("User");
        client.setPreferredLanguage("en");

        var broker = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        broker.setId(brokerId);
        broker.setFirstName("Broker");
        broker.setLastName("Agent");

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
        when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");

        // Act
        transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        // Should use "Unknown Address" default
        verify(notificationService, times(1)).createNotification(
                eq(clientId.toString()),
                eq("Stage Update"),
                contains("for Unknown Address"),
                eq(transactionId.toString()));
    }

    @Test
    void updateTransactionStage_withNotificationFailure_doesNotRollbackTransaction() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setClientId(clientId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setClientId(clientId);
        savedTx.setBrokerId(brokerId);
        savedTx.setSide(TransactionSide.BUY_SIDE);
        savedTx.setBuyerStage(BuyerStage.BUYER_OFFER_ACCEPTED);
        savedTx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());
        savedTx.setTimeline(new ArrayList<>());

        // Users exist
        when(userAccountRepository.findById(any()))
                .thenReturn(Optional.of(new com.example.courtierprobackend.user.dataaccesslayer.UserAccount()));

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        // Make email service throw exception
        doThrow(new RuntimeException("Email server down")).when(emailService).sendStageUpdateEmail(any(), any(), any(),
                any(), any(), any());

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");

        // Act
        TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        // Transaction should still be returned successfully
        assertThat(response).isNotNull();
        // Repository save WAS called
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransactionStage_SellSide_Success() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setSide(TransactionSide.SELL_SIDE);
        tx.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);
        tx.setTimeline(new ArrayList<>());
        tx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setBrokerId(brokerUuid);
        savedTx.setSide(TransactionSide.SELL_SIDE);
        savedTx.setSellerStage(SellerStage.SELLER_REVIEW_OFFERS);
        savedTx.setTimeline(new ArrayList<>());
        savedTx.setPropertyAddress(tx.getPropertyAddress());
        savedTx.getTimeline().add(TimelineEntry.builder().type(TimelineEntryType.STAGE_CHANGE)
                .title("Stage updated to SELLER_REVIEW_OFFERS").note("note").visibleToClient(true).build());

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("SELLER_REVIEW_OFFERS");
        dto.setNote("note");

        // Act
        TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto, brokerUuid);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCurrentStage()).isEqualTo("SELLER_REVIEW_OFFERS");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransactionStage_WrongBroker_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(UUID.randomUUID()); // Different broker
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerUuid))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have access");
    }

    @Test
    void updateTransactionStage_InvalidStageEnum_throwsBadRequestException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setSide(TransactionSide.BUY_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("INVALID_STAGE");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerUuid))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid");
    }

    @Test
    void updateTransactionStage_TimelineEntryVerified() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        String customNote = "Custom timeline note";

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setTimeline(new ArrayList<>());
        tx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setBrokerId(brokerUuid);
        savedTx.setSide(TransactionSide.BUY_SIDE);
        savedTx.setBuyerStage(BuyerStage.BUYER_FINANCING_FINALIZED);
        savedTx.setTimeline(new ArrayList<>());
        savedTx.setPropertyAddress(tx.getPropertyAddress());
        // savedTx will be returned by repository.save

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_FINANCING_FINALIZED");
        dto.setNote(customNote);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // Act
        TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto, brokerUuid);

        // Assert response
        assertThat(response).isNotNull();
        assertThat(response.getCurrentStage()).isEqualTo("BUYER_FINANCING_FINALIZED");

        // Verify repository.save called and capture the saved transaction
        verify(transactionRepository).save(captor.capture());
        Transaction captured = captor.getValue();

        assertThat(captured.getTimeline()).isNotNull();
        assertThat(captured.getTimeline()).isNotEmpty();

        TimelineEntry last = captured.getTimeline().get(captured.getTimeline().size() - 1);
        assertThat(last.getType()).isEqualTo(TimelineEntryType.STAGE_CHANGE);
        assertThat(last.getVisibleToClient()).isTrue();
        assertThat(last.getTitle()).contains("BUYER_FINANCING_FINALIZED");
        assertThat(last.getNote()).isEqualTo(customNote);
    }

    // ========== Helper Methods ==========

    private TransactionRequestDTO createValidBuyerTransactionDTO() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setBrokerId(UUID.randomUUID());
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        var address = new com.example.courtierprobackend.transactions.datalayer.PropertyAddress();
        address.setStreet("123 Main St");
        dto.setPropertyAddress(address);

        return dto;
    }

    private TransactionRequestDTO createValidSellerTransactionDTO() {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setBrokerId(UUID.randomUUID());
        dto.setSide(TransactionSide.SELL_SIDE);
        dto.setInitialStage("SELLER_INITIAL_CONSULTATION");

        var address = new com.example.courtierprobackend.transactions.datalayer.PropertyAddress();
        address.setStreet("456 Oak Ave");
        dto.setPropertyAddress(address);

        return dto;
    }
}
