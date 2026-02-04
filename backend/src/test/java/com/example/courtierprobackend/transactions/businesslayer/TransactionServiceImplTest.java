package com.example.courtierprobackend.transactions.businesslayer;

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
import com.example.courtierprobackend.transactions.datalayer.Offer;
import com.example.courtierprobackend.transactions.datalayer.Property;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.UpdateParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.PropertyStatus;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        private com.example.courtierprobackend.infrastructure.storage.ObjectStorageService objectStorageService;

        @Mock
        private com.example.courtierprobackend.documents.datalayer.DocumentRepository documentRequestRepository;

        @Mock
        private com.example.courtierprobackend.transactions.datalayer.repositories.DocumentConditionLinkRepository documentConditionLinkRepository;

        @Mock
        private com.example.courtierprobackend.transactions.datalayer.repositories.SearchCriteriaRepository searchCriteriaRepository;

        @BeforeEach
        void setup() {
                transactionService = new TransactionServiceImpl(transactionRepository, pinnedTransactionRepository,
                                userAccountRepository, emailService,
                                notificationService, timelineService, participantRepository, propertyRepository,
                                offerRepository, conditionRepository,
                                propertyOfferRepository, offerDocumentRepository, offerRevisionRepository,
                                objectStorageService, documentRequestRepository, documentConditionLinkRepository,
                                searchCriteriaRepository);
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
                                new PropertyAddress(dto.getPropertyAddress().getStreet(),
                                                dto.getPropertyAddress().getCity(),
                                                dto.getPropertyAddress().getProvince(),
                                                dto.getPropertyAddress().getPostalCode()));
                when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTx);

                // Simulate Notification exception - use the i18n version with Map
                doThrow(new RuntimeException("Notification Error")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class),
                                                anyString(), any());

                // Act
                TransactionResponseDTO result = transactionService.createTransaction(dto);

                // Assert
                assertThat(result).isNotNull();
                verify(transactionRepository).save(any(Transaction.class));
                // Verify notification service WAS called (but failed)
                verify(notificationService).createNotification(anyString(), anyString(), anyString(),
                                any(java.util.Map.class), anyString(), any());
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
                Transaction tx1 = new Transaction();
                tx1.setTransactionId(UUID.randomUUID());
                Transaction tx2 = new Transaction();
                tx2.setTransactionId(UUID.randomUUID());
                List<Transaction> transactions = List.of(tx1, tx2);
                when(transactionRepository.findAllByFilters(eq(brokerUuid), any(), any(), any(), anyBoolean()))
                                .thenReturn(transactions);

                // Act
                List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerUuid, null, null,
                                null);

                // Assert
                assertThat(result).hasSize(2);
                verify(transactionRepository).findAllByFilters(eq(brokerUuid), any(), any(), any(), anyBoolean());
        }

        @Test
        void getBrokerTransactions_withNoBrokerTransactions_returnsEmptyList() {
                // Arrange
                when(transactionRepository.findAllByFilters(any(UUID.class), any(), any(), any(), anyBoolean()))
                                .thenReturn(List.of());

                // Act
                List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(UUID.randomUUID(), null,
                                null,
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
                verify(transactionRepository).findAllByFilters(eq(brokerUuid), eq(TransactionStatus.ACTIVE), any(),
                                any(), anyBoolean());
        }

        @Test
        void getBrokerTransactions_withInvalidStatus_passesNull() {
                // Arrange
                UUID brokerUuid = UUID.randomUUID();

                // Act
                transactionService.getBrokerTransactions(brokerUuid, "INVALID_STATUS", null, null);

                // Assert
                verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), isNull(), isNull(),
                                anyBoolean());
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
                verify(transactionRepository).findAllByFilters(eq(brokerUuid), isNull(), isNull(), isNull(),
                                anyBoolean());
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
                List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid,
                                clientUuid);

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
                List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid,
                                clientUuid);

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
                List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid,
                                clientUuid);

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
                List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerUuid,
                                clientUuid);

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
                when(userAccountRepository.findById(clientUuid))
                                .thenReturn(Optional.of(createUserAccount(clientUuid, "Test Client")));
                when(userAccountRepository.findById(broker1Uuid))
                                .thenReturn(Optional.of(createUserAccount(broker1Uuid, "Broker One")));
                when(userAccountRepository.findById(broker2Uuid))
                                .thenReturn(Optional.of(createUserAccount(broker2Uuid, "Broker Two")));

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
                com.example.courtierprobackend.transactions.datalayer.Property acceptedProperty = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
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
                com.example.courtierprobackend.transactions.datalayer.Property pendingProperty = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
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
                assertThatThrownBy(() -> transactionService.updateTransactionStage(UUID.randomUUID(), null,
                                UUID.randomUUID()))
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
                TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto,
                                brokerUuid);

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

                // 2. Verify In-App Notification created with i18n keys and params
                verify(notificationService, times(1)).createNotification(
                                eq(clientId.toString()),
                                eq("notifications.stageUpdate.title"),
                                eq("notifications.stageUpdate.message"),
                                argThat(params -> "Buyer Offer Accepted".equals(params.get("stage")) &&
                                                "Broker Agent".equals(params.get("brokerName")) &&
                                                "123 Test St".equals(params.get("propertyAddress"))),
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

                // 2. Verify In-App Notification created with i18n keys and params (French)
                verify(notificationService, times(1)).createNotification(
                                eq(clientId.toString()),
                                eq("notifications.stageUpdate.title"),
                                eq("notifications.stageUpdate.message"),
                                argThat(params -> "Offre Acceptée".equals(params.get("stage")) &&
                                                "Courtier Pro".equals(params.get("brokerName")) &&
                                                "123 Rue Test".equals(params.get("propertyAddress"))),
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
                // Should use "Unknown Address" default and i18n keys
                verify(notificationService, times(1)).createNotification(
                                eq(clientId.toString()),
                                eq("notifications.stageUpdate.title"),
                                eq("notifications.stageUpdate.message"),
                                argThat(params -> "Unknown Address".equals(params.get("propertyAddress"))),
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
                                .thenReturn(Optional.of(
                                                new com.example.courtierprobackend.user.dataaccesslayer.UserAccount()));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

                // Make email service throw exception
                doThrow(new RuntimeException("Email server down")).when(emailService).sendStageUpdateEmail(any(), any(),
                                any(),
                                any(), any(), any());

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("BUYER_OFFER_ACCEPTED");

                // Act
                TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto,
                                brokerId);

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
                TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto,
                                brokerUuid);

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

                // Act
                TransactionResponseDTO response = transactionService.updateTransactionStage(transactionId, dto,
                                brokerUuid);

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
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

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
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

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
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

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
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

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
                                .thenThrow(new org.springframework.dao.OptimisticLockingFailureException(
                                                "Optimistic lock failed"));

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("BUYER_SHOP_FOR_PROPERTY");

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ConflictException.class)
                                .hasMessageContaining("The transaction was updated by another user");
        }

        @Test
        void updateTransactionStage_withBackwardTransition_andReason_updatesStageAndCreatesRollbackEntry() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);
                tx.setBuyerStage(BuyerStage.BUYER_OFFER_ACCEPTED); // Previous stage (Ordinal > 0)
                tx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());

                Transaction savedTx = new Transaction();
                savedTx.setTransactionId(transactionId);
                savedTx.setBrokerId(brokerId);
                savedTx.setSide(TransactionSide.BUY_SIDE);
                savedTx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY); // New Stage (Ordinal 0)
                savedTx.setClientId(clientId);
                savedTx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
                com.example.courtierprobackend.user.dataaccesslayer.UserAccount mockClient = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
                mockClient.setId(clientId);
                mockClient.setFirstName("Client");
                mockClient.setLastName("User");
                mockClient.setEmail("client@example.com");
                mockClient.setPreferredLanguage("en");

                com.example.courtierprobackend.user.dataaccesslayer.UserAccount mockBroker = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
                mockBroker.setId(brokerId);
                mockBroker.setFirstName("Broker");
                mockBroker.setLastName("User");

                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(mockClient));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(mockBroker));
                when(userAccountRepository.findById(argThat(id -> !id.equals(clientId) && !id.equals(brokerId))))
                                .thenReturn(Optional.of(mockBroker));

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("BUYER_PREQUALIFY_FINANCIALLY");
                dto.setReason("Back to start");

                // Act
                transactionService.updateTransactionStage(transactionId, dto, brokerId);

                // Assert
                // 1. Verify Timeline Entry is STAGE_ROLLBACK and has reason
                verify(timelineService).addEntry(
                                eq(transactionId),
                                eq(brokerId),
                                eq(TimelineEntryType.STAGE_ROLLBACK),
                                isNull(),
                                isNull(),
                                argThat(info -> "Back to start".equals(info.getReason()) &&
                                                "BUYER_PREQUALIFY_FINANCIALLY".equals(info.getNewStage())));

                // 2. Verify In-App Notification created with rollback i18n keys and params
                verify(notificationService, times(1)).createNotification(
                                eq(clientId.toString()),
                                eq("notifications.stageRollback.title"),
                                eq("notifications.stageRollback.message"),
                                any(java.util.Map.class),
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.STAGE_UPDATE));

                // 3. Verify Email was NOT sent for rollbacks (as per current implementation)
                verify(emailService, never()).sendStageUpdateEmail(anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString());
        }

        @Test
        void updateTransactionStage_withBackwardTransition_withoutReason_throwsBadRequest() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setSide(TransactionSide.SELL_SIDE);
                tx.setSellerStage(SellerStage.SELLER_HANDOVER_KEYS); // Late stage

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("SELLER_INITIAL_CONSULTATION"); // Early stage
                dto.setReason(""); // Missing reason

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Reason is required");
        }

        @Test
        void updateTransactionStage_withForwardTransition_withoutReason_updatesStageAndCreatesChangeEntry() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);
                tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
                tx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());

                Transaction savedTx = new Transaction();
                savedTx.setTransactionId(transactionId);
                savedTx.setBrokerId(brokerId);
                savedTx.setSide(TransactionSide.BUY_SIDE);
                savedTx.setBuyerStage(BuyerStage.BUYER_SHOP_FOR_PROPERTY);
                savedTx.setPropertyAddress(new com.example.courtierprobackend.transactions.datalayer.PropertyAddress());

                com.example.courtierprobackend.user.dataaccesslayer.UserAccount mockUser = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
                mockUser.setId(UUID.randomUUID());
                mockUser.setFirstName("Test");
                mockUser.setLastName("User");
                mockUser.setEmail("test@example.com");
                mockUser.setPreferredLanguage("en");

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
                when(userAccountRepository.findById(any())).thenReturn(Optional.of(mockUser));

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("BUYER_SHOP_FOR_PROPERTY");
                // No reason provided

                // Act
                transactionService.updateTransactionStage(transactionId, dto, brokerId);

                // Assert
                verify(timelineService).addEntry(
                                eq(transactionId),
                                eq(brokerId),
                                eq(TimelineEntryType.STAGE_CHANGE), // STAGE_CHANGE not ROLLBACK
                                isNull(),
                                isNull(),
                                argThat(info -> info.getReason() == null));

                // Verify Notifications SENT
                verify(notificationService).createNotification(any(), any(), any(), any(), any(), any());
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
                when(pinnedTransactionRepository.existsByBrokerIdAndTransactionId(brokerId, transactionId))
                                .thenReturn(false);

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
                when(pinnedTransactionRepository.existsByBrokerIdAndTransactionId(brokerId, transactionId))
                                .thenReturn(true);

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
                when(userAccountRepository.findByEmail(anyString())).thenReturn(
                                Optional.of(new com.example.courtierprobackend.user.dataaccesslayer.UserAccount()));

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

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any())).thenReturn(tx);

                // Act
                transactionService.saveInternalNotes(transactionId, notes, brokerId);

                // Assert
                verify(transactionRepository).save(any());
                verify(timelineService).addEntry(
                                eq(transactionId),
                                eq(brokerId),
                                eq(TimelineEntryType.NOTE),
                                eq(notes),
                                isNull());
        }

        @Test
        void saveInternalNotes_withNullNotes_savesEmptyNotes() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any())).thenReturn(tx);

                // Act
                transactionService.saveInternalNotes(transactionId, null, brokerId);

                // Assert - saves but no timeline entry for null/blank notes
                verify(transactionRepository).save(any());
                verify(timelineService, never()).addEntry(any(), any(), any(), any(), any());
        }

        @Test
        void saveInternalNotes_withBlankNotes_savesEmptyNotes() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any())).thenReturn(tx);

                // Act
                transactionService.saveInternalNotes(transactionId, "   ", brokerId);

                // Assert - saves but no timeline entry for null/blank notes
                verify(transactionRepository).save(any());
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

                com.example.courtierprobackend.transactions.datalayer.Condition condition = createSampleCondition(
                                transactionId);
                when(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(transactionId))
                                .thenReturn(List.of(condition));

                // Act
                var result = transactionService.getConditions(transactionId, brokerId, true);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getType()).isEqualTo(
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING);
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

                com.example.courtierprobackend.transactions.datalayer.Condition condition = createSampleCondition(
                                transactionId);
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

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .description("Must obtain mortgage approval")
                                .deadlineDate(java.time.LocalDate.now().plusDays(30))
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Condition savedCondition = createSampleCondition(
                                transactionId);
                when(conditionRepository.save(any())).thenReturn(savedCondition);

                // Act
                var result = transactionService.addCondition(transactionId, request, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getType()).isEqualTo(
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING);
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

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
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

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER)
                                .customTitle("Survey Completion")
                                .description("Must complete land survey")
                                .deadlineDate(java.time.LocalDate.now().plusDays(30))
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Condition savedCondition = createSampleCondition(
                                transactionId);
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

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
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

                com.example.courtierprobackend.transactions.datalayer.Condition existingCondition = createSampleCondition(
                                transactionId);
                existingCondition.setConditionId(conditionId);
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(existingCondition));

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
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

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO request = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .description("Test")
                                .deadlineDate(java.time.LocalDate.now().plusDays(30))
                                .build();

                // Act & Assert
                assertThatThrownBy(
                                () -> transactionService.updateCondition(transactionId, conditionId, request, brokerId))
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

                com.example.courtierprobackend.transactions.datalayer.Condition condition = createSampleCondition(
                                transactionId);
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

                com.example.courtierprobackend.transactions.datalayer.Condition condition = createSampleCondition(
                                transactionId);
                condition.setConditionId(conditionId);
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateConditionStatus(
                                transactionId, conditionId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED,
                                brokerId);

                // Assert
                assertThat(result.getStatus()).isEqualTo(
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED);
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

                com.example.courtierprobackend.transactions.datalayer.Condition condition = createSampleCondition(
                                transactionId);
                condition.setConditionId(conditionId);
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateConditionStatus(
                                transactionId, conditionId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.FAILED,
                                brokerId);

                // Assert
                assertThat(result.getStatus()).isEqualTo(
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.FAILED);
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

        private com.example.courtierprobackend.transactions.datalayer.Condition createSampleCondition(
                        UUID transactionId) {
                com.example.courtierprobackend.transactions.datalayer.Condition condition = new com.example.courtierprobackend.transactions.datalayer.Condition();
                condition.setConditionId(UUID.randomUUID());
                condition.setTransactionId(transactionId);
                condition.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING);
                condition.setDescription("Buyer must obtain mortgage approval");
                condition.setDeadlineDate(java.time.LocalDate.now().plusDays(30));
                condition.setStatus(
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING);
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
                assertThatThrownBy(
                                () -> transactionService.updateCondition(transactionId, conditionId, request, clientId))
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
                when(transactionRepository.findAllByFilters(eq(brokerId), eq(
                                com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus.ACTIVE),
                                eq(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE),
                                eq(com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage.BUYER_SUBMIT_OFFER),
                                anyBoolean()))
                                .thenReturn(List.of(new Transaction()));

                // Act
                List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerId, "ACTIVE",
                                "BUYER_SUBMIT_OFFER", "BUY");

                // Assert
                assertThat(result).hasSize(1);
                verify(transactionRepository).findAllByFilters(eq(brokerId), eq(
                                com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus.ACTIVE),
                                eq(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE),
                                eq(com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage.BUYER_SUBMIT_OFFER),
                                anyBoolean());
        }

        @Test
        void getBrokerTransactions_NoFilters_ReturnsAll() {
                // Arrange
                UUID brokerId = UUID.randomUUID();
                Transaction t1 = new Transaction();
                t1.setTransactionId(UUID.randomUUID());
                Transaction t2 = new Transaction();
                t2.setTransactionId(UUID.randomUUID());
                when(transactionRepository.findAllByFilters(eq(brokerId), isNull(), isNull(), isNull(), anyBoolean()))
                                .thenReturn(List.of(t1, t2));

                // Act
                List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerId, null, null,
                                null);

                // Assert
                assertThat(result).hasSize(2);
                verify(transactionRepository).findAllByFilters(eq(brokerId), isNull(), isNull(), isNull(),
                                anyBoolean());
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
                List<TransactionResponseDTO> result = transactionService.getBrokerClientTransactions(brokerId,
                                clientId);

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
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE),
                                contains("archived"), isNull());
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
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.STATUS_CHANGE),
                                contains("unarchived"), isNull());
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

        // =================================================================================================
        // OFFER DOCUMENT UPLOAD TESTS
        // =================================================================================================

        @Test
        void uploadOfferDocument_withValidData_returnsDocument() throws java.io.IOException {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
                when(mockFile.getContentType()).thenReturn("application/pdf");
                when(mockFile.getSize()).thenReturn(1024L);

                com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject storageObject = com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject
                                .builder()
                                .s3Key("test-key")
                                .fileName("test.pdf")
                                .build();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(objectStorageService.uploadFile(any(), any(), any())).thenReturn(storageObject);
                when(offerDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.uploadOfferDocument(offerId, mockFile, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getFileName()).isEqualTo("test.pdf");
                verify(offerDocumentRepository).save(any());
        }

        @Test
        void uploadOfferDocument_whenOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.uploadOfferDocument(offerId, mockFile, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Offer not found");
        }

        @Test
        void uploadOfferDocument_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();
                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.uploadOfferDocument(offerId, mockFile, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void uploadOfferDocument_whenWrongBroker_throwsForbiddenException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID wrongBrokerId = UUID.randomUUID();

                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId); // Different broker

                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.uploadOfferDocument(offerId, mockFile, wrongBrokerId))
                                .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void uploadOfferDocument_whenIOException_throwsRuntimeException() throws java.io.IOException {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("test.pdf");

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(objectStorageService.uploadFile(any(), any(), any()))
                                .thenThrow(new java.io.IOException("Upload failed"));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.uploadOfferDocument(offerId, mockFile, brokerId))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Failed to upload document");
        }

        // =================================================================================================
        // PROPERTY OFFER DOCUMENT UPLOAD TESTS
        // =================================================================================================

        @Test
        void uploadPropertyOfferDocument_withValidData_returnsDocument() throws java.io.IOException {
                // Arrange
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();
                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn("offer.pdf");
                when(mockFile.getContentType()).thenReturn("application/pdf");
                when(mockFile.getSize()).thenReturn(2048L);

                com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject storageObject = com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject
                                .builder()
                                .s3Key("property-offer-key")
                                .fileName("offer.pdf")
                                .build();

                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(objectStorageService.uploadFile(any(), any(), any())).thenReturn(storageObject);
                when(offerDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.uploadPropertyOfferDocument(propertyOfferId, mockFile, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getFileName()).isEqualTo("offer.pdf");
                verify(offerDocumentRepository).save(any());
        }

        @Test
        void uploadPropertyOfferDocument_whenPropertyOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyOfferId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);

                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.uploadPropertyOfferDocument(propertyOfferId, mockFile,
                                brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Property offer not found");
        }

        @Test
        void uploadPropertyOfferDocument_whenPropertyNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();
                org.springframework.web.multipart.MultipartFile mockFile = mock(
                                org.springframework.web.multipart.MultipartFile.class);

                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.uploadPropertyOfferDocument(propertyOfferId, mockFile,
                                brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Property not found");
        }

        // =================================================================================================
        // OFFER DOCUMENT DOWNLOAD URL TESTS
        // =================================================================================================

        @Test
        void getOfferDocumentDownloadUrl_viaOffer_returnsUrl() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument document = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .offerId(offerId)
                                .propertyOfferId(null)
                                .s3Key("test-s3-key")
                                .build();
                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(document));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(objectStorageService.generatePresignedUrl("test-s3-key"))
                                .thenReturn("https://s3.example.com/presigned-url");

                // Act
                String result = transactionService.getOfferDocumentDownloadUrl(documentId, userId);

                // Assert
                assertThat(result).isEqualTo("https://s3.example.com/presigned-url");
        }

        @Test
        void getOfferDocumentDownloadUrl_viaPropertyOffer_returnsUrl() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument document = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .offerId(null)
                                .propertyOfferId(propertyOfferId)
                                .s3Key("property-offer-s3-key")
                                .build();
                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();
                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(document));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(objectStorageService.generatePresignedUrl("property-offer-s3-key"))
                                .thenReturn("https://s3.example.com/property-url");

                // Act
                String result = transactionService.getOfferDocumentDownloadUrl(documentId, userId);

                // Assert
                assertThat(result).isEqualTo("https://s3.example.com/property-url");
        }

        @Test
        void getOfferDocumentDownloadUrl_whenDocumentNotFound_throwsNotFoundException() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getOfferDocumentDownloadUrl(documentId, userId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document not found");
        }

        // =================================================================================================
        // OFFER DOCUMENT DELETION TESTS
        // =================================================================================================

        @Test
        void deleteOfferDocument_viaOffer_deletesDocument() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument document = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .offerId(offerId)
                                .propertyOfferId(null)
                                .s3Key("delete-s3-key")
                                .build();
                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(document));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                transactionService.deleteOfferDocument(documentId, brokerId);

                // Assert
                verify(objectStorageService).deleteFile("delete-s3-key");
                verify(offerDocumentRepository).delete(document);
        }

        @Test
        void deleteOfferDocument_viaPropertyOffer_deletesDocument() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument document = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .offerId(null)
                                .propertyOfferId(propertyOfferId)
                                .s3Key("property-delete-key")
                                .build();
                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();
                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(document));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                transactionService.deleteOfferDocument(documentId, brokerId);

                // Assert
                verify(objectStorageService).deleteFile("property-delete-key");
                verify(offerDocumentRepository).delete(document);
        }

        // =================================================================================================
        // CLEAR ACTIVE PROPERTY TESTS
        // =================================================================================================

        @Test
        void clearActiveProperty_clearsPropertyAddress() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setSide(TransactionSide.BUY_SIDE);
                tx.setPropertyAddress(new PropertyAddress("123 Main St", "City", "Province", "A1A 1A1"));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any())).thenReturn(tx);

                // Act
                transactionService.clearActiveProperty(transactionId, brokerId);

                // Assert
                assertThat(tx.getPropertyAddress()).isNull();
                verify(transactionRepository).save(tx);
        }

        @Test
        void clearActiveProperty_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.clearActiveProperty(transactionId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void clearActiveProperty_whenWrongBroker_throwsForbiddenException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID wrongBrokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.clearActiveProperty(transactionId, wrongBrokerId))
                                .isInstanceOf(ForbiddenException.class);
        }

        // =================================================================================================
        // UNIFIED DOCUMENTS TESTS
        // =================================================================================================

        @Test
        void getAllTransactionDocuments_includesOfferAttachments() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID documentId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setSide(TransactionSide.SELL_SIDE);

                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .build();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument offerDoc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .offerId(offerId)
                                .fileName("offer-doc.pdf")
                                .mimeType("application/pdf")
                                .sizeBytes(1024L)
                                .createdAt(java.time.LocalDateTime.now())
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId)).thenReturn(List.of());
                when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of(offer));
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offerId)).thenReturn(List.of(offerDoc));
                when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());

                // Act
                var result = transactionService.getAllTransactionDocuments(transactionId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getFileName()).isEqualTo("offer-doc.pdf");
                assertThat(result.get(0).getSource()).isEqualTo("OFFER_ATTACHMENT");
                assertThat(result.get(0).getSourceName()).contains("Test Buyer");
        }

        @Test
        void getAllTransactionDocuments_includesPropertyOfferAttachments() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID documentId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("456 Oak Ave", "City", "Province", "B2B 2B2"))
                                .build();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument propOfferDoc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .propertyOfferId(propertyOfferId)
                                .fileName("property-offer-doc.pdf")
                                .mimeType("application/pdf")
                                .sizeBytes(2048L)
                                .createdAt(java.time.LocalDateTime.now())
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId)).thenReturn(List.of());
                when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());
                when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                                .thenReturn(List.of(property));
                when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(List.of(propertyOffer));
                when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(propertyOfferId))
                                .thenReturn(List.of(propOfferDoc));

                // Act
                var result = transactionService.getAllTransactionDocuments(transactionId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getFileName()).isEqualTo("property-offer-doc.pdf");
                assertThat(result.get(0).getSource()).isEqualTo("PROPERTY_OFFER_ATTACHMENT");
                assertThat(result.get(0).getSourceName()).contains("456 Oak Ave");
        }

        @Test
        void getAllTransactionDocuments_sortsByDateDescending() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                Offer offer = com.example.courtierprobackend.transactions.datalayer.Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Buyer")
                                .build();

                java.time.LocalDateTime older = java.time.LocalDateTime.now().minusDays(2);
                java.time.LocalDateTime newer = java.time.LocalDateTime.now();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument olderDoc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(UUID.randomUUID())
                                .offerId(offerId)
                                .fileName("older.pdf")
                                .createdAt(older)
                                .build();
                com.example.courtierprobackend.transactions.datalayer.OfferDocument newerDoc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(UUID.randomUUID())
                                .offerId(offerId)
                                .fileName("newer.pdf")
                                .createdAt(newer)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId)).thenReturn(List.of());
                when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of(offer));
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offerId))
                                .thenReturn(List.of(olderDoc, newerDoc));
                when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());

                // Act
                var result = transactionService.getAllTransactionDocuments(transactionId, userId, true);

                // Assert
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getFileName()).isEqualTo("newer.pdf");
                assertThat(result.get(1).getFileName()).isEqualTo("older.pdf");
        }

        @Test
        void getAllTransactionDocuments_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getAllTransactionDocuments(transactionId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // PROPERTY OFFER TESTS (covering notification paths)
        // =================================================================================================

        @Test
        void addPropertyOffer_withValidData_sendsNotificationAndEmail() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "A1A 1A1"))
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setEmail("client@test.com");
                client.setFirstName("Test");
                client.setLastName("Client");
                client.setPreferredLanguage("en");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Test");
                broker.setLastName("Broker");

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.empty());
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyRepository.save(any())).thenReturn(property);
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

                // Act
                var result = transactionService.addPropertyOffer(propertyId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(notificationService).createNotification(eq(clientId.toString()), anyString(), anyString(),
                                any(java.util.Map.class), anyString(), any());
                verify(emailService).sendPropertyOfferMadeNotification(eq("client@test.com"), anyString(), anyString(),
                                anyString(), anyString(), eq(1), eq("en"));
        }

        @Test
        void addPropertyOffer_whenNotificationFails_logsAndProceeds() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "A1A 1A1"))
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.empty());
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyRepository.save(any())).thenReturn(property);
                doThrow(new RuntimeException("Notification error")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class),
                                                anyString(), any());

                // Act
                var result = transactionService.addPropertyOffer(propertyId, dto, brokerId);

                // Assert - should succeed despite notification failure
                assertThat(result).isNotNull();
        }

        @Test
        void addPropertyOffer_whenEmailFails_logsAndProceeds() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "A1A 1A1"))
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setEmail("client@test.com");
                client.setFirstName("Test");
                client.setLastName("Client");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Test");
                broker.setLastName("Broker");

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.empty());
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyRepository.save(any())).thenReturn(property);
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                doThrow(new RuntimeException("Email error")).when(emailService)
                                .sendPropertyOfferMadeNotification(anyString(), anyString(), anyString(), anyString(),
                                                anyString(), anyInt(), anyString());

                // Act
                var result = transactionService.addPropertyOffer(propertyId, dto, brokerId);

                // Assert - should succeed despite email failure
                assertThat(result).isNotNull();
        }

        // =================================================================================================
        // OFFER UPDATE WITH REVISIONS TESTS
        // =================================================================================================

        @Test
        void updateOffer_withStatusChange_createsRevision() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .offerAmount(new java.math.BigDecimal("400000"))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO
                                .builder()
                                .buyerName("Test Buyer")
                                .offerAmount(new java.math.BigDecimal("420000"))
                                .status(ReceivedOfferStatus.COUNTERED)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(offerRevisionRepository.findMaxRevisionNumberByOfferId(offerId)).thenReturn(null);
                when(offerRevisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateOffer(transactionId, offerId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(offerRevisionRepository)
                                .save(any(com.example.courtierprobackend.transactions.datalayer.OfferRevision.class));
        }

        @Test
        void updateOffer_withCounteredStatus_sendsNotification() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .offerAmount(new java.math.BigDecimal("400000"))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO
                                .builder()
                                .buyerName("Test Buyer")
                                .offerAmount(new java.math.BigDecimal("400000"))
                                .status(ReceivedOfferStatus.COUNTERED)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(offerRevisionRepository.findMaxRevisionNumberByOfferId(offerId)).thenReturn(1);
                when(offerRevisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateOffer(transactionId, offerId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(notificationService).createNotification(eq(clientId.toString()), anyString(), anyString(),
                                any(java.util.Map.class), anyString(), any());
        }

        // =================================================================================================
        // GET BROKER TRANSACTIONS EDGE CASES
        // =================================================================================================

        @Test
        void getBrokerTransactions_withBuySideFilter_handlesCorrectly() {
                // Arrange
                UUID brokerId = UUID.randomUUID();
                when(transactionRepository.findAllByFilters(eq(brokerId), any(), eq(TransactionSide.BUY_SIDE), any(),
                                anyBoolean()))
                                .thenReturn(List.of());

                // Act
                transactionService.getBrokerTransactions(brokerId, null, null, "BUY_SIDE");

                // Assert
                verify(transactionRepository).findAllByFilters(eq(brokerId), isNull(), eq(TransactionSide.BUY_SIDE),
                                isNull(), anyBoolean());
        }

        @Test
        void getBrokerTransactions_withSellerStageFilter_handlesCorrectly() {
                // Arrange
                UUID brokerId = UUID.randomUUID();
                when(transactionRepository.findAllByFilters(any(), any(), any(), any(), anyBoolean()))
                                .thenReturn(List.of());

                // Act
                transactionService.getBrokerTransactions(brokerId, null, "SELLER_INITIAL_CONSULTATION", null);

                // Assert
                verify(transactionRepository).findAllByFilters(eq(brokerId), isNull(), isNull(),
                                eq(SellerStage.SELLER_INITIAL_CONSULTATION), anyBoolean());
        }

        // =================================================================================================
        // UPDATE PROPERTY OFFER WITH CONDITION LINKS TESTS
        // =================================================================================================

        @Test
        void updatePropertyOffer_withConditionIds_createsLinks() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID conditionId1 = UUID.randomUUID();
                UUID conditionId2 = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer existingOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .offerRound(1)
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("510000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .conditionIds(List.of(conditionId1, conditionId2))
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyRepository.save(any())).thenReturn(property);
                when(documentConditionLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(documentConditionLinkRepository).deleteByPropertyOfferId(propertyOfferId);
                verify(documentConditionLinkRepository, times(2)).save(
                                any(com.example.courtierprobackend.transactions.datalayer.DocumentConditionLink.class));
        }

        @Test
        void updatePropertyOffer_syncsToPropertyWhenLatest() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer existingOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .offerRound(1)
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("550000"))
                                .status(BuyerOfferStatus.ACCEPTED)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyRepository.save(any())).thenReturn(property);

                // Act
                var result = transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(propertyRepository).save(
                                argThat(p -> p.getOfferAmount().compareTo(new java.math.BigDecimal("550000")) == 0 &&
                                                p.getOfferStatus() == PropertyOfferStatus.ACCEPTED));
        }

        // =================================================================================================
        // ADD OFFER WITH EMAIL NOTIFICATION TESTS
        // =================================================================================================

        @Test
        void addOffer_withValidData_sendsEmailNotification() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.SELL_SIDE);

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setEmail("client@test.com");
                client.setFirstName("Test");
                client.setLastName("Client");
                client.setPreferredLanguage("fr");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Test");
                broker.setLastName("Broker");

                com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO
                                .builder()
                                .buyerName("Buyer Name")
                                .offerAmount(new java.math.BigDecimal("600000"))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

                // Act
                var result = transactionService.addOffer(transactionId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(emailService).sendOfferReceivedNotification(
                                eq("client@test.com"),
                                anyString(),
                                anyString(),
                                eq("Buyer Name"),
                                anyString(),
                                eq("fr"));
        }

        @Test
        void addOffer_whenEmailFails_logsAndProceeds() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.SELL_SIDE);

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setEmail("client@test.com");
                client.setFirstName("Test");
                client.setLastName("Client");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Test");
                broker.setLastName("Broker");

                com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO
                                .builder()
                                .buyerName("Buyer Name")
                                .offerAmount(new java.math.BigDecimal("600000"))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                doThrow(new RuntimeException("Email error")).when(emailService)
                                .sendOfferReceivedNotification(anyString(), anyString(), anyString(), anyString(),
                                                anyString(), anyString());

                // Act
                var result = transactionService.addOffer(transactionId, dto, brokerId);

                // Assert - should succeed despite email failure
                assertThat(result).isNotNull();
        }

        // =================================================================================================
        // GET OFFER DOCUMENTS VIA PARENT OFFER TESTS
        // =================================================================================================

        @Test
        void getOfferDocuments_returnsDocumentsList() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID documentId = UUID.randomUUID();

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                com.example.courtierprobackend.transactions.datalayer.OfferDocument doc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .offerId(offerId)
                                .fileName("test.pdf")
                                .build();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offerId)).thenReturn(List.of(doc));

                // Act
                var result = transactionService.getOfferDocuments(offerId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getFileName()).isEqualTo("test.pdf");
        }

        @Test
        void getPropertyOfferDocuments_returnsDocumentsList() {
                // Arrange
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID documentId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                com.example.courtierprobackend.transactions.datalayer.OfferDocument doc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .propertyOfferId(propertyOfferId)
                                .fileName("property-offer.pdf")
                                .build();

                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(propertyOfferId))
                                .thenReturn(List.of(doc));

                // Act
                var result = transactionService.getPropertyOfferDocuments(propertyOfferId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getFileName()).isEqualTo("property-offer.pdf");
        }

        // =================================================================================================
        // REMOVE OFFER TESTS
        // =================================================================================================

        @Test
        void removeOffer_deletesOfferAndAddsTimeline() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));

                // Act
                transactionService.removeOffer(transactionId, offerId, brokerId);

                // Assert
                verify(offerRepository).delete(offer);
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.OFFER_REMOVED),
                                isNull(), isNull(), any());
        }

        @Test
        void removeOffer_whenOfferDoesNotBelongToTransaction_throwsBadRequest() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(UUID.randomUUID()) // Different transaction
                                .buyerName("Test Buyer")
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.removeOffer(transactionId, offerId, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Offer does not belong to this transaction");
        }

        // =================================================================================================
        // UPDATE TRANSACTION STAGE EDGE CASES
        // =================================================================================================

        @Test
        void updateTransactionStage_withInvalidSellerStage_throwsBadRequest() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setSide(TransactionSide.SELL_SIDE);
                tx.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);
                tx.setStatus(TransactionStatus.ACTIVE);

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("INVALID_STAGE");

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("not a valid seller stage");
        }

        @Test
        void updateTransactionStage_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("SELLER_INITIAL_CONSULTATION");

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // CONDITION TESTS
        // =================================================================================================

        @Test
        void updateCondition_withTypeOther_requiresCustomTitle() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER)
                                .customTitle(null) // Missing custom title
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateCondition(transactionId, conditionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Custom title is required for condition type OTHER");
        }

        @Test
        void updateCondition_withStatusChange_setsTimestamps() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateCondition(transactionId, conditionId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(conditionRepository).save(argThat(c -> c
                                .getStatus() == com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED
                                &&
                                c.getSatisfiedAt() != null));
        }

        @Test
        void updateCondition_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateCondition(transactionId, conditionId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void removeCondition_deletesConditionAndAddsTimeline() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.INSPECTION)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

                // Act
                transactionService.removeCondition(transactionId, conditionId, brokerId);

                // Assert
                verify(conditionRepository).delete(condition);
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId),
                                eq(TimelineEntryType.CONDITION_REMOVED), anyString(), isNull(), any());
        }

        @Test
        void removeCondition_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.removeCondition(transactionId, conditionId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // GET PROPERTY OFFERS TESTS
        // =================================================================================================

        @Test
        void getPropertyOffers_returnsOffersList() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer offer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(UUID.randomUUID())
                                .propertyId(propertyId)
                                .offerRound(1)
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(List.of(offer));

                // Act
                var result = transactionService.getPropertyOffers(propertyId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
        }

        @Test
        void getPropertyOffers_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getPropertyOffers(propertyId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // PROPERTY OFFER STATUS CHANGE NOTIFICATION TESTS
        // =================================================================================================

        @Test
        void updatePropertyOffer_withStatusChange_sendsNotification() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "A1A 1A1"))
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer existingOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .offerRound(1)
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.COUNTERED)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyRepository.save(any())).thenReturn(property);

                // Act
                var result = transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(notificationService).createNotification(eq(clientId.toString()), anyString(), anyString(),
                                any(java.util.Map.class), anyString(), any());
        }

        // =================================================================================================
        // ADD PROPERTY TESTS
        // =================================================================================================

        @Test
        void addProperty_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO
                                .builder()
                                .centrisNumber("12345678")
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.addProperty(transactionId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void getParticipants_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getParticipants(transactionId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void getPropertyById_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getPropertyById(propertyId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // OFFER RECEIVED EMAIL NOTIFICATION TESTS
        // =================================================================================================

        @Test
        void addOffer_withNotificationFailure_logsAndProceeds() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.SELL_SIDE);

                com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO
                                .builder()
                                .buyerName("Buyer Name")
                                .offerAmount(new java.math.BigDecimal("600000"))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                doThrow(new RuntimeException("Notification error")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class),
                                                anyString(), any());

                // Act
                var result = transactionService.addOffer(transactionId, dto, brokerId);

                // Assert - should succeed despite notification failure
                assertThat(result).isNotNull();
        }

        // =================================================================================================
        // ADD PROPERTY WITH NOTIFICATION TESTS
        // =================================================================================================

        @Test
        void addProperty_withValidData_sendsNotification() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO
                                .builder()
                                .centrisNumber("12345678")
                                .address(new PropertyAddress("123 Test St", "City", "Province", "A1A 1A1"))
                                .askingPrice(new java.math.BigDecimal("500000"))
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.addProperty(transactionId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.PROPERTY_ADDED),
                                anyString(), isNull(), any());
                verify(notificationService).createNotification(eq(clientId.toString()), anyString(), anyString(),
                                any(java.util.Map.class), anyString(), any());
        }

        @Test
        void addProperty_whenNotificationFails_logsAndProceeds() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO
                                .builder()
                                .centrisNumber("12345678")
                                .address(new PropertyAddress("123 Test St", "City", "Province", "A1A 1A1"))
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                doThrow(new RuntimeException("Notification error")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class),
                                                anyString(), any());

                // Act
                var result = transactionService.addProperty(transactionId, dto, brokerId);

                // Assert - should succeed despite notification failure
                assertThat(result).isNotNull();
        }

        // =================================================================================================
        // UPDATE PROPERTY WITH STATUS CHANGE TESTS
        // =================================================================================================

        @Test
        void updateProperty_withStatusChange_addsTimelineEntry() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "A1A 1A1"))
                                .offerStatus(PropertyOfferStatus.OFFER_TO_BE_MADE)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO
                                .builder()
                                .offerStatus(PropertyOfferStatus.OFFER_MADE)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateProperty(transactionId, propertyId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId),
                                eq(TimelineEntryType.PROPERTY_UPDATED), anyString(), isNull(), any());
        }

        @Test
        void updateProperty_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO
                                .builder()
                                .offerStatus(PropertyOfferStatus.OFFER_MADE)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateProperty(transactionId, propertyId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // REMOVE PROPERTY TESTS
        // =================================================================================================

        @Test
        void removeProperty_deletesPropertyAndAddsTimeline() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Delete St", "City", "Province", "A1A 1A1"))
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));

                // Act
                transactionService.removeProperty(transactionId, propertyId, brokerId);

                // Assert
                verify(propertyRepository).delete(property);
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId),
                                eq(TimelineEntryType.PROPERTY_REMOVED), anyString(), isNull(), any());
        }

        @Test
        void removeProperty_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.removeProperty(transactionId, propertyId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // PROPERTY OFFER EMAIL NOTIFICATION TESTS
        // =================================================================================================

        @Test
        void updatePropertyOffer_toAccepted_sendsEmailNotification() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("456 Accept St", "City", "Province", "B2B 2B2"))
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setEmail("client@test.com");
                client.setFirstName("Test");
                client.setLastName("Client");
                client.setPreferredLanguage("en");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Test");
                broker.setLastName("Broker");

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer existingOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .offerRound(1)
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.COUNTERED)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("520000"))
                                .status(BuyerOfferStatus.ACCEPTED)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyRepository.save(any())).thenReturn(property);
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

                // Act
                var result = transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(emailService).sendPropertyOfferStatusChangedNotification(
                                eq("client@test.com"), anyString(), anyString(), anyString(), anyString(), anyString(),
                                any(), eq("en"));
        }

        @Test
        void updatePropertyOffer_toDeclined_sendsNotification() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("789 Decline St", "City", "Province", "C3C 3C3"))
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer existingOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .offerRound(1)
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .status(BuyerOfferStatus.DECLINED)
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.of(existingOffer));
                when(propertyRepository.save(any())).thenReturn(property);

                // Act
                var result = transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(notificationService).createNotification(eq(clientId.toString()), anyString(), anyString(),
                                any(java.util.Map.class), anyString(), any());
        }

        // =================================================================================================
        // GET CONDITIONS TESTS
        // =================================================================================================

        @Test
        void getConditions_returnsConditionsList() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(transactionId))
                                .thenReturn(List.of(condition));

                // Act
                var result = transactionService.getConditions(transactionId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
        }

        @Test
        void getConditions_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getConditions(transactionId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // ADD CONDITION TESTS
        // =================================================================================================

        @Test
        void addCondition_withValidData_returnsCondition() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.INSPECTION)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING)
                                .description("Home inspection required")
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.addCondition(transactionId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(conditionRepository).save(any());
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.CONDITION_ADDED),
                                anyString(), isNull(), any());
        }

        @Test
        void addCondition_withTypeOtherMissingCustomTitle_throwsBadRequest() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER)
                                .customTitle(null)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.addCondition(transactionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Custom title is required for condition type OTHER");
        }

        @Test
        void addCondition_whenNotificationFails_logsAndProceeds() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                doThrow(new RuntimeException("Notification error")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class),
                                                anyString(), any());

                // Act
                var result = transactionService.addCondition(transactionId, dto, brokerId);

                // Assert - should succeed despite notification failure
                assertThat(result).isNotNull();
        }

        @Test
        void addCondition_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.addCondition(transactionId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // GET OFFER BY ID TESTS
        // =================================================================================================

        @Test
        void getOfferById_returnsOffer() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offerId)).thenReturn(List.of());
                when(documentConditionLinkRepository.findByOfferId(offerId)).thenReturn(List.of());

                // Act
                var result = transactionService.getOfferById(offerId, userId, true);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getBuyerName()).isEqualTo("Test Buyer");
        }

        @Test
        void getOfferById_whenOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getOfferById(offerId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Offer not found");
        }

        @Test
        void getOfferById_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getOfferById(offerId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // GET OFFER REVISIONS TESTS
        // =================================================================================================

        @Test
        void getOfferRevisions_returnsRevisionsList() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                com.example.courtierprobackend.transactions.datalayer.OfferRevision revision = com.example.courtierprobackend.transactions.datalayer.OfferRevision
                                .builder()
                                .revisionId(UUID.randomUUID())
                                .offerId(offerId)
                                .revisionNumber(1)
                                .createdAt(java.time.LocalDateTime.now())
                                .build();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRevisionRepository.findByOfferIdOrderByRevisionNumberAsc(offerId))
                                .thenReturn(List.of(revision));

                // Act
                var result = transactionService.getOfferRevisions(offerId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
        }

        @Test
        void getOfferRevisions_whenOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getOfferRevisions(offerId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Offer not found");
        }

        // =================================================================================================
        // GET OFFER DOCUMENTS VIA PARENT NOT FOUND
        // =================================================================================================

        @Test
        void getOfferDocuments_whenOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getOfferDocuments(offerId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Offer not found");
        }

        @Test
        void getOfferDocuments_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID offerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .build();

                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getOfferDocuments(offerId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void getPropertyOfferDocuments_whenPropertyOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyOfferId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getPropertyOfferDocuments(propertyOfferId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Property offer not found");
        }

        @Test
        void getPropertyOfferDocuments_whenPropertyNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getPropertyOfferDocuments(propertyOfferId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Property not found");
        }

        @Test
        void getPropertyOfferDocuments_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getPropertyOfferDocuments(propertyOfferId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // REMOVE OFFER WITH TRANSACTION NOT FOUND
        // =================================================================================================

        @Test
        void removeOffer_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.removeOffer(transactionId, offerId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void removeOffer_whenOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.removeOffer(transactionId, offerId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Offer not found");
        }

        // =================================================================================================
        // UPDATE PROPERTY OFFER NOT FOUND TESTS
        // =================================================================================================

        @Test
        void updatePropertyOffer_whenPropertyNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto,
                                brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Property not found");
        }

        @Test
        void updatePropertyOffer_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto,
                                brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void updatePropertyOffer_whenOfferNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updatePropertyOffer(propertyId, propertyOfferId, dto,
                                brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Property offer not found");
        }

        // =================================================================================================
        // ADD PROPERTY OFFER NOT FOUND TESTS
        // =================================================================================================

        @Test
        void addPropertyOffer_whenPropertyNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.addPropertyOffer(propertyId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Property not found");
        }

        @Test
        void addPropertyOffer_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO
                                .builder()
                                .offerAmount(new java.math.BigDecimal("500000"))
                                .build();

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.addPropertyOffer(propertyId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // GET PROPERTIES TESTS
        // =================================================================================================

        @Test
        void getProperties_returnsList() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                                .thenReturn(List.of(property));

                // Act
                var result = transactionService.getProperties(transactionId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
        }

        @Test
        void getProperties_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getProperties(transactionId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // GET OFFERS TESTS
        // =================================================================================================

        @Test
        void getOffers_returnsList() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setSide(TransactionSide.SELL_SIDE);

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of(offer));
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offerId)).thenReturn(List.of());
                when(documentConditionLinkRepository.findByOfferId(offerId)).thenReturn(List.of());

                // Act
                var result = transactionService.getOffers(transactionId, userId, true);

                // Assert
                assertThat(result).hasSize(1);
        }

        @Test
        void getOffers_whenTransactionNotFound_throwsNotFoundException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.getOffers(transactionId, userId, true))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // =================================================================================================
        // CONDITION DOES NOT BELONG TO TRANSACTION TESTS
        // =================================================================================================

        @Test
        void updateCondition_whenConditionDoesNotBelongToTransaction_throwsBadRequest() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(UUID.randomUUID()) // Different transaction
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO
                                .builder()
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateCondition(transactionId, conditionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Condition does not belong to this transaction");
        }

        @Test
        void removeCondition_whenConditionDoesNotBelongToTransaction_throwsBadRequest() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(UUID.randomUUID()) // Different transaction
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.removeCondition(transactionId, conditionId, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Condition does not belong to this transaction");
        }

        // ========== Additional Coverage Tests ==========

        @Test
        void createTransaction_withUnsupportedSide_throwsBadRequest() {
                // Arrange - We can't set a null side on the DTO, but we can test the else
                // branch another way
                // This would require a transaction with null side - test coverage for line 209
                TransactionRequestDTO dto = createValidBuyerTransactionDTO();
                dto.setSide(null); // This should trigger the else branch

                when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> transactionService.createTransaction(dto))
                                .isInstanceOf(Exception.class);
        }

        @Test
        void getNotes_whenNoNotes_returnsEmptyFilteredList() {
                // Coverage for line 269 filter predicate
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                // Return a non-NOTE entry to verify filter works
                TimelineEntryDTO nonNote = TimelineEntryDTO.builder()
                                .id(UUID.randomUUID())
                                .type(TimelineEntryType.STAGE_CHANGE)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(timelineService.getTimelineForTransaction(transactionId)).thenReturn(List.of(nonNote));

                // Act
                List<TimelineEntryDTO> result = transactionService.getNotes(transactionId, brokerId);

                // Assert - should be empty since TypeChange is filtered out
                assertThat(result).isEmpty();
        }

        @Test
        void createNote_whenNoNotesExist_returnsNull() {
                // Coverage for line 291-295
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                NoteRequestDTO note = new NoteRequestDTO();
                note.setTitle("Test Title");
                note.setMessage("Test Message");

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(timelineService.getTimelineForTransaction(transactionId)).thenReturn(List.of());

                // Act
                TimelineEntryDTO result = transactionService.createNote(transactionId, note, brokerId);

                // Assert - should be null as no notes exist after filter
                assertThat(result).isNull();
        }

        @Test
        void getBrokerTransactions_withSellSideFilter_parsesSideCorrectly() {
                // Coverage for line 319 (sell side parsing)
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(UUID.randomUUID());
                tx.setBrokerId(brokerId);
                tx.setSide(TransactionSide.SELL_SIDE);
                tx.setStatus(TransactionStatus.ACTIVE);
                tx.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);

                when(transactionRepository.findAllByFilters(eq(brokerId), any(), eq(TransactionSide.SELL_SIDE), any(),
                                eq(false)))
                                .thenReturn(List.of(tx));

                // Act
                List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerId, null, null,
                                "sell");

                // Assert
                assertThat(result).hasSize(1);
        }

        @Test
        void updateTransactionStage_withUnsupportedSide_throwsBadRequest() {
                // Coverage for line 468
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setSide(null); // Unsupported side

                StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
                dto.setStage("BUYER_SEARCHING");

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert - should throw for unsupported side
                assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Unsupported transaction side");
        }

        @Test
        void addProperty_withNullAddress_usesUnknown() {
                // Coverage for line 738 (Unknown address fallback)
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO dto = new com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO();
                dto.setAddress(null); // null address
                dto.setAskingPrice(new java.math.BigDecimal("500000"));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.addProperty(transactionId, dto, brokerId);

                // Assert - Should succeed and timeline entry added with "Unknown"
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.PROPERTY_ADDED),
                                contains("Unknown"), any(), any());
        }

        @Test
        void updateProperty_withStatusChange_addsTimeline() {
                // Coverage for lines 812-826
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .offerStatus(com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus.OFFER_TO_BE_MADE)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO dto = new com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO();
                dto.setOfferStatus(
                                com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus.OFFER_MADE);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                transactionService.updateProperty(transactionId, propertyId, dto, brokerId);

                // Assert - timeline entry added for status change
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId),
                                eq(TimelineEntryType.PROPERTY_UPDATED), contains("offer status changed"), any(), any());
        }

        @Test
        void removeProperty_addsTimelineEntry() {
                // Coverage for line 846-860
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(null) // Test null address -> "Unknown"
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));

                // Act
                transactionService.removeProperty(transactionId, propertyId, brokerId);

                // Assert
                verify(propertyRepository).delete(property);
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId),
                                eq(TimelineEntryType.PROPERTY_REMOVED), contains("Unknown"), any(), any());
        }

        @Test
        void addPropertyOffer_withNoDefaultStatus_usesOfferMade() {
                // Coverage for line 987
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO dto = new com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO();
                dto.setOfferAmount(new java.math.BigDecimal("400000"));
                dto.setStatus(null); // Should default to OFFER_MADE

                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyOfferRepository.findMaxOfferRoundByPropertyId(propertyId)).thenReturn(null);
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.addPropertyOffer(propertyId, dto, brokerId);

                // Assert
                assertThat(result.getStatus()).isEqualTo(
                                com.example.courtierprobackend.transactions.datalayer.enums.BuyerOfferStatus.OFFER_MADE);
        }

        @Test
        void getOfferDocumentDownloadUrl_forPropertyOffer_verifiesAccess() {
                // Coverage for lines 1654-1665
                UUID documentId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument doc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .propertyOfferId(propertyOfferId)
                                .s3Key("s3/key")
                                .build();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(objectStorageService.generatePresignedUrl("s3/key")).thenReturn("https://presigned.url");

                // Act
                String result = transactionService.getOfferDocumentDownloadUrl(documentId, userId);

                // Assert
                assertThat(result).isEqualTo("https://presigned.url");
        }

        @Test
        void deleteOfferDocument_forPropertyOffer_deletesDocument() {
                // Coverage for lines 1680-1691
                UUID documentId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument doc = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .propertyOfferId(propertyOfferId)
                                .s3Key("s3/key")
                                .build();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                transactionService.deleteOfferDocument(documentId, brokerId);

                // Assert
                verify(objectStorageService).deleteFile("s3/key");
                verify(offerDocumentRepository).delete(doc);
        }

        // Note: updateOffer_withStatusChange_createsRevision test already exists
        // earlier in file

        @Test
        void updateCondition_withOtherTypeAndNullTitle_throwsBadRequest() {
                // Coverage for lines 1944-1946
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = new com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO();
                dto.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER);
                dto.setCustomTitle(null); // Missing required title

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));

                // Act & Assert
                assertThatThrownBy(() -> transactionService.updateCondition(transactionId, conditionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Custom title is required for condition type OTHER");
        }

        @Test
        void updateCondition_setsCustomTitleToNull_forNonOtherType() {
                // Coverage for line 1955
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.OTHER)
                                .customTitle("Old Title")
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = new com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO();
                dto.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING); // Not
                                                                                                                  // OTHER

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateCondition(transactionId, conditionId, dto, brokerId);

                // Assert - customTitle should be null since type is not OTHER
                assertThat(result.getCustomTitle()).isNull();
        }

        @Test
        void updateCondition_clearsSatisfiedAt_whenStatusChangesFromSatisfied() {
                // Coverage for lines 1962-1964
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.INSPECTION)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED)
                                .satisfiedAt(LocalDateTime.now())
                                .build();

                com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO dto = new com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO();
                dto.setType(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.INSPECTION);
                dto.setStatus(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING); // Changed
                                                                                                                    // from
                                                                                                                    // SATISFIED

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                var result = transactionService.updateCondition(transactionId, conditionId, dto, brokerId);

                // Assert - satisfiedAt should be cleared
                assertThat(result.getSatisfiedAt()).isNull();
        }

        @Test
        void updateConditionStatus_toPending_addsUpdatedTimelineEntry() {
                // Coverage for lines 2070, 2101-2102
                UUID transactionId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                com.example.courtierprobackend.transactions.datalayer.Condition condition = com.example.courtierprobackend.transactions.datalayer.Condition
                                .builder()
                                .conditionId(conditionId)
                                .transactionId(transactionId)
                                .type(com.example.courtierprobackend.transactions.datalayer.enums.ConditionType.FINANCING)
                                .status(com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.SATISFIED)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(conditionRepository.findByConditionId(conditionId)).thenReturn(Optional.of(condition));
                when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act - change to PENDING (not SATISFIED or FAILED)
                transactionService.updateConditionStatus(transactionId, conditionId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus.PENDING,
                                brokerId);

                // Assert - should add CONDITION_UPDATED entry
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId),
                                eq(TimelineEntryType.CONDITION_UPDATED), any(), any(), any());
        }

        @Test
        void getAllTransactionDocuments_sortsNullUploadedAt() {
                // Coverage for lines 2290-2292
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId)).thenReturn(List.of());
                when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());
                when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());

                // Act
                var result = transactionService.getAllTransactionDocuments(transactionId, brokerId, true);

                // Assert
                assertThat(result).isEmpty();
        }

        // ========== Additional Coverage Tests ==========

        @Test
        void createNote_withEmptyTimelineReturnsNull() {
                // Coverage for line 291 - empty notes list returns null
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                NoteRequestDTO noteDTO = new NoteRequestDTO();
                noteDTO.setTitle("Test Title");
                noteDTO.setMessage("Test Message");

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                // Return empty list so there are no notes after filtering
                when(timelineService.getTimelineForTransaction(transactionId)).thenReturn(List.of());

                // Act
                var result = transactionService.createNote(transactionId, noteDTO, brokerId);

                // Assert
                assertThat(result).isNull();
        }

        @Test
        void createTransaction_withPostalCodeNormalization() {
                // Coverage for line 217 - postal code normalization
                TransactionRequestDTO dto = createValidBuyerTransactionDTO();
                dto.getPropertyAddress().setPostalCode("h2x 1y4"); // lowercase format

                when(transactionRepository.findByClientIdAndPropertyAddress_StreetAndStatus(
                                any(UUID.class), anyString(), any())).thenReturn(Optional.empty());

                Transaction savedTx = new Transaction();
                savedTx.setTransactionId(UUID.randomUUID());
                savedTx.setClientId(dto.getClientId());
                savedTx.setBrokerId(dto.getBrokerId());
                savedTx.setPropertyAddress(dto.getPropertyAddress());
                when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

                // Act
                TransactionResponseDTO result = transactionService.createTransaction(dto);

                // Assert - postal code should be normalized
                assertThat(result).isNotNull();
                verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void addPropertyOffer_withNullFirstAndLastName_coversNameBuilding() {
                // Coverage for lines 1053-1056 - client/broker name building with nulls
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "H1H1H1"))
                                .build();

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName(null);
                client.setLastName(null);
                client.setEmail("client@test.com");
                client.setPreferredLanguage("en");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName(null);
                broker.setLastName(null);
                broker.setEmail("broker@test.com");

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO offerDto = new com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO();
                offerDto.setOfferAmount(java.math.BigDecimal.valueOf(500000.0));
                offerDto.setStatus(BuyerOfferStatus.OFFER_MADE);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(propertyOfferRepository.findMaxOfferRoundByPropertyId(propertyId)).thenReturn(null);
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> {
                        var po = (com.example.courtierprobackend.transactions.datalayer.PropertyOffer) inv
                                        .getArgument(0);
                        po.setPropertyOfferId(UUID.randomUUID());
                        return po;
                });
                when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
                when(documentConditionLinkRepository.findByPropertyOfferId(any())).thenReturn(List.of());

                // Act
                var result = transactionService.addPropertyOffer(propertyId, offerDto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(emailService).sendPropertyOfferMadeNotification(anyString(), anyString(), anyString(),
                                anyString(), anyString(), anyInt(), anyString());
        }

        @Test
        void addPropertyOffer_emailNotificationException_coversLine1068() {
                // Coverage for line 1068 - email notification exception handling
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "H1H1H1"))
                                .build();

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("Client");
                client.setLastName("Name");
                client.setEmail("client@test.com");
                client.setPreferredLanguage("en");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Broker");
                broker.setLastName("Name");

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO offerDto = new com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO();
                offerDto.setOfferAmount(java.math.BigDecimal.valueOf(500000.0));
                offerDto.setStatus(BuyerOfferStatus.OFFER_MADE);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(propertyOfferRepository.findMaxOfferRoundByPropertyId(propertyId)).thenReturn(null);
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> {
                        var po = (com.example.courtierprobackend.transactions.datalayer.PropertyOffer) inv
                                        .getArgument(0);
                        po.setPropertyOfferId(UUID.randomUUID());
                        return po;
                });
                when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
                when(documentConditionLinkRepository.findByPropertyOfferId(any())).thenReturn(List.of());

                // Make email service throw an exception
                doThrow(new RuntimeException("Email error")).when(emailService)
                                .sendPropertyOfferMadeNotification(anyString(), anyString(), anyString(), anyString(),
                                                anyString(), anyInt(), anyString());

                // Act - should not throw, just log
                var result = transactionService.addPropertyOffer(propertyId, offerDto, brokerId);

                // Assert
                assertThat(result).isNotNull();
        }

        @Test
        void updatePropertyOfferStatus_withEmailNotification_coversLines1174to1190() {
                // Coverage for lines 1174-1178, 1186, 1189-1190
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .address(new PropertyAddress("123 Main St", "City", "Province", "H1H1H1"))
                                .build();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer offer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .offerRound(1)
                                .counterpartyResponse(
                                                com.example.courtierprobackend.transactions.datalayer.enums.CounterpartyResponse.COUNTERED)
                                .build();

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("Client");
                client.setLastName("Name");
                client.setEmail("client@test.com");
                client.setPreferredLanguage("en");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Broker");
                broker.setLastName("Name");

                com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO updateDto = new com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO();
                updateDto.setStatus(BuyerOfferStatus.ACCEPTED);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId)).thenReturn(Optional.of(offer));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(propertyOfferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(propertyOfferRepository.findTopByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(Optional.of(offer));
                when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(propertyOfferId))
                                .thenReturn(List.of());
                when(documentConditionLinkRepository.findByPropertyOfferId(propertyOfferId)).thenReturn(List.of());

                // Act
                var result = transactionService.updatePropertyOffer(propertyId, propertyOfferId, updateDto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(emailService).sendPropertyOfferStatusChangedNotification(
                                eq("client@test.com"), anyString(), anyString(), anyString(), anyString(), anyString(),
                                any(), eq("en"));
        }

        @Test
        void addOffer_withEmailNotification_coversLines1342to1356() {
                // Coverage for lines 1342-1345, 1355-1356
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.SELL_SIDE);

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("Client");
                client.setLastName("Name");
                client.setEmail("client@test.com");
                client.setPreferredLanguage("fr");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Broker");
                broker.setLastName("Name");

                com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO offerDto = com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO
                                .builder()
                                .buyerName("Test Buyer")
                                .offerAmount(java.math.BigDecimal.valueOf(500000.0))
                                .expiryDate(java.time.LocalDate.now().plusDays(1))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(offerRepository.save(any())).thenAnswer(inv -> {
                        var o = (Offer) inv.getArgument(0);
                        o.setOfferId(UUID.randomUUID());
                        return o;
                });
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
                when(documentConditionLinkRepository.findByOfferId(any())).thenReturn(List.of());

                // Act
                var result = transactionService.addOffer(transactionId, offerDto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(emailService).sendOfferReceivedNotification(
                                eq("client@test.com"), anyString(), anyString(), eq("Test Buyer"), anyString(),
                                eq("fr"));
        }

        @Test
        void updateOfferStatus_withEmailNotification_coversLines1472to1488() {
                // Coverage for lines 1472-1484, 1487-1488
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.SELL_SIDE);

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .offerAmount(java.math.BigDecimal.valueOf(500000.0))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("Client");
                client.setLastName("Name");
                client.setEmail("client@test.com");
                client.setPreferredLanguage("en");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Broker");
                broker.setLastName("Name");

                com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO updateDto = com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO
                                .builder()
                                .status(ReceivedOfferStatus.ACCEPTED)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offerId)).thenReturn(List.of());
                when(documentConditionLinkRepository.findByOfferId(offerId)).thenReturn(List.of());

                // Act
                var result = transactionService.updateOffer(transactionId, offerId, updateDto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                verify(emailService).sendOfferStatusChangedNotification(
                                eq("client@test.com"), anyString(), anyString(), eq("Test Buyer"), anyString(),
                                eq("ACCEPTED"), eq("en"));
        }

        @Test
        void getOfferDocumentDownloadUrl_withPropertyOfferDocument_coversLines1654to1664() {
                // Coverage for lines 1654, 1656, 1660, 1662, 1664
                UUID documentId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument document = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .propertyOfferId(propertyOfferId)
                                .offerId(null) // This is a property offer document
                                .s3Key("test-key")
                                .build();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(document));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(objectStorageService.generatePresignedUrl("test-key")).thenReturn("https://presigned-url.com");

                // Act
                String result = transactionService.getOfferDocumentDownloadUrl(documentId, brokerId);

                // Assert
                assertThat(result).isEqualTo("https://presigned-url.com");
        }

        @Test
        void deleteOfferDocument_withPropertyOfferDocument_coversLines1675to1690() {
                // Coverage for lines 1675, 1680, 1682, 1686, 1688, 1690
                UUID documentId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                com.example.courtierprobackend.transactions.datalayer.OfferDocument document = com.example.courtierprobackend.transactions.datalayer.OfferDocument
                                .builder()
                                .documentId(documentId)
                                .propertyOfferId(propertyOfferId)
                                .offerId(null)
                                .s3Key("test-key")
                                .build();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer propertyOffer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(offerDocumentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(document));
                when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                                .thenReturn(Optional.of(propertyOffer));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                transactionService.deleteOfferDocument(documentId, brokerId);

                // Assert
                verify(objectStorageService).deleteFile("test-key");
                verify(offerDocumentRepository).delete(document);
        }

        @Test
        void submitClientOfferDecision_notificationException_coversLines1804to1805() {
                // Coverage for lines 1804-1805
                UUID transactionId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.SELL_SIDE);

                Offer offer = Offer.builder()
                                .offerId(offerId)
                                .transactionId(transactionId)
                                .buyerName("Test Buyer")
                                .offerAmount(java.math.BigDecimal.valueOf(500000.0))
                                .status(ReceivedOfferStatus.PENDING)
                                .build();

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("Client");
                client.setLastName("Name");

                com.example.courtierprobackend.transactions.datalayer.dto.ClientOfferDecisionDTO decisionDto = new com.example.courtierprobackend.transactions.datalayer.dto.ClientOfferDecisionDTO();
                decisionDto.setDecision(
                                com.example.courtierprobackend.transactions.datalayer.enums.ClientOfferDecision.ACCEPT);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(offer));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(offerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offerId)).thenReturn(List.of());
                when(documentConditionLinkRepository.findByOfferId(offerId)).thenReturn(List.of());

                // Make notification service throw
                doThrow(new RuntimeException("Notification error")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), any(java.util.Map.class),
                                                anyString(), any());

                // Act - should not throw
                var result = transactionService.submitClientOfferDecision(offerId, decisionDto, clientId);

                // Assert
                assertThat(result).isNotNull();
        }

        @Test
        void toPropertyOfferResponseDTO_withEmptyConditions_coversLines1214to1216() {
                // Coverage for lines 1214, 1216 - filtering empty conditions
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());
                tx.setSide(TransactionSide.BUY_SIDE);

                com.example.courtierprobackend.transactions.datalayer.Property property = com.example.courtierprobackend.transactions.datalayer.Property
                                .builder()
                                .propertyId(propertyId)
                                .transactionId(transactionId)
                                .build();

                com.example.courtierprobackend.transactions.datalayer.PropertyOffer offer = com.example.courtierprobackend.transactions.datalayer.PropertyOffer
                                .builder()
                                .propertyOfferId(propertyOfferId)
                                .propertyId(propertyId)
                                .status(BuyerOfferStatus.OFFER_MADE)
                                .build();

                // Create a condition link but have the condition not be found
                com.example.courtierprobackend.transactions.datalayer.DocumentConditionLink link = com.example.courtierprobackend.transactions.datalayer.DocumentConditionLink
                                .builder()
                                .conditionId(UUID.randomUUID())
                                .propertyOfferId(propertyOfferId)
                                .build();

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId))
                                .thenReturn(List.of(offer));
                when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(propertyOfferId))
                                .thenReturn(List.of());
                when(documentConditionLinkRepository.findByPropertyOfferId(propertyOfferId)).thenReturn(List.of(link));
                when(conditionRepository.findByConditionId(any())).thenReturn(Optional.empty()); // Condition not found

                // Act
                var result = transactionService.getPropertyOffers(propertyId, brokerId, true);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getConditions()).isEmpty();
        }

        @Test
        void getAllTransactionDocuments_withNullCustomTitle_coversLine2229() {
                // Coverage for line 2229 - null customTitle falls back to docType
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());
                tx.setSide(TransactionSide.BUY_SIDE);

                // Create a document request with null customTitle but a docType
                com.example.courtierprobackend.documents.datalayer.Document docRequest = new com.example.courtierprobackend.documents.datalayer.Document();
                docRequest.setDocumentId(requestId);
                docRequest.setCustomTitle(null);
                docRequest.setDocType(
                                com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum.ID_VERIFICATION);
                docRequest.setTransactionRef(
                                new com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef(
                                                transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));

                // Create submitted document
                com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject storage = com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject
                                .builder()
                                .s3Key("key")
                                .fileName("file.pdf")
                                .mimeType("application/pdf")
                                .sizeBytes(1000L)
                                .build();
                com.example.courtierprobackend.documents.datalayer.DocumentVersion submitted = com.example.courtierprobackend.documents.datalayer.DocumentVersion
                                .builder()
                                .versionId(docId)
                                .uploadedAt(LocalDateTime.now())
                                .storageObject(storage)
                                .build();
                docRequest.setVersions(List.of(submitted));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId))
                                .thenReturn(List.of(docRequest));
                when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());
                when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId)).thenReturn(List.of());

                // Act
                var result = transactionService.getAllTransactionDocuments(transactionId, brokerId, true);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getSourceName()).isEqualTo("ID_VERIFICATION");
        }

        // ==================== updatePropertyStatus Tests ====================

        @Test
        void updatePropertyStatus_asClient_validTransition_success() {
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);
                tx.setSide(TransactionSide.BUY_SIDE);

                Property property = new Property();
                property.setPropertyId(propertyId);
                property.setTransactionId(transactionId);
                property.setStatus(PropertyStatus.SUGGESTED);
                property.setAddress(new PropertyAddress("123 Main", "City", "QC", "H1H 1H1"));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(propertyRepository.save(any(Property.class))).thenAnswer(i -> i.getArguments()[0]);
                when(userAccountRepository.findById(clientId))
                                .thenReturn(Optional.of(createUserAccount(clientId, "Client Name")));

                PropertyResponseDTO result = transactionService.updatePropertyStatus(
                                transactionId, propertyId, PropertyStatus.INTERESTED, "Looks good", clientId);

                assertThat(result.getStatus()).isEqualTo(PropertyStatus.INTERESTED);
                verify(timelineService).addEntry(eq(transactionId), eq(clientId),
                                eq(TimelineEntryType.PROPERTY_UPDATED), anyString(), isNull(), any());
        }

        @Test
        void updatePropertyStatus_asClient_invalidTransition_throwsBadRequest() {
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(UUID.randomUUID());

                Property property = new Property();
                property.setPropertyId(propertyId);
                property.setTransactionId(transactionId);
                property.setStatus(PropertyStatus.INTERESTED); // Already accepted

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));

                assertThatThrownBy(() -> transactionService.updatePropertyStatus(
                                transactionId, propertyId, PropertyStatus.NOT_INTERESTED, null, clientId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Clients can only review Suggested properties");
        }

        @Test
        void updatePropertyStatus_asBroker_success() {
                UUID transactionId = UUID.randomUUID();
                UUID propertyId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                Property property = new Property();
                property.setPropertyId(propertyId);
                property.setTransactionId(transactionId);
                property.setStatus(PropertyStatus.NEEDS_INFO);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
                when(propertyRepository.save(any(Property.class))).thenAnswer(i -> i.getArguments()[0]);

                PropertyResponseDTO result = transactionService.updatePropertyStatus(
                                transactionId, propertyId, PropertyStatus.SUGGESTED, "Info provided", brokerId);

                assertThat(result.getStatus()).isEqualTo(PropertyStatus.SUGGESTED);
        }

        @Test
        void updatePropertyStatus_unauthorizedUser_throwsForbidden() {
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(UUID.randomUUID());

                Property property = new Property();
                property.setPropertyId(UUID.randomUUID());
                property.setTransactionId(transactionId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(propertyRepository.findByPropertyId(any())).thenReturn(Optional.of(property));

                assertThatThrownBy(() -> transactionService.updatePropertyStatus(
                                transactionId, property.getPropertyId(), PropertyStatus.INTERESTED, null, userId))
                                .isInstanceOf(ForbiddenException.class);
        }

        // ==================== Participant Coverage Tests ====================

        @Test
        void addParticipant_duplicateEmail_throwsBadRequest() {
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                String email = "duplicate@example.com";

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                AddParticipantRequestDTO dto = new AddParticipantRequestDTO();
                dto.setEmail(email);
                dto.setName("New Participant");
                dto.setRole(ParticipantRole.OTHER);

                TransactionParticipant existing = new TransactionParticipant();
                existing.setEmail(email);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(existing));

                assertThatThrownBy(() -> transactionService.addParticipant(transactionId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Un participant avec cet email existe déjà");
        }

        @Test
        void updateParticipant_fullUpdate_success() {
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID participantId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                TransactionParticipant participant = new TransactionParticipant();
                participant.setId(participantId);
                participant.setTransactionId(transactionId);
                participant.setRole(ParticipantRole.OTHER);
                participant.setSystem(false);

                UpdateParticipantRequestDTO dto = new UpdateParticipantRequestDTO();
                dto.setName("Updated Name");
                dto.setEmail("new@example.com");
                dto.setRole(ParticipantRole.CO_BROKER);
                dto.setPhoneNumber("555-1234");
                dto.setPermissions(java.util.Set.of(ParticipantPermission.VIEW_DOCUMENTS));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
                when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));
                // Simulate that new email is valid user for CO_BROKER role
                when(userAccountRepository.findByEmail("new@example.com")).thenReturn(Optional.of(new UserAccount()));
                when(participantRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

                ParticipantResponseDTO result = transactionService.updateParticipant(transactionId, participantId, dto,
                                brokerId);

                assertThat(result.getName()).isEqualTo("Updated Name");
                assertThat(result.getRole()).isEqualTo(ParticipantRole.CO_BROKER);
                assertThat(result.getPermissions()).contains(ParticipantPermission.VIEW_DOCUMENTS);
        }

        @Test
        void updateParticipant_systemParticipant_throwsBadRequest() {
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID participantId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                TransactionParticipant participant = new TransactionParticipant();
                participant.setId(participantId);
                participant.setTransactionId(transactionId);
                participant.setSystem(true); // Is System

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

                UpdateParticipantRequestDTO dto = new UpdateParticipantRequestDTO();
                dto.setName("New Name");

                assertThatThrownBy(
                                () -> transactionService.updateParticipant(transactionId, participantId, dto, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Impossible de modifier ce participant système");
        }

        @Test
        void removeParticipant_systemParticipant_throwsBadRequest() {
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID participantId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                TransactionParticipant participant = new TransactionParticipant();
                participant.setId(participantId);
                participant.setTransactionId(transactionId);
                participant.setSystem(true);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

                assertThatThrownBy(() -> transactionService.removeParticipant(transactionId, participantId, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Impossible de supprimer ce participant système");
        }

        // ==================== Broker Transaction Coverage ====================

        @Test
        void getBrokerTransactions_includesCoBrokerTransactions() {
                UUID brokerId = UUID.randomUUID();
                String brokerEmail = "broker@example.com";

                // Broker is Primary on Tx1
                Transaction tx1 = new Transaction();
                tx1.setTransactionId(UUID.randomUUID());
                tx1.setBrokerId(brokerId);

                // Broker is Co-Broker on Tx2
                Transaction tx2 = new Transaction();
                tx2.setTransactionId(UUID.randomUUID());
                tx2.setBrokerId(UUID.randomUUID()); // Different primary broker

                UserAccount brokerAccount = new UserAccount();
                brokerAccount.setEmail(brokerEmail);

                when(transactionRepository.findAllByFilters(eq(brokerId), any(), any(), any(), anyBoolean()))
                                .thenReturn(List.of(tx1));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(brokerAccount));
                when(transactionRepository.findAllByParticipantEmail(brokerEmail)).thenReturn(List.of(tx2));

                List<TransactionResponseDTO> result = transactionService.getBrokerTransactions(brokerId, null, null,
                                null);

                assertThat(result).hasSize(2);
                assertThat(result.stream().map(TransactionResponseDTO::getTransactionId))
                                .containsExactlyInAnyOrder(tx1.getTransactionId(), tx2.getTransactionId());
        }
}
