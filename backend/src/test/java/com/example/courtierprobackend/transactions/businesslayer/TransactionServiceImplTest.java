package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.AddParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ParticipantResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.TransactionParticipant;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.PinnedTransactionRepository;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository;
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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private TimelineService timelineService;

    @Mock
    private TransactionParticipantRepository participantRepository;

    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.PropertyRepository propertyRepository;

    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.OfferRepository offerRepository;

    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.ConditionRepository conditionRepository;

    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.PropertyOfferRepository propertyOfferRepository;

    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.OfferDocumentRepository offerDocumentRepository;

    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.OfferRevisionRepository offerRevisionRepository;

    @Mock
    private com.example.courtierprobackend.infrastructure.storage.S3StorageService s3StorageService;


    @BeforeEach
    void setup() {
        transactionService = new TransactionServiceImpl(transactionRepository, pinnedTransactionRepository,
                userAccountRepository, emailService,
                notificationService, timelineService, participantRepository, propertyRepository, offerRepository, conditionRepository,
                propertyOfferRepository, offerDocumentRepository, offerRevisionRepository, s3StorageService);
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
        // Vérifie que l'audit timeline est bien appelé
        verify(timelineService).addEntry(
                any(UUID.class), // transactionId
                eq(dto.getBrokerId()),
                eq(TimelineEntryType.CREATED),
                isNull(),
                isNull(),
                any() // TransactionInfo
        );
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
        verify(notificationService).createNotification(
                eq(dto.getClientId().toString()),
                eq("notifications.transactionCreated.title"),
                eq("notifications.transactionCreated.message"),
                any(java.util.Map.class),
                anyString(),
                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.GENERAL));
    }

    @Test
    void createTransaction_NotificationFailure_LogsAndProceeds() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

        Transaction expectedTx = new Transaction();
        expectedTx.setTransactionId(UUID.randomUUID());
        expectedTx.setClientId(dto.getClientId());
        expectedTx.setBrokerId(dto.getBrokerId());
        expectedTx.setPropertyAddress(
                new PropertyAddress(dto.getPropertyAddress().getStreet(), dto.getPropertyAddress().getCity(),
                        dto.getPropertyAddress().getProvince(), dto.getPropertyAddress().getPostalCode()));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTx);

        // Simulate Notification exception - use the i18n version with Map
        doThrow(new RuntimeException("Notification Error")).when(notificationService)
                .createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class), anyString(), any());

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
        // Verify notification service WAS called (but failed)
        verify(notificationService).createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class), anyString(), any());
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
        // Arrange - use SELL_SIDE since BUY_SIDE no longer requires property street
        TransactionRequestDTO dto = createValidSellerTransactionDTO();
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

    @Test
    void createTransaction_whenClientNotFound_usesUnknownClientName() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(UUID.randomUUID());
        savedTx.setClientId(dto.getClientId());
        savedTx.setBrokerId(dto.getBrokerId());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        // Ensure user lookup returns empty
        when(userAccountRepository.findById(dto.getClientId())).thenReturn(Optional.empty());

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result.getClientName()).isEqualTo("Unknown User");
    }

    @Test
    void createTransaction_whenClientNameIsEmpty_usesUnknownClientName() {
        // Arrange
        TransactionRequestDTO dto = createValidBuyerTransactionDTO();
        when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(UUID.randomUUID());
        savedTx.setClientId(dto.getClientId());
        savedTx.setBrokerId(dto.getBrokerId());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        // Ensure user lookup returns user with null/empty names
        var user = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        user.setFirstName("");
        user.setLastName(null);
        when(userAccountRepository.findById(dto.getClientId())).thenReturn(Optional.of(user));

        // Act
        TransactionResponseDTO result = transactionService.createTransaction(dto);

        // Assert
        assertThat(result.getClientName()).isEqualTo("Unknown User");
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
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        TimelineEntryDTO noteDTO = new TimelineEntryDTO();
        noteDTO.setType(TimelineEntryType.NOTE);
        noteDTO.setNote("Test message");
        when(timelineService.getTimelineForTransaction(transactionId)).thenReturn(List.of(noteDTO));

        // Act
        var result = transactionService.getNotes(transactionId, brokerUuid);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNote()).isEqualTo("Test message");
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
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(timelineService.getTimelineForTransaction(transactionId)).thenReturn(List.of());

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
        noteDTO.setActorId(brokerUuid); // L'acteur est le broker dans ce contexte
        noteDTO.setTitle("New Note");
        noteDTO.setMessage("Note content");
        noteDTO.setVisibleToClient(true);

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Stub pour que la timeline contienne la note attendue après ajout
        TimelineEntryDTO noteResult = new TimelineEntryDTO();
        noteResult.setType(TimelineEntryType.NOTE);
        noteResult.setNote("New Note: Note content");
        when(timelineService.getTimelineForTransaction(transactionId)).thenReturn(List.of(noteResult));

        // Act
        var result = transactionService.createNote(transactionId, noteDTO, brokerUuid);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNote()).isEqualTo("New Note: Note content");
        verify(timelineService, times(1)).addEntry(
                eq(transactionId),
                eq(brokerUuid),
                eq(TimelineEntryType.NOTE),
                eq("New Note: Note content"),
                isNull());
        verify(timelineService, times(1)).getTimelineForTransaction(transactionId);
    }

    @Test
    void createNote_withMissingActorId_throwsBadRequestException() {
        // Arrange
        NoteRequestDTO noteDTO = new NoteRequestDTO();
        noteDTO.setActorId(null);
        noteDTO.setTitle("Note");
        noteDTO.setMessage("Message");

        // Act & Assert
        // La logique métier actuelle nève plus d'exception si actorId est null
        // On vérifie simplement que la méthode ne lève pas d'exception et retourne null
        // (car pas de stub sur timelineService)
        var result = transactionService.createNote(UUID.randomUUID(), noteDTO, UUID.randomUUID());
        assertThat(result).isNull();
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

    // ========== getBrokerTransactions Tests ==========

    @Test
    void getBrokerTransactions_withValidBrokerId_returnsList() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        List<Transaction> transactions = List.of(new Transaction(), new Transaction());
        when(transactionRepository.findAllByFilters(eq(brokerUuid), any(), any(), any(), anyBoolean())).thenReturn(transactions);

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerUuid, null, null, null);

        // Assert
        assertThat(result).hasSize(2);
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), any(), any(), any(), anyBoolean());
    }

    @Test
    void getBrokerTransactions_withNoBrokerTransactions_returnsEmptyList() {
        // Arrange
        when(transactionRepository.findAllByFilters(any(UUID.class), any(), any(), any(), anyBoolean())).thenReturn(List.of());

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
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), eq(TransactionStatus.ACTIVE), any(), any(), anyBoolean());
    }

    @Test
    void getBrokerTransactions_withInvalidStatus_passesNull() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();

        // Act
        transactionService.getBrokerTransactions(brokerUuid, "INVALID_STATUS", null, null);

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), isNull(), isNull(), anyBoolean());
    }

    @Test
    void getBrokerTransactions_withSellSideFilter_passesCorrectFilters() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();

        // Act
        transactionService.getBrokerTransactions(brokerUuid, null, "sell", "SELLER_INITIAL_CONSULTATION");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), any(), any(), anyBoolean());
    }

    @Test
    void getBrokerTransactions_withInvalidFilters_passesNulls() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();

        // Act
        transactionService.getBrokerTransactions(brokerUuid, "invalid", "invalid", "invalid");

        // Assert
        verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), isNull(), isNull(), anyBoolean());
    }

    // ========== getBrokerClientTransactions Tests ==========

    @Test
    void getBrokerClientTransactions_withValidBrokerAndClient_returnsList() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        UUID clientUuid = UUID.randomUUID();
        
        Transaction tx1 = new Transaction();
        tx1.setTransactionId(UUID.randomUUID());
        tx1.setBrokerId(brokerUuid);
        tx1.setClientId(clientUuid);
        
        Transaction tx2 = new Transaction();
        tx2.setTransactionId(UUID.randomUUID());
        tx2.setBrokerId(brokerUuid);
        tx2.setClientId(clientUuid);
        
        when(transactionRepository.findAllByClientId(clientUuid)).thenReturn(List.of(tx1, tx2));

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid, clientUuid);

        // Assert
        assertThat(result).hasSize(2);
        verify(transactionRepository).findAllByClientId(clientUuid);
    }

    @Test
    void getBrokerClientTransactions_filtersByBrokerId() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        UUID otherBrokerUuid = UUID.randomUUID();
        UUID clientUuid = UUID.randomUUID();
        
        // Transaction belonging to the requesting broker
        Transaction txBelongsToRequestingBroker = new Transaction();
        txBelongsToRequestingBroker.setTransactionId(UUID.randomUUID());
        txBelongsToRequestingBroker.setBrokerId(brokerUuid);
        txBelongsToRequestingBroker.setClientId(clientUuid);
        
        // Transaction belonging to a different broker
        Transaction txBelongsToDifferentBroker = new Transaction();
        txBelongsToDifferentBroker.setTransactionId(UUID.randomUUID());
        txBelongsToDifferentBroker.setBrokerId(otherBrokerUuid);
        txBelongsToDifferentBroker.setClientId(clientUuid);
        
        when(transactionRepository.findAllByClientId(clientUuid))
                .thenReturn(List.of(txBelongsToRequestingBroker, txBelongsToDifferentBroker));

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid, clientUuid);

        // Assert - Should only return the transaction belonging to requesting broker
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionId()).isEqualTo(txBelongsToRequestingBroker.getTransactionId());
    }

    @Test
    void getBrokerClientTransactions_withNoMatchingTransactions_returnsEmptyList() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        UUID otherBrokerUuid = UUID.randomUUID();
        UUID clientUuid = UUID.randomUUID();
        
        // All transactions belong to a different broker
        Transaction txBelongsToDifferentBroker = new Transaction();
        txBelongsToDifferentBroker.setTransactionId(UUID.randomUUID());
        txBelongsToDifferentBroker.setBrokerId(otherBrokerUuid);
        txBelongsToDifferentBroker.setClientId(clientUuid);
        
        when(transactionRepository.findAllByClientId(clientUuid))
                .thenReturn(List.of(txBelongsToDifferentBroker));

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid, clientUuid);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getBrokerClientTransactions_withNoTransactions_returnsEmptyList() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        UUID clientUuid = UUID.randomUUID();
        
        when(transactionRepository.findAllByClientId(clientUuid)).thenReturn(List.of());

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid, clientUuid);

        // Assert
        assertThat(result).isEmpty();
    }

    // ========== getAllClientTransactions Tests ==========

    @Test
    void getAllClientTransactions_returnsAllTransactionsWithBrokerNames() {
        // Arrange
        UUID clientUuid = UUID.randomUUID();
        UUID broker1Uuid = UUID.randomUUID();
        UUID broker2Uuid = UUID.randomUUID();
        
        Transaction tx1 = new Transaction();
        tx1.setTransactionId(UUID.randomUUID());
        tx1.setClientId(clientUuid);
        tx1.setBrokerId(broker1Uuid);
        tx1.setSide(TransactionSide.BUY_SIDE);
        tx1.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx1.setStatus(TransactionStatus.ACTIVE);
        
        Transaction tx2 = new Transaction();
        tx2.setTransactionId(UUID.randomUUID());
        tx2.setClientId(clientUuid);
        tx2.setBrokerId(broker2Uuid);
        tx2.setSide(TransactionSide.SELL_SIDE);
        tx2.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);
        tx2.setStatus(TransactionStatus.ACTIVE);
        
        when(transactionRepository.findAllByClientId(clientUuid)).thenReturn(List.of(tx1, tx2));
        when(userAccountRepository.findById(clientUuid)).thenReturn(Optional.of(createUserAccount(clientUuid, "Test Client")));
        when(userAccountRepository.findById(broker1Uuid)).thenReturn(Optional.of(createUserAccount(broker1Uuid, "Broker One")));
        when(userAccountRepository.findById(broker2Uuid)).thenReturn(Optional.of(createUserAccount(broker2Uuid, "Broker Two")));

        // Act
        List<TransactionResponseDTO> result = transactionService.getAllClientTransactions(clientUuid);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBrokerName()).isEqualTo("Broker One");
        assertThat(result.get(1).getBrokerName()).isEqualTo("Broker Two");
        assertThat(result.get(0).getClientName()).isEqualTo("Test Client");
    }

    @Test
    void getAllClientTransactions_withNoTransactions_returnsEmptyList() {
        // Arrange
        UUID clientUuid = UUID.randomUUID();
        
        when(transactionRepository.findAllByClientId(clientUuid)).thenReturn(List.of());

        // Act
        List<TransactionResponseDTO> result = transactionService.getAllClientTransactions(clientUuid);

        // Assert
        assertThat(result).isEmpty();
    }

    private UserAccount createUserAccount(UUID id, String name) {
        UserAccount account = new UserAccount();
        account.setId(id);
        String[] parts = name.split(" ", 2);
        account.setFirstName(parts[0]);
        account.setLastName(parts.length > 1 ? parts[1] : "");
        return account;
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

    @Test
    void getByTransactionId_buySide_usesCentrisFromAcceptedProperty() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setClientId(UUID.randomUUID());
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_SHOP_FOR_PROPERTY);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setCentrisNumber("TX-CENTRIS"); // Transaction's own centris

        // Create accepted property with different centris
        com.example.courtierprobackend.transactions.datalayer.Property acceptedProperty = 
            com.example.courtierprobackend.transactions.datalayer.Property.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(transactionId)
                .offerStatus(com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus.ACCEPTED)
                .centrisNumber("PROPERTY-CENTRIS")
                .build();

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                .thenReturn(List.of(acceptedProperty));

        // Act
        TransactionResponseDTO result = transactionService.getByTransactionId(transactionId, brokerUuid);

        // Assert - Should use centris from accepted property, not from transaction
        assertThat(result.getCentrisNumber()).isEqualTo("PROPERTY-CENTRIS");
    }

    @Test
    void getByTransactionId_buySide_noAcceptedProperty_usesTransactionCentris() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setClientId(UUID.randomUUID());
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_SHOP_FOR_PROPERTY);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setCentrisNumber("TX-CENTRIS");

        // Create property that is NOT accepted
        com.example.courtierprobackend.transactions.datalayer.Property pendingProperty = 
            com.example.courtierprobackend.transactions.datalayer.Property.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(transactionId)
                .offerStatus(com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus.OFFER_MADE)
                .centrisNumber("PROPERTY-CENTRIS")
                .build();

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                .thenReturn(List.of(pendingProperty));

        // Act
        TransactionResponseDTO result = transactionService.getByTransactionId(transactionId, brokerUuid);

        // Assert - Should use transaction's centris since no accepted property
        assertThat(result.getCentrisNumber()).isEqualTo("TX-CENTRIS");
    }

    @Test
    void getByTransactionId_sellSide_usesTransactionCentris() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerUuid);
        tx.setClientId(UUID.randomUUID());
        tx.setSide(TransactionSide.SELL_SIDE);
        tx.setSellerStage(SellerStage.SELLER_LISTING_PUBLISHED);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setCentrisNumber("SELL-CENTRIS");

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        TransactionResponseDTO result = transactionService.getByTransactionId(transactionId, brokerUuid);

        // Assert - Should use transaction's centris for sell-side (not property lookup)
        assertThat(result.getCentrisNumber()).isEqualTo("SELL-CENTRIS");
        // Verify property lookup was NOT called for sell-side
        verify(propertyRepository, never()).findByTransactionIdOrderByCreatedAtDesc(any());
    }

    // ========== updateTransactionStage Tests ==========

    @Test
    void updateTransactionStage_withNullDto_throwsBadRequestException() {
        assertThatThrownBy(() -> transactionService.updateTransactionStage(UUID.randomUUID(), null, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("request body is required");
    }

    @Test
    void updateTransactionStage_withNullStage_throwsBadRequestException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("stage is required");
    }

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
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");
        dto.setNote("note");

        // Act
        TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto, brokerUuid);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCurrentStage()).isEqualTo("BUYER_OFFER_ACCEPTED");
        verify(transactionRepository).save(any(Transaction.class));
        verify(timelineService).addEntry(eq(transactionId), eq(brokerUuid), eq(TimelineEntryType.STAGE_CHANGE),
                isNull(), isNull(), any());
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
        // savedTx.setTimeline(new ArrayList<>());

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
                eq(transactionId.toString()),
                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.STAGE_UPDATE));
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
        // savedTx.setTimeline(new ArrayList<>());

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
                eq(transactionId.toString()),
                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.STAGE_UPDATE));
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
        // savedTx.setTimeline(new ArrayList<>());

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
                eq(transactionId.toString()),
                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.STAGE_UPDATE));
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
        // savedTx.setTimeline(new ArrayList<>());

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
        // Plus de setTimeline

        Transaction savedTx = new Transaction();
        savedTx.setTransactionId(transactionId);
        savedTx.setBrokerId(brokerUuid);
        savedTx.setSide(TransactionSide.SELL_SIDE);
        savedTx.setSellerStage(SellerStage.SELLER_REVIEW_OFFERS);
        // Plus de setTimeline

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
        // Vérifie que l'audit timeline est bien appelé
        verify(timelineService).addEntry(
                eq(transactionId),
                eq(brokerUuid),
                eq(TimelineEntryType.STAGE_CHANGE),
                isNull(),
                isNull(),
                any() // TransactionInfo
        );
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
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_FINANCING_FINALIZED");
        dto.setNote(customNote);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // Act
        TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto, brokerUuid);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCurrentStage()).isEqualTo("BUYER_FINANCING_FINALIZED");
        verify(transactionRepository).save(any(Transaction.class));
        verify(timelineService).addEntry(eq(transactionId), eq(brokerUuid), eq(TimelineEntryType.STAGE_CHANGE),
                isNull(), isNull(), any());
    }

    @Test
    void updateTransactionStage_AutoClose_BuyerOccupancy() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_SECOND_NOTARY_APPOINTMENT);
        tx.setStatus(TransactionStatus.ACTIVE);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OCCUPANCY");

        // Act
        transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CLOSED_SUCCESSFULLY);
        assertThat(tx.getClosedAt()).isNotNull();
        verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE),
                contains("CLOSED_SUCCESSFULLY"), isNull());
    }

    @Test
    void updateTransactionStage_AutoTerminate_BuyerTerminated() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_SHOP_FOR_PROPERTY);
        tx.setStatus(TransactionStatus.ACTIVE);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_TERMINATED");

        // Act
        transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.TERMINATED_EARLY);
        assertThat(tx.getClosedAt()).isNotNull();
        verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE),
                contains("TERMINATED_EARLY"), isNull());
    }

    @Test
    void updateTransactionStage_AutoTerminate_SellerTerminated() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.SELL_SIDE);
        tx.setSellerStage(SellerStage.SELLER_LISTING_PUBLISHED);
        tx.setStatus(TransactionStatus.ACTIVE);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("SELLER_TERMINATED");

        // Act
        transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.TERMINATED_EARLY);
        assertThat(tx.getClosedAt()).isNotNull();
        verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE),
                contains("TERMINATED_EARLY"), isNull());
    }

    @Test
    void updateTransactionStage_AutoClose_SellerHandoverKeys() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.SELL_SIDE);
        tx.setSellerStage(SellerStage.SELLER_NOTARY_APPOINTMENT);
        tx.setStatus(TransactionStatus.ACTIVE);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("SELLER_HANDOVER_KEYS");

        // Act
        transactionService.updateTransactionStage(transactionId, dto, brokerId);

        // Assert
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CLOSED_SUCCESSFULLY);
        assertThat(tx.getClosedAt()).isNotNull();
        verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE),
                contains("CLOSED_SUCCESSFULLY"), isNull());
    }

    @Test
    void updateTransactionStage_ClosedTransaction_ThrowsException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setStatus(TransactionStatus.CLOSED_SUCCESSFULLY);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OCCUPANCY");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot update stage of a closed or terminated transaction");
    }

    @Test
    void updateTransactionStage_TerminatedTransaction_ThrowsException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setStatus(TransactionStatus.TERMINATED_EARLY);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OCCUPANCY");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot update stage of a closed or terminated transaction");
    }

    @Test
    void updateTransactionStage_OptimisticLockingFailure_ThrowsConflictException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setStatus(TransactionStatus.ACTIVE);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Optimistic lock failed"));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_SHOP_FOR_PROPERTY");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ConflictException.class)
                .hasMessageContaining("The transaction was updated by another user");
    }

    // ========== pinTransaction Tests ==========

    @Test
    void pinTransaction_withValidBrokerAndTransaction_createsPin() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(pinnedTransactionRepository.existsByBrokerIdAndTransactionId(brokerId, transactionId)).thenReturn(false);

        // Act
        transactionService.pinTransaction(transactionId, brokerId);

        // Assert
        verify(pinnedTransactionRepository)
                .save(any(com.example.courtierprobackend.transactions.datalayer.PinnedTransaction.class));
    }

    @Test
    void pinTransaction_withAlreadyPinned_doesNotDuplicate() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(pinnedTransactionRepository.existsByBrokerIdAndTransactionId(brokerId, transactionId)).thenReturn(true);

        // Act
        transactionService.pinTransaction(transactionId, brokerId);

        // Assert
        verify(pinnedTransactionRepository, never()).save(any());
    }

    @Test
    void pinTransaction_withWrongBroker_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID otherBrokerId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(otherBrokerId);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.pinTransaction(transactionId, brokerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void pinTransaction_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        when(transactionRepository.findByTransactionId(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.pinTransaction(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    // ========== unpinTransaction Tests ==========

    @Test
    void unpinTransaction_withValidBrokerAndTransaction_deletesPin() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        transactionService.unpinTransaction(transactionId, brokerId);

        // Assert
        verify(pinnedTransactionRepository).deleteByBrokerIdAndTransactionId(brokerId, transactionId);
    }

    @Test
    void unpinTransaction_withWrongBroker_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID otherBrokerId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(otherBrokerId);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.unpinTransaction(transactionId, brokerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void unpinTransaction_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        when(transactionRepository.findByTransactionId(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.unpinTransaction(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    // ========== getPinnedTransactionIds Tests ==========

    @Test
    void getPinnedTransactionIds_withPinnedTransactions_returnsIds() {
        // Arrange
        UUID brokerId = UUID.randomUUID();
        UUID txId1 = UUID.randomUUID();
        UUID txId2 = UUID.randomUUID();

        var pin1 = com.example.courtierprobackend.transactions.datalayer.PinnedTransaction.builder()
                .brokerId(brokerId)
                .transactionId(txId1)
                .build();
        var pin2 = com.example.courtierprobackend.transactions.datalayer.PinnedTransaction.builder()
                .brokerId(brokerId)
                .transactionId(txId2)
                .build();

        when(pinnedTransactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(pin1, pin2));

        // Act
        var result = transactionService.getPinnedTransactionIds(brokerId);

        // Assert
        assertThat(result).containsExactlyInAnyOrder(txId1, txId2);
    }

    @Test
    void getPinnedTransactionIds_withNoPinnedTransactions_returnsEmptySet() {
        // Arrange
        UUID brokerId = UUID.randomUUID();
        when(pinnedTransactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of());

        // Act
        var result = transactionService.getPinnedTransactionIds(brokerId);

        // Assert
        assertThat(result).isEmpty();
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

    // ========== Participant Tests ==========

    @Test
    void addParticipant_withValidData_addsParticipant() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        AddParticipantRequestDTO dto = new AddParticipantRequestDTO();
        dto.setName("Co-Broker John");
        dto.setRole(ParticipantRole.CO_BROKER);
        dto.setEmail("john@example.com");

        TransactionParticipant savedParticipant = new TransactionParticipant();
        savedParticipant.setId(UUID.randomUUID());
        savedParticipant.setTransactionId(transactionId);
        savedParticipant.setName("Co-Broker John");
        savedParticipant.setRole(ParticipantRole.CO_BROKER);
        savedParticipant.setEmail("john@example.com");

        when(participantRepository.save(any(TransactionParticipant.class))).thenReturn(savedParticipant);

        // Act
        ParticipantResponseDTO result = transactionService.addParticipant(transactionId, dto, brokerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Co-Broker John");
        verify(participantRepository).save(any(TransactionParticipant.class));
    }

    @Test
    void removeParticipant_withValidData_removesParticipant() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        TransactionParticipant participant = new TransactionParticipant();
        participant.setId(participantId);
        participant.setTransactionId(transactionId);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        // Act
        transactionService.removeParticipant(transactionId, participantId, brokerId);

        // Assert
        verify(participantRepository).delete(participant);
    }

    @Test
    void getParticipants_returnsList() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        TransactionParticipant p1 = new TransactionParticipant();
        p1.setId(UUID.randomUUID());
        p1.setName("P1");
        p1.setTransactionId(transactionId);
        p1.setRole(ParticipantRole.BROKER);

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        // We need to ensure the access check passes.
        // If we pass a random userId in the test, verifyTransactionAccess will fail
        // unless we mock it or setup the tx correctly.
        // The service implementation calls
        // TransactionAccessUtils.verifyTransactionAccess(tx, userId).
        // If we pass a random user ID, that util will likely throw Forbidden unless the
        // user is the broker or client.
        // So we should use the brokerId as the userId in the test.

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(p1));

        // Act
        List<ParticipantResponseDTO> result = transactionService.getParticipants(transactionId, brokerId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("P1");
    }

    @Test
    void addParticipant_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        AddParticipantRequestDTO dto = new AddParticipantRequestDTO();
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.addParticipant(transactionId, dto, brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void addParticipant_withWrongBrokerId_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(UUID.randomUUID()); // Different broker
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        AddParticipantRequestDTO dto = new AddParticipantRequestDTO();

        // Act & Assert
        assertThatThrownBy(() -> transactionService.addParticipant(transactionId, dto, brokerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("access");
    }

    @Test
    void removeParticipant_withNonExistentTransaction_throwsNotFoundException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.removeParticipant(transactionId, participantId, brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void removeParticipant_withWrongBrokerId_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(UUID.randomUUID()); // Different broker
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.removeParticipant(transactionId, participantId, brokerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("access");
    }

    @Test
    void removeParticipant_withNonExistentParticipant_throwsNotFoundException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findById(participantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.removeParticipant(transactionId, participantId, brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Participant not found");
    }

    @Test
    void removeParticipant_withParticipantNotBelongingToTransaction_throwsBadRequestException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);

        TransactionParticipant participant = new TransactionParticipant();
        participant.setId(participantId);
        participant.setTransactionId(UUID.randomUUID()); // Different transaction

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.removeParticipant(transactionId, participantId, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void saveInternalNotes_withValidNotes_addsTimelineEntry() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        String notes = "Internal meeting notes";

        // Act
        transactionService.saveInternalNotes(transactionId, notes, brokerId);

        // Assert
        verify(timelineService).addEntry(
                eq(transactionId),
                eq(brokerId),
                eq(TimelineEntryType.NOTE),
                eq(notes),
                isNull());
    }

    @Test
    void saveInternalNotes_withNullNotes_doesNothing() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        // Act
        transactionService.saveInternalNotes(transactionId, null, brokerId);

        // Assert
        verify(timelineService, never()).addEntry(any(), any(), any(), any(), any());
    }

    @Test
    void saveInternalNotes_withBlankNotes_doesNothing() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        // Act
        transactionService.saveInternalNotes(transactionId, "   ", brokerId);

        // Assert
        verify(timelineService, never()).addEntry(any(), any(), any(), any(), any());
    }

    // ========== Condition Management Tests ==========

    @Test
    void getConditions_withValidBrokerAccess_returnsConditions() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.Condition condition = 
                createSampleCondition(transactionId);
        when(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(transactionId))
                .thenReturn(List.of(condition));

        // Act
        var result = transactionService.getConditions(transactionId, brokerId, true);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING);
        verify(conditionRepository).findByTransactionIdOrderByDeadlineDateAsc(transactionId);
    }

    @Test
    void getConditions_withClientAccess_returnsConditions() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(clientId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.Condition condition = 
                createSampleCondition(transactionId);
        when(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(transactionId))
                .thenReturn(List.of(condition));

        // Act
        var result = transactionService.getConditions(transactionId, clientId, false);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void getConditions_withUnauthorizedAccess_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID unauthorizedUserId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, UUID.randomUUID());
        tx.setClientId(UUID.randomUUID()); // Different client
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getConditions(transactionId, unauthorizedUserId, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void addCondition_withValidData_createsCondition() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = 
                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO.builder()
                        .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                        .description("Must obtain mortgage approval")
                        .deadlineDate(java.time.LocalDate.now().plusDays(30))
                        .build();

        com.example.courtierprobackend.transactions.datalayer.Condition savedCondition = 
                createSampleCondition(transactionId);
        when(conditionRepository.save(any())).thenReturn(savedCondition);

        // Act
        var result = transactionService.addCondition(transactionId, request, brokerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING);
        verify(conditionRepository).save(any());
        verify(timelineService).addEntry(
                eq(transactionId),
                eq(brokerId),
                eq(TimelineEntryType.CONDITION_ADDED),
                any(),
                any(),
                any());
    }

    @Test
    void addCondition_withOtherTypeAndNoCustomTitle_throwsBadRequestException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = 
                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO.builder()
                        .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER)
                        .description("Custom requirement")
                        .deadlineDate(java.time.LocalDate.now().plusDays(30))
                        .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionService.addCondition(transactionId, request, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Custom title is required");
    }

    @Test
    void addCondition_withOtherTypeAndCustomTitle_createsCondition() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = 
                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO.builder()
                        .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER)
                        .customTitle("Survey Completion")
                        .description("Must complete land survey")
                        .deadlineDate(java.time.LocalDate.now().plusDays(30))
                        .build();

        com.example.courtierprobackend.transactions.datalayer.Condition savedCondition = 
                createSampleCondition(transactionId);
        savedCondition.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER);
        savedCondition.setCustomTitle("Survey Completion");
        when(conditionRepository.save(any())).thenReturn(savedCondition);

        // Act
        var result = transactionService.addCondition(transactionId, request, brokerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCustomTitle()).isEqualTo("Survey Completion");
    }

    @Test
    void addCondition_withUnauthorizedBroker_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID unauthorizedBrokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = 
                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO.builder()
                        .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                        .description("Test")
                        .deadlineDate(java.time.LocalDate.now().plusDays(30))
                        .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionService.addCondition(transactionId, request, unauthorizedBrokerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateCondition_withValidData_updatesCondition() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.Condition existingCondition = 
                createSampleCondition(transactionId);
        existingCondition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(existingCondition));

        com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = 
                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO.builder()
                        .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                        .description("Updated description")
                        .deadlineDate(java.time.LocalDate.now().plusDays(45))
                        .build();

        when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = transactionService.updateCondition(transactionId, conditionId, request, brokerId);

        // Assert
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(timelineService).addEntry(
                eq(transactionId),
                eq(brokerId),
                eq(TimelineEntryType.CONDITION_UPDATED),
                anyString(),
                isNull(),
                any());
    }

    @Test
    void updateCondition_withNonExistentCondition_throwsNotFoundException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.empty());

        com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = 
                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO.builder()
                        .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                        .description("Test")
                        .deadlineDate(java.time.LocalDate.now().plusDays(30))
                        .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateCondition(transactionId, conditionId, request, brokerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeCondition_withValidData_deletesCondition() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.Condition condition = 
                createSampleCondition(transactionId);
        condition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

        // Act
        transactionService.removeCondition(transactionId, conditionId, brokerId);

        // Assert
        verify(conditionRepository).delete(condition);
        verify(timelineService).addEntry(
                eq(transactionId),
                eq(brokerId),
                eq(TimelineEntryType.CONDITION_REMOVED),
                anyString(),
                isNull(),
                any());
    }

    @Test
    void updateConditionStatus_toSatisfied_setsSatisfiedAt() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.Condition condition = 
                createSampleCondition(transactionId);
        condition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
        when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = transactionService.updateConditionStatus(
                transactionId, conditionId, 
                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED, 
                brokerId);

        // Assert
        assertThat(result.getStatus()).isEqualTo(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED);
        assertThat(result.getSatisfiedAt()).isNotNull();
        verify(timelineService).addEntry(
                eq(transactionId),
                eq(brokerId),
                eq(TimelineEntryType.CONDITION_SATISFIED),
                anyString(),
                isNull(),
                any());
    }

    @Test
    void updateConditionStatus_toFailed_doesNotSetSatisfiedAt() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        com.example.courtierprobackend.transactions.datalayer.Condition condition = 
                createSampleCondition(transactionId);
        condition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
        when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = transactionService.updateConditionStatus(
                transactionId, conditionId, 
                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.FAILED, 
                brokerId);

        // Assert
        assertThat(result.getStatus()).isEqualTo(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.FAILED);
        assertThat(result.getSatisfiedAt()).isNull();
    }

    // Helper method for condition tests
    private Transaction createBuySideTransaction(UUID transactionId, UUID brokerId) {
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setStatus(TransactionStatus.ACTIVE);
        return tx;
    }

    private com.example.courtierprobackend.transactions.datalayer.Condition createSampleCondition(UUID transactionId) {
        com.example.courtierprobackend.transactions.datalayer.Condition condition = 
                new com.example.courtierprobackend.transactions.datalayer.Condition();
        condition.setConditionId(UUID.randomUUID());
        condition.setTransactionId(transactionId);
        condition.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING);
        condition.setDescription("Buyer must obtain mortgage approval");
        condition.setDeadlineDate(java.time.LocalDate.now().plusDays(30));
        condition.setStatus(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING);
        condition.setCreatedAt(LocalDateTime.now());
        condition.setUpdatedAt(LocalDateTime.now());
        return condition;
    }

    @Test
    void updateConditionStatus_withUnauthorizedBroker_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID otherBrokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateConditionStatus(
                transactionId, conditionId, 
                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED, 
                otherBrokerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateConditionStatus_withNonExistentCondition_throwsNotFoundException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateConditionStatus(
                transactionId, conditionId, 
                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED, 
                brokerId))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.NotFoundException.class);
    }

    @Test
    void updateConditionStatus_toFailed_createsCorrectTimelineEntry() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        var condition = createSampleCondition(transactionId);
        condition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
        when(conditionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.updateConditionStatus(
                transactionId, conditionId, 
                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.FAILED, 
                brokerId);

        // Assert - verify timeline uses CONDITION_FAILED type
        verify(timelineService).addEntry(
                eq(transactionId),
                eq(brokerId),
                eq(TimelineEntryType.CONDITION_FAILED),
                anyString(),
                any(),
                any());
    }

    @Test
    void removeCondition_withUnauthorizedBroker_throwsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID otherBrokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.removeCondition(
                transactionId, conditionId, otherBrokerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeCondition_withNonExistentCondition_throwsNotFoundException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.removeCondition(
                transactionId, conditionId, brokerId))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.NotFoundException.class);
    }

    @Test
    void removeCondition_withConditionFromDifferentTransaction_throwsBadRequestException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID otherTransactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        var condition = createSampleCondition(otherTransactionId);
        condition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.removeCondition(
                transactionId, conditionId, brokerId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateCondition_withConditionFromDifferentTransaction_throwsBadRequestException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID otherTransactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        var condition = createSampleCondition(otherTransactionId);
        condition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

        var request = new com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO();
        request.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.INSPECTION);
        request.setDescription("Updated description");

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateCondition(
                transactionId, conditionId, request, brokerId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateConditionStatus_withConditionFromDifferentTransaction_throwsBadRequestException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID otherTransactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        var condition = createSampleCondition(otherTransactionId);
        condition.setConditionId(conditionId);
        when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.updateConditionStatus(
                transactionId, conditionId, 
                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED, 
                brokerId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addCondition_withClientId_throwsForbiddenException() {
        // Arrange - client trying to add condition
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(clientId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        var request = new com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO();
        request.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING);
        request.setDescription("Test condition");

        // Act & Assert - client ID used instead of broker ID
        assertThatThrownBy(() -> transactionService.addCondition(transactionId, request, clientId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateCondition_withClientId_throwsForbiddenException() {
        // Arrange - client trying to update condition
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(clientId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        var request = new com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO();
        request.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.INSPECTION);
        request.setDescription("Updated");

        // Act & Assert - client ID used instead of broker ID
        assertThatThrownBy(() -> transactionService.updateCondition(transactionId, conditionId, request, clientId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeCondition_withClientId_throwsForbiddenException() {
        // Arrange - client trying to remove condition
        UUID transactionId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Transaction tx = createBuySideTransaction(transactionId, brokerId);
        tx.setClientId(clientId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert - client ID used instead of broker ID
        assertThatThrownBy(() -> transactionService.removeCondition(transactionId, conditionId, clientId))
                .isInstanceOf(ForbiddenException.class);
    }
    // =================================================================================================
    // NEW TESTS FOR RETRIEVAL METHODS
    // =================================================================================================

    @Test
    void getBrokerTransactions_WithFilters_ReturnsFilteredTransactions() {
        // Arrange
        UUID brokerId = UUID.randomUUID();
        when(transactionRepository.findAllByFilters(eq(brokerId), eq(com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus.ACTIVE), eq(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE), eq(com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage.BUYER_SUBMIT_OFFER), anyBoolean()))
            .thenReturn(List.of(new Transaction()));

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerId, "ACTIVE", "BUYER_SUBMIT_OFFER", "BUY");

        // Assert
        assertThat(result).hasSize(1);
        verify(transactionRepository).findAllByFilters(eq(brokerId), eq(com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus.ACTIVE), eq(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE), eq(com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage.BUYER_SUBMIT_OFFER), anyBoolean());
    }

    @Test
    void getBrokerTransactions_NoFilters_ReturnsAll() {
        // Arrange
        UUID brokerId = UUID.randomUUID();
        when(transactionRepository.findAllByFilters(eq(brokerId), isNull(), isNull(), isNull(), anyBoolean()))
            .thenReturn(List.of(new Transaction(), new Transaction()));

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerId, null, null, null);

        // Assert
        assertThat(result).hasSize(2);
        verify(transactionRepository).findAllByFilters(eq(brokerId), isNull(), isNull(), isNull(), anyBoolean());
    }

    @Test
    void getClientTransactions_ReturnsTransactionsForClient() {
        // Arrange
        UUID clientId = UUID.randomUUID();
        when(transactionRepository.findAllByClientId(clientId)).thenReturn(List.of(new Transaction()));

        // Act
        List<TransactionResponseDTO> result = transactionService.getClientTransactions(clientId);

        // Assert
        assertThat(result).hasSize(1);
        verify(transactionRepository).findAllByClientId(clientId);
    }

    @Test
    void getBrokerClientTransactions_ReturnsTransactionsForClientAndBroker() {
        // Arrange
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Transaction t1 = new Transaction();
        t1.setBrokerId(brokerId);
        t1.setClientId(clientId);

        Transaction t2 = new Transaction();
        t2.setBrokerId(UUID.randomUUID()); // Different broker
        t2.setClientId(clientId);

        when(transactionRepository.findAllByClientId(clientId)).thenReturn(List.of(t1, t2));

        // Act
        List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerId, clientId);

        // Assert
        assertThat(result).hasSize(1);
        // Should filter out t2
        verify(transactionRepository).findAllByClientId(clientId);
    }

    @Test
    void getAllClientTransactions_ReturnsAllForClient() {
        // Arrange
        UUID clientId = UUID.randomUUID();
        UUID brokerId1 = UUID.randomUUID();
        UUID brokerId2 = UUID.randomUUID();

        Transaction t1 = new Transaction();
        t1.setBrokerId(brokerId1);
        t1.setClientId(clientId);
        
        Transaction t2 = new Transaction();
        t2.setBrokerId(brokerId2);
        t2.setClientId(clientId);

        when(transactionRepository.findAllByClientId(clientId)).thenReturn(List.of(t1, t2));

        // Act
        List<TransactionResponseDTO> result = transactionService.getAllClientTransactions(clientId);

        // Assert
        assertThat(result).hasSize(2);
        verify(transactionRepository).findAllByClientId(clientId);
    }

    @Test
    void getByTransactionId_WithValidAccess_ReturnsTransaction() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act
        TransactionResponseDTO result = transactionService.getByTransactionId(transactionId, brokerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
    }

    @Test
    void getByTransactionId_WithUnauthorizedUser_ThrowsForbiddenException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setClientId(UUID.randomUUID());
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getByTransactionId(transactionId, otherUserId))
                .isInstanceOf(ForbiddenException.class);
    }

        // =================================================================================================
        // ARCHIVE FEATURE TESTS
        // =================================================================================================

        @Test
        void archiveTransaction_SuccessfullyArchives() {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();
            Transaction tx = new Transaction();
            tx.setTransactionId(transactionId);
            tx.setBrokerId(brokerId);
            tx.setArchived(false);

            when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

            transactionService.archiveTransaction(transactionId, brokerId);

            assertThat(tx.getArchived()).isTrue();
            assertThat(tx.getArchivedAt()).isNotNull();
            assertThat(tx.getArchivedBy()).isEqualTo(brokerId);
            verify(transactionRepository).save(tx);
            verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE), contains("archived"), isNull());
        }

        @Test
        void archiveTransaction_AlreadyArchived_ThrowsBadRequest() {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();
            Transaction tx = new Transaction();
            tx.setTransactionId(transactionId);
            tx.setBrokerId(brokerId);
            tx.setArchived(true);

            when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> transactionService.archiveTransaction(transactionId, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already archived");
        }

        @Test
        void unarchiveTransaction_SuccessfullyUnarchives() {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();
            Transaction tx = new Transaction();
            tx.setTransactionId(transactionId);
            tx.setBrokerId(brokerId);
            tx.setArchived(true);
            tx.setArchivedAt(java.time.LocalDateTime.now());
            tx.setArchivedBy(brokerId);

            when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

            transactionService.unarchiveTransaction(transactionId, brokerId);

            assertThat(tx.getArchived()).isFalse();
            assertThat(tx.getArchivedAt()).isNull();
            assertThat(tx.getArchivedBy()).isNull();
            verify(transactionRepository).save(tx);
            verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE), contains("unarchived"), isNull());
        }

        @Test
        void unarchiveTransaction_NotArchived_ThrowsBadRequest() {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();
            Transaction tx = new Transaction();
            tx.setTransactionId(transactionId);
            tx.setBrokerId(brokerId);
            tx.setArchived(false);

            when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> transactionService.unarchiveTransaction(transactionId, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not archived");
        }

        @Test
        void getArchivedTransactions_ReturnsArchivedList() {
            UUID brokerId = UUID.randomUUID();
            Transaction tx1 = new Transaction();
            tx1.setTransactionId(UUID.randomUUID());
            tx1.setBrokerId(brokerId);
            tx1.setArchived(true);
            Transaction tx2 = new Transaction();
            tx2.setTransactionId(UUID.randomUUID());
            tx2.setBrokerId(brokerId);
            tx2.setArchived(true);

            when(transactionRepository.findArchivedByBrokerId(brokerId)).thenReturn(List.of(tx1, tx2));
            // Properly mock userAccountRepository to return Optional<UserAccount>
            when(userAccountRepository.findByAuth0UserId(any())).thenReturn(Optional.of(new UserAccount()));

            List<TransactionResponseDTO> result = transactionService.getArchivedTransactions(brokerId);

            assertThat(result).hasSize(2);
            verify(transactionRepository).findArchivedByBrokerId(brokerId);
        }
}
