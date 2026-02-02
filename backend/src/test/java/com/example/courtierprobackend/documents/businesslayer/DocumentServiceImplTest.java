package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.documents.datalayer.DocumentVersion;
import com.example.courtierprobackend.documents.datalayer.enums.*;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.OutstandingDocumentDTO;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {
        @Test
        void updateDocumentRequest_WithClientAndBroker_SendsNotificationAndEmail() {
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setTransactionId(UUID.randomUUID());
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(tx.getTransactionId(), clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("Notes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT); // trigger update

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);
                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");

                service.updateDocument(requestId, dto, brokerId);

                verify(emailService, atLeastOnce()).sendDocumentEditedNotification(any(), any(), any(), any(), any(),
                                any());
                verify(notificationService, atLeastOnce()).createNotification(any(), any(), any(), any(), any());
        }

        @Test
        void updateDocumentRequest_WithMissingClient_SkipsNotificationAndEmail() {
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setTransactionId(UUID.randomUUID());
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(tx.getTransactionId(), clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT); // trigger update

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.empty());
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");

                service.updateDocument(requestId, dto, brokerId);

                verify(emailService, never()).sendDocumentEditedNotification(any(), any(), any(), any(), any(), any());
                verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }

        @Test
        void updateDocumentRequest_WithMissingBroker_SkipsNotificationAndEmail() {
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setTransactionId(UUID.randomUUID());
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(tx.getTransactionId(), clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT); // trigger update

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.empty());
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");

                service.updateDocument(requestId, dto, brokerId);

                verify(emailService, never()).sendDocumentEditedNotification(any(), any(), any(), any(), any(), any());
                verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }

        @Test
        void updateDocumentRequest_WhenDtoFieldsNullOrEmpty_FallsBackToRequestFields() {
                UUID requestId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("OriginalTitle");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(Boolean.TRUE);
                request.setBrokerNotes("OriginalNotes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(null); // Should fallback to request
                dto.setCustomTitle(""); // Should fallback to request
                dto.setExpectedFrom(null); // Should fallback to request
                dto.setVisibleToClient(null); // Should fallback to request
                dto.setBrokerNotes(""); // Should fallback to request
                dto.setStage(null); // Should fallback to request

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));

                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.getDocType()).isEqualTo(request.getDocType());
                assertThat(result.getCustomTitle()).isEqualTo(request.getCustomTitle());
                assertThat(result.getExpectedFrom()).isEqualTo(request.getExpectedFrom());
                assertThat(result.isVisibleToClient()).isEqualTo(request.getVisibleToClient());
                assertThat(result.getBrokerNotes()).isEqualTo(request.getBrokerNotes());
                assertThat(result.getStage()).isEqualTo(request.getStage());
        }

        @Mock
        private DocumentRepository repository;
        @Mock
        private S3StorageService storageService;
        @Mock
        private EmailService emailService;
        @Mock
        private TransactionRepository transactionRepository;
        @Mock
        private UserAccountRepository userAccountRepository;
        @Mock
        private com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;
        @Mock
        private org.springframework.context.MessageSource messageSource;

        @Mock
        TimelineService timelineService;

        @Mock
        private com.example.courtierprobackend.transactions.datalayer.repositories.DocumentConditionLinkRepository documentConditionLinkRepository;
        @Mock
        private TransactionParticipantRepository participantRepository;

        private DocumentServiceImpl service;

        @BeforeEach
        void setUp() {
                service = new DocumentServiceImpl(repository, storageService, emailService, notificationService,
                                transactionRepository, userAccountRepository, timelineService, messageSource,
                                documentConditionLinkRepository, participantRepository);
        }

        // ========== getDocumentsForTransaction Tests ==========

        @Test
        void getDocumentsForTransaction_WithValidAccess_ReturnsDocuments() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setClientId(UUID.randomUUID());

                UUID requestId = UUID.randomUUID();
                Document doc = new Document();
                doc.setDocumentId(requestId);
                doc.setTransactionRef(new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                doc.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                doc.setStatus(DocumentStatusEnum.REQUESTED);
                doc.setVersions(new ArrayList<>());

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(repository.findByTransactionRef_TransactionId(transactionId)).thenReturn(List.of(doc));

                // Act
                List<DocumentResponseDTO> result = service.getDocumentsForTransaction(transactionId, userId);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getDocumentId()).isEqualTo(requestId);
        }

        @Test
        void getDocumentsForTransaction_WithNoAccess_ThrowsForbiddenException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> service.getDocumentsForTransaction(transactionId, userId))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have access");
        }

        // ========== submitDocument Tests ==========

        @Test
        void submitDocument_WithValidData_UploadsAndNotifies() throws IOException {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID uploaderId = clientId;

                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Pay Stub");
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setVersions(new ArrayList<>());

                String brokerAuth0Id = UUID.randomUUID().toString();
                String clientAuth0Id = UUID.randomUUID().toString();

                UserAccount broker = new UserAccount(brokerAuth0Id, "broker@test.com", "Jane", "Smith", UserRole.BROKER,
                                "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(clientAuth0Id, "client@test.com", "John", "Doe", UserRole.CLIENT,
                                "en");
                client.setId(clientId);

                MockMultipartFile file = new MockMultipartFile("file", "paystub.pdf", "application/pdf",
                                "content".getBytes());
                StorageObject storageObject = StorageObject.builder().s3Key("key").fileName("test.pdf").build();

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(storageService.uploadFile(any(), eq(transactionId), eq(requestId))).thenReturn(storageObject);
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(eq("notification.document.submitted.title"), any(),
                                any(java.util.Locale.class)))
                                .thenReturn("Document Submitted");
                when(messageSource.getMessage(eq("notification.document.submitted.message"), any(),
                                any(java.util.Locale.class)))
                                .thenReturn("Document submitted message");
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");

                // Act
                DocumentResponseDTO result = service.submitDocument(transactionId, requestId, file, uploaderId,
                                UploadedByRefEnum.CLIENT);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.SUBMITTED);
                verify(emailService).sendDocumentSubmittedNotification(any(), eq("broker@test.com"), eq("John Doe"),
                                anyString(), anyString(), anyString());
                verify(notificationService).createNotification(
                                eq(broker.getId().toString()),
                                anyString(),
                                anyString(),
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_SUBMITTED));
        }

        @Test
        void submitDocument_NotificationFailure_LogsAndProceeds() throws IOException {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                request.setVersions(new ArrayList<>());

                UserAccount broker = new UserAccount(UUID.randomUUID().toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(UUID.randomUUID().toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);

                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                                "content".getBytes());
                StorageObject storageObject = StorageObject.builder().s3Key("key").fileName("test.pdf").build();

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(storageService.uploadFile(any(), eq(transactionId), eq(requestId))).thenReturn(storageObject);
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                // Simulate Notification Exception
                doThrow(new RuntimeException("Notification failed")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), anyString(), any());

                // Act
                DocumentResponseDTO result = service.submitDocument(transactionId, requestId, file, clientId,
                                UploadedByRefEnum.CLIENT);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.SUBMITTED);
        }

        @Test
        void submitDocument_WithMismatchedTransaction_ThrowsBadRequestException() throws IOException {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                                "content".getBytes());

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));

                // Act & Assert
                assertThatThrownBy(() -> service.submitDocument(transactionId, requestId, file, UUID.randomUUID(),
                                UploadedByRefEnum.CLIENT))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("does not belong to transaction");
        }

        // ========== reviewDocument Tests ==========

        @Test
        void reviewDocument_Approve_AllowsOptionalCommentAndSendsEmail() {
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                request.setCustomTitle("Bank Statement");
                request.setStatus(DocumentStatusEnum.SUBMITTED);

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Broker",
                                UserRole.BROKER,
                                "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Client",
                                UserRole.CLIENT,
                                "en");
                client.setId(clientId);

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(DocumentStatusEnum.APPROVED);
                reviewDTO.setComments(null); // optional on approval

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc");
                when(messageSource.getMessage(eq("notification.document.reviewed.title"), any(),
                                any(java.util.Locale.class)))
                                .thenReturn("Localized Title");
                when(messageSource.getMessage(eq("notification.document.reviewed.approved"), any(),
                                any(java.util.Locale.class))).thenReturn("Localized Message");

                DocumentResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
                                brokerId);

                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
                assertThat(result.getBrokerNotes()).isNull();
                verify(emailService).sendDocumentStatusUpdatedNotification(any(Document.class),
                                eq("client@test.com"),
                                eq("John Client"),
                                eq("Jane Broker"), eq("Bank Statement"), anyString(), anyString());
                verify(notificationService).createNotification(
                                eq(clientId.toString()),
                                anyString(),
                                anyString(),
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_APPROVED));
        }

        @Test
        void reviewDocument_NotificationFailure_LogsAndProceeds() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                request.setCustomTitle("Bank Statement");
                request.setStatus(DocumentStatusEnum.SUBMITTED);

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Broker",
                                UserRole.BROKER,
                                "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Client",
                                UserRole.CLIENT,
                                "en");
                client.setId(clientId);

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(DocumentStatusEnum.APPROVED);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                // Simulate Notification Exception
                doThrow(new RuntimeException("Notification failed")).when(notificationService)
                                .createNotification(any(), any(), any(), any(), any());

                // Act
                DocumentResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
                                brokerId);

                // Assert
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
        }

        @Test
        void reviewDocument_Revision_SetsNotesAndSendsEmail() {
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setStatus(DocumentStatusEnum.SUBMITTED);

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Broker",
                                UserRole.BROKER,
                                "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Client",
                                UserRole.CLIENT,
                                "en");
                client.setId(clientId);

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(DocumentStatusEnum.NEEDS_REVISION);
                reviewDTO.setComments("Please update dates");

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc");
                when(messageSource.getMessage(eq("notification.document.reviewed.title"), any(),
                                any(java.util.Locale.class)))
                                .thenReturn("Localized Title");
                when(messageSource.getMessage(eq("notification.document.reviewed.needs_revision"), any(),
                                any(java.util.Locale.class))).thenReturn("Localized Message");

                DocumentResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
                                brokerId);

                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.NEEDS_REVISION);
                assertThat(result.getBrokerNotes()).isEqualTo("Please update dates");
                verify(emailService).sendDocumentStatusUpdatedNotification(any(Document.class),
                                eq("client@test.com"),
                                eq("John Client"),
                                eq("Jane Broker"), eq("PAY_STUBS"), anyString(), anyString());
                verify(notificationService).createNotification(
                                eq(clientId.toString()),
                                anyString(),
                                anyString(),
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REVISION));
        }

        @Test
        void reviewDocument_Rejected_SendsEmailAndNotification() {
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                request.setStatus(DocumentStatusEnum.SUBMITTED);

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Broker",
                                UserRole.BROKER,
                                "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Client",
                                UserRole.CLIENT,
                                "en");
                client.setId(clientId);

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(DocumentStatusEnum.REJECTED);
                reviewDTO.setComments("Document is blurry");

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc");
                when(messageSource.getMessage(eq("notification.document.rejected.title"), any(),
                                any(java.util.Locale.class)))
                                .thenReturn("Localized Title");
                when(messageSource.getMessage(eq("notification.document.rejected.message"), any(),
                                any(java.util.Locale.class))).thenReturn("Localized Message");

                DocumentResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
                                brokerId);

                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.REJECTED);
                verify(emailService).sendDocumentStatusUpdatedNotification(any(Document.class),
                                eq("client@test.com"),
                                eq("John Client"),
                                eq("Jane Broker"), anyString(), anyString(), anyString());
                verify(notificationService).createNotification(
                                eq(clientId.toString()),
                                anyString(),
                                anyString(),
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REJECTED));
        }

        // ========== getDocumentDownloadUrl Tests ==========

        @Test
        void getDocumentDownloadUrl_WithValidAccess_ReturnsUrl() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID documentId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                DocumentVersion submittedDoc = DocumentVersion.builder()
                                .versionId(documentId)
                                .storageObject(StorageObject.builder().s3Key("path/to/file.pdf").build())
                                .build();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setVersions(List.of(submittedDoc));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setClientId(UUID.randomUUID());

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(storageService.generatePresignedUrl("path/to/file.pdf")).thenReturn("https://presigned.url");

                // Act
                String result = service.getDocumentDownloadUrl(requestId, documentId, userId);

                // Assert
                assertThat(result).isEqualTo("https://presigned.url");
        }

        @Test
        void getDocumentDownloadUrl_WithNoAccess_ThrowsForbiddenException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(UUID.randomUUID());

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(
                                () -> service.getDocumentDownloadUrl(requestId, UUID.randomUUID(), UUID.randomUUID()))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have access");
        }

        // ========== getAllDocumentsForUser Tests ==========

        @Test
        void getAllDocumentsForUser_WithDocuments_ReturnsList() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID reqId1 = UUID.randomUUID();
                UUID reqId2 = UUID.randomUUID();

                Document doc1 = new Document();
                doc1.setDocumentId(reqId1);
                doc1.setTransactionRef(new TransactionRef(UUID.randomUUID(), userId, TransactionSide.BUY_SIDE));
                doc1.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                doc1.setStatus(DocumentStatusEnum.REQUESTED);
                doc1.setVersions(new ArrayList<>());

                Document doc2 = new Document();
                doc2.setDocumentId(reqId2);
                doc2.setTransactionRef(new TransactionRef(UUID.randomUUID(), userId, TransactionSide.SELL_SIDE));
                doc2.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                doc2.setStatus(DocumentStatusEnum.SUBMITTED);
                doc2.setVersions(new ArrayList<>());

                when(repository.findByUserId(userId)).thenReturn(List.of(doc1, doc2));

                // Act
                List<DocumentResponseDTO> result = service.getAllDocumentsForUser(userId);

                // Assert
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getDocumentId()).isEqualTo(reqId1);
                assertThat(result.get(1).getDocumentId()).isEqualTo(reqId2);
        }

        @Test
        void getAllDocumentsForUser_WithNoDocuments_ReturnsEmptyList() {
                // Arrange
                UUID userId = UUID.randomUUID();
                when(repository.findByUserId(userId)).thenReturn(List.of());

                // Act
                List<DocumentResponseDTO> result = service.getAllDocumentsForUser(userId);

                // Assert
                assertThat(result).isEmpty();
        }

        // ========== getDocument Tests ==========

        @Test
        void getDocumentRequest_WithValidAccess_ReturnsDocument() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setVersions(new ArrayList<>());

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setClientId(clientId);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                DocumentResponseDTO result = service.getDocument(requestId, userId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getDocumentId()).isEqualTo(requestId);
                assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.PAY_STUBS);
        }

        @Test
        void getDocumentRequest_WithClientAccess_ReturnsDocument() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.EMPLOYMENT_LETTER);
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setVersions(new ArrayList<>());

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(clientId);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                DocumentResponseDTO result = service.getDocument(requestId, clientId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getDocumentId()).isEqualTo(requestId);
        }

        @Test
        void getDocumentRequest_NotFound_ThrowsNotFoundException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                when(repository.findByDocumentId(requestId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> service.getDocument(requestId, UUID.randomUUID()))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document not found");
        }

        @Test
        void getDocumentRequest_WithNoAccess_ThrowsForbiddenException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(clientId);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> service.getDocument(requestId, UUID.randomUUID()))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have access");
        }

        // ========== updateDocument Tests ==========

        @Test
        void updateDocumentRequest_WithValidData_UpdatesRequest() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Document existingRequest = new Document();
                existingRequest.setDocumentId(requestId);
                existingRequest.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                existingRequest.setCustomTitle("Old Title");
                existingRequest.setVersions(new ArrayList<>());
                existingRequest.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                DocumentRequestDTO updateDTO = new DocumentRequestDTO();
                updateDTO.setCustomTitle("New Title");
                updateDTO.setBrokerNotes("Updated notes");

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(existingRequest));
                when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.of(tx));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                // Act
                DocumentResponseDTO result = service.updateDocument(requestId, updateDTO, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getCustomTitle()).isEqualTo("New Title");
                assertThat(result.getBrokerNotes()).isEqualTo("Updated notes");
        }

        @Test
        void updateDocumentRequest_NotFound_ThrowsNotFoundException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                DocumentRequestDTO updateDTO = new DocumentRequestDTO();

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> service.updateDocument(requestId, updateDTO, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document not found");
        }

        // ========== deleteDocument Tests ==========

        @Test
        void deleteDocumentRequest_WithValidId_DeletesSuccessfully() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.of(tx));

                // Act
                service.deleteDocument(requestId, brokerId);

                // Assert
                verify(repository).delete(request);
        }

        @Test
        void deleteDocumentRequest_NotFound_ThrowsNotFoundException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                when(repository.findByDocumentId(requestId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> service.deleteDocument(requestId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document not found");
        }

        // ========== getDocumentDownloadUrl Additional Tests ==========

        @Test
        void getDocumentDownloadUrl_WithNonExistentDocument_ThrowsNotFoundException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID documentId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                DocumentVersion submittedDoc = DocumentVersion.builder()
                                .versionId(UUID.randomUUID())
                                .storageObject(StorageObject.builder().s3Key("path/to/other.pdf").build())
                                .build();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setVersions(List.of(submittedDoc));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setClientId(UUID.randomUUID());

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> service.getDocumentDownloadUrl(requestId, documentId, userId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document version not found");
        }

        @Test
        void createDocumentRequest_WithMissingClient_SkipsNotification() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(UUID.randomUUID());
                tx.setBrokerId(brokerId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                // Client not found
                when(userAccountRepository.findById(tx.getClientId())).thenReturn(Optional.empty());
                // Broker found
                when(userAccountRepository.findById(tx.getBrokerId())).thenReturn(Optional.of(new UserAccount()));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                // Act
                service.createDocument(transactionId, dto, brokerId);

                // Assert
                verify(emailService, never()).sendDocumentRequestedNotification(anyString(), anyString(), anyString(),
                                anyString(), anyString(), anyString(), anyString());
                verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(),
                                anyString(),
                                any());
        }

        @Test
        void updateDocumentRequest_WithAllFields_UpdatesAll() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                request.setVisibleToClient(false);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("New Title");
                dto.setExpectedFrom(DocumentPartyEnum.BROKER);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("New Notes");

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.of(tx));

                // Act
                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);

                // Assert
                assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.PAY_STUBS);
                assertThat(result.getCustomTitle()).isEqualTo("New Title");
                assertThat(result.getExpectedFrom()).isEqualTo(DocumentPartyEnum.BROKER);
                assertThat(result.isVisibleToClient()).isTrue();
                assertThat(result.getBrokerNotes()).isEqualTo("New Notes");
        }

                // Arrange


        @Test
        void reviewDocument_WithMissingClient_SkipsNotification() {
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                request.setStatus(DocumentStatusEnum.SUBMITTED);
                request.setDocType(DocumentTypeEnum.ID_VERIFICATION);

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(UUID.randomUUID());
                tx.setBrokerId(UUID.randomUUID());

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                lenient().when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                lenient().when(userAccountRepository.findById(tx.getClientId())).thenReturn(Optional.empty()); // Missing
                                                                                                               // client
                lenient().when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(DocumentStatusEnum.APPROVED);
                service.reviewDocument(transactionId, requestId, reviewDTO, tx.getBrokerId());

                verify(emailService, never()).sendDocumentStatusUpdatedNotification(any(), anyString(), anyString(),
                                anyString(), anyString(), anyString(), anyString());
                verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(),
                                anyString(),
                                any());
        }

        // ========== Private Helper Tests ==========
        @Test
        void isFrench_ReturnsTrueForFrVariants() throws Exception {
                var method = service.getClass().getDeclaredMethod("isFrench", String.class);
                method.setAccessible(true);
                assertThat((Boolean) method.invoke(service, "fr")).isTrue();
                assertThat((Boolean) method.invoke(service, "FR")).isTrue();
                assertThat((Boolean) method.invoke(service, "Fr")).isTrue();
                assertThat((Boolean) method.invoke(service, "fR")).isTrue();
                assertThat((Boolean) method.invoke(service, "en")).isFalse();
                assertThat((Boolean) method.invoke(service, (Object) null)).isFalse();
        }

        @Test
        void updateDocumentRequest_WhenNormalizedCandidateIsIdentical_ReturnsEarly() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("Notes");
                request.setStage(
                                com.example.courtierprobackend.documents.datalayer.enums.StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("Title");
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("Notes");
                dto.setStage(com.example.courtierprobackend.documents.datalayer.enums.StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                // No stubbing for repository.save needed since it should not be called

                // Act
                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);

                // Assert
                assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.PAY_STUBS);
                assertThat(result.getCustomTitle()).isEqualTo("Title");
                // ...other asserts as needed...
        }

        @Test
        void createDocumentRequest_WithNotificationAndMessageSource_TriggersNotificationAndMessageSource() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                dto.setCustomTitle("Bank Statement");

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);
                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");

                // Act
                service.createDocument(transactionId, dto, brokerId);

                // Assert
                // Capture arguments to diagnose mismatch
                org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
                verify(emailService).sendDocumentRequestedNotification(
                                captor.capture(),
                                captor.capture(),
                                captor.capture(),
                                captor.capture(),
                                captor.capture(),
                                captor.capture(),
                                captor.capture());
                java.util.List<String> args = captor.getAllValues();
                org.junit.jupiter.api.Assertions.assertEquals("client@test.com", args.get(0));
                org.junit.jupiter.api.Assertions.assertEquals("John Doe", args.get(1));
                org.junit.jupiter.api.Assertions.assertEquals("Jane Smith", args.get(2));
                org.junit.jupiter.api.Assertions.assertEquals("Bank Statement", args.get(3));
                org.junit.jupiter.api.Assertions.assertEquals("BANK_STATEMENT", args.get(4));
                org.junit.jupiter.api.Assertions.assertNull(args.get(5)); // brokerNotes should be null
                org.junit.jupiter.api.Assertions.assertEquals("en", args.get(6));
                verify(messageSource).getMessage(
                                contains("document.type."),
                                any(),
                                anyString(),
                                any(java.util.Locale.class));
        }

        @Test
        void updateDocumentRequest_WhenDocTypeDiffers_ProceedsWithUpdate() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("Notes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT); // Different
                dto.setCustomTitle("Title");
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("Notes");
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.BANK_STATEMENT);
        }

        @Test
        void updateDocumentRequest_WhenCustomTitleDiffers_ProceedsWithUpdate() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title1");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("Notes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("Title2"); // Different
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("Notes");
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.getCustomTitle()).isEqualTo("Title2");
        }

        @Test
        void updateDocumentRequest_WhenExpectedFromDiffers_ProceedsWithUpdate() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("Notes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("Title");
                dto.setExpectedFrom(DocumentPartyEnum.BROKER); // Different
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("Notes");
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.getExpectedFrom()).isEqualTo(DocumentPartyEnum.BROKER);
        }

        @Test
        void updateDocumentRequest_WhenVisibleToClientDiffers_ProceedsWithUpdate() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(false);
                request.setBrokerNotes("Notes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("Title");
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true); // Different
                dto.setBrokerNotes("Notes");
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.isVisibleToClient()).isTrue();
        }

        @Test
        void updateDocumentRequest_WhenBrokerNotesDiffers_ProceedsWithUpdate() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("Notes1");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("Title");
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("Notes2"); // Different
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.getBrokerNotes()).isEqualTo("Notes2");
        }

        @Test
        void updateDocumentRequest_WhenStageDiffers_ProceedsWithUpdate() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("Notes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("Title");
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("Notes");
                dto.setStage(StageEnum.BUYER_SHOP_FOR_PROPERTY); // Different

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.getStage()).isEqualTo(StageEnum.BUYER_SHOP_FOR_PROPERTY);
        }

        @Test
        void updateDocumentRequest_WhenCustomTitleNullAndEmptyHandled() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle(null);
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes(null);
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle(""); // Empty string
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes(""); // Empty string
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                assertThat(result.getCustomTitle()).isNull();
                assertThat(result.getBrokerNotes()).isNull();
        }

        @Test
        void updateDocumentRequest_WhenCaseInsensitiveEnumAndString() {
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("title");
                request.setExpectedFrom(DocumentPartyEnum.CLIENT);
                request.setVisibleToClient(true);
                request.setBrokerNotes("notes");
                request.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId()))
                                .thenReturn(Optional.of(tx));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("TITLE"); // Different case
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setVisibleToClient(true);
                dto.setBrokerNotes("NOTES"); // Different case
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.updateDocument(requestId, dto, brokerId);
                // Should still update because normalization is case-insensitive
                assertThat(result.getCustomTitle()).isEqualTo("TITLE");
                assertThat(result.getBrokerNotes()).isEqualTo("NOTES");
        }

        // ========== Additional Coverage Tests ==========

        @Test
        void getDocumentsForTransaction_NotFound_ThrowsNotFoundException() {
                // Coverage for line 72
                UUID transactionId = UUID.randomUUID();
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.getDocumentsForTransaction(transactionId, UUID.randomUUID()))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void getDocumentRequest_TransactionNotFound_ThrowsNotFoundException() {
                // Coverage for line 95
                UUID requestId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.getDocument(requestId, UUID.randomUUID()))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void createDocumentRequest_NotFound_ThrowsNotFoundException() {
                // Coverage for line 107
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.createDocument(transactionId, dto, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void createDocumentRequest_WithNullVisibleToClient_DefaultsToTrue() {
                // Coverage for line 117
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                dto.setVisibleToClient(null); // null should default to true

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                DocumentResponseDTO result = service.createDocument(transactionId, dto, brokerId);

                assertThat(result.isVisibleToClient()).isTrue();
        }

        @Test
        void createDocumentRequest_WithConditionIds_SavesLinks() {
                // Coverage for lines 126-133
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID conditionId1 = UUID.randomUUID();
                UUID conditionId2 = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                dto.setConditionIds(List.of(conditionId1, conditionId2));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                service.createDocument(transactionId, dto, brokerId);

                verify(documentConditionLinkRepository, times(2)).save(any());
        }

        @Test
        void createDocumentRequest_WithNotificationException_LogsAndContinues() {
                // Coverage for lines 177-178
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);
                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenThrow(new RuntimeException("MessageSource error"));

                // Act - should not throw
                DocumentResponseDTO result = service.createDocument(transactionId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
        }

        @Test
        void updateDocumentRequest_WithFrenchLocale_UsesFrenchLocale() {
                // Coverage for line 260
                UUID requestId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("OldTitle");
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "Jean", "Dupont",
                                UserRole.CLIENT, "fr");
                client.setId(clientId);
                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Marie", "Martin",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("NewTitle"); // Different to trigger update

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(messageSource.getMessage(anyString(), any(), anyString(), eq(java.util.Locale.FRENCH)))
                                .thenReturn("Document");
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Document");

                service.updateDocument(requestId, dto, brokerId);

                // Verify messageSource was called (indicating locale handling)
                verify(messageSource, atLeast(1)).getMessage(anyString(), any(), anyString(),
                                any(java.util.Locale.class));
        }

        @Test
        void updateDocumentRequest_WithConditionIds_UpdatesLinks() {
                // Coverage for lines 318-325
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID conditionId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Title1");
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PAY_STUBS);
                dto.setCustomTitle("Title2");
                dto.setConditionIds(List.of(conditionId));

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                Transaction tx = new Transaction();
                tx.setBrokerId(brokerId);
                when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.of(tx));

                service.updateDocument(requestId, dto, brokerId);

                verify(documentConditionLinkRepository).deleteByDocumentId(requestId);
                verify(documentConditionLinkRepository).save(any());
        }

        @Test
        void submitDocument_RequestNotFound_ThrowsNotFoundException() {
                // Coverage for line 344
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.submitDocument(transactionId, requestId,
                                new org.springframework.mock.web.MockMultipartFile("file", "test.pdf",
                                                "application/pdf", "content".getBytes()),
                                UUID.randomUUID(), UploadedByRefEnum.CLIENT))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document not found");
        }

        @Test
        void submitDocument_TransactionNotFound_ThrowsNotFoundException() throws IOException {
                // Coverage for line 352
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                request.setVersions(new ArrayList<>());

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.submitDocument(transactionId, requestId,
                                new org.springframework.mock.web.MockMultipartFile("file", "test.pdf",
                                                "application/pdf", "content".getBytes()),
                                UUID.randomUUID(), UploadedByRefEnum.CLIENT))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void submitDocument_WithFrenchBroker_UsesFrenchLocale() throws IOException {
                // Coverage for lines 402, 409
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                request.setVersions(new ArrayList<>());

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Marie", "Martin",
                                UserRole.BROKER, "fr");
                broker.setId(brokerId);

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);

                StorageObject storageObject = StorageObject.builder().s3Key("key").fileName("test.pdf").build();

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(storageService.uploadFile(any(), eq(transactionId), eq(requestId))).thenReturn(storageObject);
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Test Message");

                org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                                "file", "test.pdf", "application/pdf", "content".getBytes());

                service.submitDocument(transactionId, requestId, file, clientId, UploadedByRefEnum.CLIENT);

                // Verify French locale was used for broker notification
                verify(messageSource, atLeast(1)).getMessage(anyString(), any(), anyString(),
                                eq(java.util.Locale.FRENCH));
        }

        @Test
        void reviewDocument_MismatchedTransaction_ThrowsBadRequest() {
                // Coverage for lines 484, 487
                UUID transactionId = UUID.randomUUID();
                UUID differentTransactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(differentTransactionId, UUID.randomUUID(),
                                TransactionSide.BUY_SIDE));
                request.setStatus(DocumentStatusEnum.SUBMITTED);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(DocumentStatusEnum.APPROVED);

                assertThatThrownBy(() -> service.reviewDocument(transactionId, requestId, reviewDTO, UUID.randomUUID()))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Document does not belong to this transaction");
        }

        @Test
        void reviewDocument_NotSubmittedStatus_ThrowsBadRequest() {
                // Coverage for lines 490, 491
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                request.setStatus(DocumentStatusEnum.REQUESTED); // Not SUBMITTED

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(DocumentStatusEnum.APPROVED);

                assertThatThrownBy(() -> service.reviewDocument(transactionId, requestId, reviewDTO, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Only submitted documents can be reviewed");
        }

        @Test
        void getDocumentDownloadUrl_RequestNotFound_ThrowsNotFoundException() {
                // Coverage for line 462
                UUID requestId = UUID.randomUUID();

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.empty());

                assertThatThrownBy(
                                () -> service.getDocumentDownloadUrl(requestId, UUID.randomUUID(), UUID.randomUUID()))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document not found");
        }

        @Test
        void getDocumentDownloadUrl_TransactionNotFound_ThrowsNotFoundException() {
                // Coverage for line 466
                UUID requestId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                request.setVersions(List.of());

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                assertThatThrownBy(
                                () -> service.getDocumentDownloadUrl(requestId, UUID.randomUUID(), UUID.randomUUID()))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void mapToResponseDTO_WithNullSubmittedDocuments_ReturnsNull() {
                // Coverage for line 442
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(UUID.randomUUID());

                Document request = new Document();
                request.setDocumentId(UUID.randomUUID());
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setVersions(null);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(repository.findByTransactionRef_TransactionId(transactionId)).thenReturn(List.of(request));

                List<DocumentResponseDTO> result = service.getDocumentsForTransaction(transactionId, brokerId);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getVersions()).isNull();
        }

        @Test
        void getOutstandingDocumentSummary_ShouldReturnList() {
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setCreatedAt(java.time.LocalDateTime.now().minusDays(10));
                // Set due date to 5 days ago, so it is 5 days overdue
                request.setDueDate(java.time.LocalDateTime.now().minusDays(5));

                when(repository.findOutstandingDocumentsForBroker(eq(brokerId), any(java.time.LocalDateTime.class)))
                                .thenReturn(List.of(request));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);
                tx.setPropertyAddress(new PropertyAddress("123 Main St", "City", "QC", "H1H1H1"));

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("John");
                client.setLastName("Doe");
                client.setEmail("john@example.com");

                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));

                List<OutstandingDocumentDTO> result = service.getOutstandingDocumentSummary(brokerId);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getId()).isEqualTo(requestId);
                assertThat(result.get(0).getTitle()).isEqualTo("PROOF_OF_FUNDS");
                assertThat(result.get(0).getTransactionAddress()).contains("123 Main St", "City", "QC", "H1H1H1");
                assertThat(result.get(0).getClientName()).isEqualTo("John Doe");
                // Due date was 5 days ago, so it is 5 days outstanding
                assertThat(result.get(0).getDaysOutstanding()).isEqualTo(5);
        }

        @Test
        void sendDocumentReminder_ShouldSendEmail() {
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                request.setStatus(DocumentStatusEnum.REQUESTED);

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                // Assuming permission checks pass if broker owns transaction or we mock access
                // utils if used
                when(participantRepository.findByTransactionId(transactionId))
                                .thenReturn(java.util.Collections.emptyList());

                UserAccount client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("John");
                client.setLastName("Doe");
                client.setEmail("john@example.com");
                client.setPreferredLanguage("en");

                UserAccount broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("Agent");
                broker.setLastName("Smith");

                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

                service.sendDocumentReminder(requestId, brokerId);

                verify(emailService).sendDocumentRequestedNotification(
                                eq("john@example.com"),
                                eq("John Doe"),
                                eq("Agent Smith"),
                                anyString(),
                                eq("PROOF_OF_FUNDS"),
                                any(),
                                eq("en"));
        }

        // ========== Draft Workflow Tests ==========

        @Test
        void createDocument_WithDraftStatus_SkipsNotificationsAndTimelineEntry() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                dto.setStatus(DocumentStatusEnum.DRAFT); // Set as DRAFT

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findByTransactionId(transactionId))
                                .thenReturn(java.util.Collections.emptyList());
                when(repository.save(any(Document.class))).thenAnswer(inv -> {
                        Document saved = inv.getArgument(0);
                        saved.setDocumentId(UUID.randomUUID());
                        return saved;
                });

                // Act
                DocumentResponseDTO result = service.createDocument(transactionId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.DRAFT);

                // Verify no email notifications sent
                verify(emailService, never()).sendDocumentRequestedNotification(any(), any(), any(), any(), any(), any(), any());

                // Verify no timeline entry added
                verify(timelineService, never()).addEntry(any(), any(), any(), any(), any());

                // Verify no in-app notification sent
                verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }

        @Test
        void createDocument_WithRequestedStatus_SendsNotifications() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                DocumentRequestDTO dto = new DocumentRequestDTO();
                dto.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);
                dto.setStage(StageEnum.BUYER_PREQUALIFY_FINANCIALLY);
                dto.setStatus(DocumentStatusEnum.REQUESTED);

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);
                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findByTransactionId(transactionId))
                                .thenReturn(java.util.Collections.emptyList());
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> {
                        Document saved = inv.getArgument(0);
                        saved.setDocumentId(UUID.randomUUID());
                        return saved;
                });
                lenient().when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");
                lenient().when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class)))
                                .thenReturn("Notification Message");

                // Act
                DocumentResponseDTO result = service.createDocument(transactionId, dto, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED);

                // Verify email notification sent
                verify(emailService, atLeastOnce()).sendDocumentRequestedNotification(any(), any(), any(), any(), any(), any(), any());

                // Verify timeline entry added
                verify(timelineService, atLeastOnce()).addEntry(any(), any(), any(), any(), any());
        }

        @Test
        void getDocumentsForTransaction_WhenClient_FiltersDraftDocuments() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document draftDoc = new Document();
                draftDoc.setDocumentId(UUID.randomUUID());
                draftDoc.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                draftDoc.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                draftDoc.setStatus(DocumentStatusEnum.DRAFT);
                draftDoc.setVersions(new ArrayList<>());

                Document requestedDoc = new Document();
                requestedDoc.setDocumentId(UUID.randomUUID());
                requestedDoc.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                requestedDoc.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                requestedDoc.setStatus(DocumentStatusEnum.REQUESTED);
                requestedDoc.setVersions(new ArrayList<>());

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(repository.findByTransactionRef_TransactionId(transactionId))
                                .thenReturn(List.of(draftDoc, requestedDoc));

                // Act - Client requests documents
                List<DocumentResponseDTO> result = service.getDocumentsForTransaction(transactionId, clientId);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED);
                assertThat(result.stream().noneMatch(d -> d.getStatus() == DocumentStatusEnum.DRAFT)).isTrue();
        }

        @Test
        void getDocumentsForTransaction_WhenBroker_IncludesDraftDocuments() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document draftDoc = new Document();
                draftDoc.setDocumentId(UUID.randomUUID());
                draftDoc.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                draftDoc.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                draftDoc.setStatus(DocumentStatusEnum.DRAFT);
                draftDoc.setVersions(new ArrayList<>());

                Document requestedDoc = new Document();
                requestedDoc.setDocumentId(UUID.randomUUID());
                requestedDoc.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                requestedDoc.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                requestedDoc.setStatus(DocumentStatusEnum.REQUESTED);
                requestedDoc.setVersions(new ArrayList<>());

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(repository.findByTransactionRef_TransactionId(transactionId))
                                .thenReturn(List.of(draftDoc, requestedDoc));

                // Act - Broker requests documents
                List<DocumentResponseDTO> result = service.getDocumentsForTransaction(transactionId, brokerId);

                // Assert - Broker should see all documents including drafts
                assertThat(result).hasSize(2);
                assertThat(result.stream().anyMatch(d -> d.getStatus() == DocumentStatusEnum.DRAFT)).isTrue();
        }

        @Test
        void sendDocumentRequest_WithDraftDocument_TransitionsToRequested() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document draftDoc = new Document();
                draftDoc.setDocumentId(documentId);
                draftDoc.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                draftDoc.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                draftDoc.setStatus(DocumentStatusEnum.DRAFT);
                draftDoc.setVersions(new ArrayList<>());

                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);
                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(draftDoc));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findByTransactionId(transactionId))
                                .thenReturn(java.util.Collections.emptyList());
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");
                when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class)))
                                .thenReturn("Notification Message");

                // Act
                DocumentResponseDTO result = service.sendDocumentRequest(documentId, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED);

                // Verify email notification sent
                verify(emailService).sendDocumentRequestedNotification(
                                eq("client@test.com"),
                                eq("John Doe"),
                                eq("Jane Smith"),
                                anyString(),
                                eq("PROOF_OF_FUNDS"),
                                any(),
                                eq("en"));

                // Verify timeline entry added
                verify(timelineService).addEntry(eq(transactionId), eq(brokerId), any(), any(), any());

                // Verify in-app notification sent
                verify(notificationService).createNotification(
                                eq(clientId.toString()),
                                anyString(),
                                anyString(),
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REQUEST));
        }

        @Test
        void sendDocumentRequest_WithNonDraftDocument_ThrowsBadRequestException() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document requestedDoc = new Document();
                requestedDoc.setDocumentId(documentId);
                requestedDoc.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                requestedDoc.setDocType(DocumentTypeEnum.PROOF_OF_FUNDS);
                requestedDoc.setStatus(DocumentStatusEnum.REQUESTED); // Not a draft
                requestedDoc.setVersions(new ArrayList<>());

                when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(requestedDoc));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(participantRepository.findByTransactionId(transactionId))
                                .thenReturn(java.util.Collections.emptyList());

                // Act & Assert
                assertThatThrownBy(() -> service.sendDocumentRequest(documentId, brokerId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Only draft documents can be sent as requests");
        }

        @Test
        void sendDocumentRequest_WithNotFoundDocument_ThrowsNotFoundException() {
                // Arrange
                UUID documentId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                when(repository.findByDocumentId(documentId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> service.sendDocumentRequest(documentId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document not found");
        }
        @Test
        void submitDocument_WhenUploadedByBroker_NotifiesClient() throws IOException {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document request = new Document();
                request.setDocumentId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Pay Stub");
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setVersions(new ArrayList<>());

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);

                MockMultipartFile file = new MockMultipartFile("file", "paystub.pdf", "application/pdf",
                                "content".getBytes());
                StorageObject storageObject = StorageObject.builder().s3Key("key").fileName("test.pdf").build();

                when(repository.findByDocumentId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(storageService.uploadFile(any(), eq(transactionId), eq(requestId))).thenReturn(storageObject);
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

                // Mock message prompts
                when(messageSource.getMessage(eq("notification.document.submitted.title"), any(),
                                any(java.util.Locale.class)))
                                .thenReturn("Document Submitted");
                when(messageSource.getMessage(eq("notification.document.submitted.message"), any(),
                                any(java.util.Locale.class)))
                                .thenReturn("Jane Smith submitted document: Pay Stub");
                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc Type");

                // Act
                // Broker uploads document
                DocumentResponseDTO result = service.submitDocument(transactionId, requestId, file, brokerId,
                                UploadedByRefEnum.BROKER);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.SUBMITTED);

                // Verify Email sent to CLIENT
                verify(emailService).sendDocumentSubmittedNotification(
                                any(),
                                eq("client@test.com"), // Expected recipient: client
                                eq("John Doe"), // Expected name greeting: client name
                                eq("Pay Stub"), // Doc name
                                anyString(),
                                eq("en") // Client language
                );

                // Verify In-App Notification created for CLIENT
                verify(notificationService).createNotification(
                                eq(client.getId().toString()), // Expected recipient ID: client
                                anyString(),
                                eq("Jane Smith submitted document: Pay Stub"), // Expected message with Broker Name
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_SUBMITTED));
        }

        @Test
        void shareDocumentWithClient_Success() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID documentId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);

                Document document = new Document();
                document.setDocumentId(documentId);
                document.setTransactionRef(
                                com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef.builder()
                                                .transactionId(transactionId).build());
                document.setDocType(DocumentTypeEnum.OTHER);
                document.setCustomTitle("Draft Doc");
                document.setStatus(DocumentStatusEnum.DRAFT);
                document.setFlow(DocumentFlowEnum.UPLOAD);

                UserAccount broker = new UserAccount(brokerId.toString(), "broker@test.com", "Broker", "User",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(clientId.toString(), "client@test.com", "Client", "User",
                                UserRole.CLIENT, "en");
                client.setId(clientId);

                when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(document));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(repository.save(any(Document.class))).thenAnswer(i -> i.getArguments()[0]);
                when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Message");

                // Act
                DocumentResponseDTO result = service.shareDocumentWithClient(documentId, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.SUBMITTED);
                assertThat(result.isVisibleToClient()).isTrue();

                // Verify Email Sent
                verify(emailService).sendDocumentSubmittedNotification(
                                eq(document),
                                eq("client@test.com"),
                                eq("Broker User"),
                                eq("Draft Doc"),
                                eq("OTHER"),
                                eq("en"));

                // Verify In-App Notification
                verify(notificationService).createNotification(
                                eq(clientId.toString()),
                                anyString(),
                                anyString(),
                                eq(transactionId.toString()),
                                any());
        }
}
