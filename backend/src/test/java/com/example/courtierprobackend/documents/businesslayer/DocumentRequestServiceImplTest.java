package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.SubmittedDocument;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
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
 * Unit tests for DocumentRequestServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class DocumentRequestServiceImplTest {

        @Mock
        private DocumentRequestRepository repository;
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

        private DocumentRequestServiceImpl service;

        @BeforeEach
        void setUp() {
                service = new DocumentRequestServiceImpl(repository, storageService, emailService, notificationService,
                                transactionRepository, userAccountRepository, timelineService, messageSource);
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
                DocumentRequest doc = new DocumentRequest();
                doc.setRequestId(requestId);
                doc.setTransactionRef(new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                doc.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                doc.setStatus(DocumentStatusEnum.REQUESTED);
                doc.setSubmittedDocuments(new ArrayList<>());

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(repository.findByTransactionRef_TransactionId(transactionId)).thenReturn(List.of(doc));

                // Act
                List<DocumentRequestResponseDTO> result = service.getDocumentsForTransaction(transactionId, userId);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getRequestId()).isEqualTo(requestId);
        }

        @Test
        void getDocumentsForTransaction_WithNoAccess_ThrowsForbiddenException() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(UUID.randomUUID());

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> service.getDocumentsForTransaction(transactionId, userId))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have access");
        }

        // ========== createDocumentRequest Tests ==========

        @Test
        void createDocumentRequest_WithValidData_CreatesRequest() {
                // Arrange
                UUID transactionId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                UUID brokerId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                tx.setBrokerId(brokerId);
                tx.setClientId(clientId);
                tx.setSide(TransactionSide.BUY_SIDE);

                DocumentRequestRequestDTO dto = new DocumentRequestRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                dto.setCustomTitle("Bank Statement Q1");
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);

                String clientAuth0Id = UUID.randomUUID().toString();
                String brokerAuth0Id = UUID.randomUUID().toString();

                UserAccount client = new UserAccount(clientAuth0Id, "client@test.com", "John", "Doe", UserRole.CLIENT,
                                "en");
                client.setId(clientId);
                UserAccount broker = new UserAccount(brokerAuth0Id, "broker@test.com", "Jane", "Smith", UserRole.BROKER,
                                "en");
                broker.setId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Message");
                when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class)))
                                .thenReturn("Localized Title");

                // Act
                DocumentRequestResponseDTO result = service.createDocumentRequest(transactionId, dto);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.BANK_STATEMENT);
                verify(emailService).sendDocumentRequestedNotification(anyString(), anyString(), anyString(),
                                anyString(),
                                anyString(), anyString());
                verify(notificationService).createNotification(
                                eq(clientId.toString()),
                                anyString(),
                                anyString(),
                                eq(transactionId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REQUEST));
        }

        @Test
        void createDocumentRequest_NotificationFailure_LogsAndProceeds() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);
                tx.setSide(TransactionSide.BUY_SIDE);

                DocumentRequestRequestDTO dto = new DocumentRequestRequestDTO();
                dto.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                dto.setExpectedFrom(DocumentPartyEnum.CLIENT);

                UserAccount client = new UserAccount(UUID.randomUUID().toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);
                UserAccount broker = new UserAccount(UUID.randomUUID().toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Message");
                when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class)))
                                .thenReturn("Localized Title");

                // Simulate Notification Exception
                doThrow(new RuntimeException("Notification failed")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), anyString(), any());

                // Act
                DocumentRequestResponseDTO result = service.createDocumentRequest(transactionId, dto);

                // Assert
                assertThat(result).isNotNull(); // Servic should succeed despite notification error
                verify(emailService).sendDocumentRequestedNotification(anyString(), anyString(), anyString(),
                                anyString(),
                                anyString(), anyString());
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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setCustomTitle("Pay Stub");
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setSubmittedDocuments(new ArrayList<>());

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

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(storageService.uploadFile(any(), eq(transactionId), eq(requestId))).thenReturn(storageObject);
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                // Act
                DocumentRequestResponseDTO result = service.submitDocument(transactionId, requestId, file, uploaderId,
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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                request.setSubmittedDocuments(new ArrayList<>());

                UserAccount broker = new UserAccount(UUID.randomUUID().toString(), "broker@test.com", "Jane", "Smith",
                                UserRole.BROKER, "en");
                broker.setId(brokerId);
                UserAccount client = new UserAccount(UUID.randomUUID().toString(), "client@test.com", "John", "Doe",
                                UserRole.CLIENT, "en");
                client.setId(clientId);

                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                                "content".getBytes());
                StorageObject storageObject = StorageObject.builder().s3Key("key").fileName("test.pdf").build();

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(storageService.uploadFile(any(), eq(transactionId), eq(requestId))).thenReturn(storageObject);
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                // Simulate Notification Exception
                doThrow(new RuntimeException("Notification failed")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), anyString(), any());

                // Act
                DocumentRequestResponseDTO result = service.submitDocument(transactionId, requestId, file, clientId,
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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

                MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                                "content".getBytes());

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));

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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
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

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc");
                when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class)))
                                .thenReturn("Localized Title");
                when(messageSource.getMessage(eq("notification.document.reviewed.message"), any(),
                                any(java.util.Locale.class))).thenReturn("Localized Message");

                DocumentRequestResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
                                brokerId);

                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
                assertThat(result.getBrokerNotes()).isNull();
                verify(emailService).sendDocumentStatusUpdatedNotification(any(DocumentRequest.class),
                                eq("client@test.com"),
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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
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

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                // Simulate Notification Exception
                doThrow(new RuntimeException("Notification failed")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), anyString(), any());

                // Act
                DocumentRequestResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
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

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc");
                when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class)))
                                .thenReturn("Localized Title");
                when(messageSource.getMessage(eq("notification.document.reviewed.message"), any(),
                                any(java.util.Locale.class))).thenReturn("Localized Message");

                DocumentRequestResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
                                brokerId);

                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.NEEDS_REVISION);
                assertThat(result.getBrokerNotes()).isEqualTo("Please update dates");
                verify(emailService).sendDocumentStatusUpdatedNotification(any(DocumentRequest.class),
                                eq("client@test.com"),
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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
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

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                when(messageSource.getMessage(anyString(), any(), anyString(), any(java.util.Locale.class)))
                                .thenReturn("Localized Doc");
                when(messageSource.getMessage(anyString(), any(), any(java.util.Locale.class)))
                                .thenReturn("Localized Title");
                when(messageSource.getMessage(eq("notification.document.rejected.message"), any(),
                                any(java.util.Locale.class))).thenReturn("Localized Message");

                DocumentRequestResponseDTO result = service.reviewDocument(transactionId, requestId, reviewDTO,
                                brokerId);

                assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.REJECTED);
                verify(emailService).sendDocumentStatusUpdatedNotification(any(DocumentRequest.class),
                                eq("client@test.com"),
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

                SubmittedDocument submittedDoc = SubmittedDocument.builder()
                                .documentId(documentId)
                                .storageObject(StorageObject.builder().s3Key("path/to/file.pdf").build())
                                .build();

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setSubmittedDocuments(List.of(submittedDoc));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setClientId(UUID.randomUUID());

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
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

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(
                                new TransactionRef(transactionId, UUID.randomUUID(), TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(UUID.randomUUID());

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
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

                DocumentRequest doc1 = new DocumentRequest();
                doc1.setRequestId(reqId1);
                doc1.setTransactionRef(new TransactionRef(UUID.randomUUID(), userId, TransactionSide.BUY_SIDE));
                doc1.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                doc1.setStatus(DocumentStatusEnum.REQUESTED);
                doc1.setSubmittedDocuments(new ArrayList<>());

                DocumentRequest doc2 = new DocumentRequest();
                doc2.setRequestId(reqId2);
                doc2.setTransactionRef(new TransactionRef(UUID.randomUUID(), userId, TransactionSide.SELL_SIDE));
                doc2.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                doc2.setStatus(DocumentStatusEnum.SUBMITTED);
                doc2.setSubmittedDocuments(new ArrayList<>());

                when(repository.findByUserId(userId)).thenReturn(List.of(doc1, doc2));

                // Act
                List<DocumentRequestResponseDTO> result = service.getAllDocumentsForUser(userId);

                // Assert
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getRequestId()).isEqualTo(reqId1);
                assertThat(result.get(1).getRequestId()).isEqualTo(reqId2);
        }

        @Test
        void getAllDocumentsForUser_WithNoDocuments_ReturnsEmptyList() {
                // Arrange
                UUID userId = UUID.randomUUID();
                when(repository.findByUserId(userId)).thenReturn(List.of());

                // Act
                List<DocumentRequestResponseDTO> result = service.getAllDocumentsForUser(userId);

                // Assert
                assertThat(result).isEmpty();
        }

        // ========== getDocumentRequest Tests ==========

        @Test
        void getDocumentRequest_WithValidAccess_ReturnsDocument() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.PAY_STUBS);
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setSubmittedDocuments(new ArrayList<>());

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setClientId(clientId);

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                DocumentRequestResponseDTO result = service.getDocumentRequest(requestId, userId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getRequestId()).isEqualTo(requestId);
                assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.PAY_STUBS);
        }

        @Test
        void getDocumentRequest_WithClientAccess_ReturnsDocument() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setDocType(DocumentTypeEnum.EMPLOYMENT_LETTER);
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setSubmittedDocuments(new ArrayList<>());

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(clientId);

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act
                DocumentRequestResponseDTO result = service.getDocumentRequest(requestId, clientId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getRequestId()).isEqualTo(requestId);
        }

        @Test
        void getDocumentRequest_NotFound_ThrowsNotFoundException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                when(repository.findByRequestId(requestId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> service.getDocumentRequest(requestId, UUID.randomUUID()))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document request not found");
        }

        @Test
        void getDocumentRequest_WithNoAccess_ThrowsForbiddenException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(UUID.randomUUID());
                tx.setClientId(clientId);

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> service.getDocumentRequest(requestId, UUID.randomUUID()))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have access");
        }

        // ========== updateDocumentRequest Tests ==========

        @Test
        void updateDocumentRequest_WithValidData_UpdatesRequest() {
                // Arrange
                UUID requestId = UUID.randomUUID();

                DocumentRequest existingRequest = new DocumentRequest();
                existingRequest.setRequestId(requestId);
                existingRequest.setDocType(DocumentTypeEnum.ID_VERIFICATION);
                existingRequest.setCustomTitle("Old Title");
                existingRequest.setSubmittedDocuments(new ArrayList<>());

                DocumentRequestRequestDTO updateDTO = new DocumentRequestRequestDTO();
                updateDTO.setCustomTitle("New Title");
                updateDTO.setBrokerNotes("Updated notes");

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(existingRequest));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                // Act
                DocumentRequestResponseDTO result = service.updateDocumentRequest(requestId, updateDTO);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getCustomTitle()).isEqualTo("New Title");
                assertThat(result.getBrokerNotes()).isEqualTo("Updated notes");
        }

        @Test
        void updateDocumentRequest_NotFound_ThrowsNotFoundException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                DocumentRequestRequestDTO updateDTO = new DocumentRequestRequestDTO();

                when(repository.findByRequestId(requestId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> service.updateDocumentRequest(requestId, updateDTO))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document request not found");
        }

        @Test
        void updateDocumentRequest_WithPartialData_OnlyUpdatesProvidedFields() {
                // Arrange
                UUID requestId = UUID.randomUUID();

                DocumentRequest existingRequest = new DocumentRequest();
                existingRequest.setRequestId(requestId);
                existingRequest.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                existingRequest.setCustomTitle("Original Title");
                existingRequest.setBrokerNotes("Original Notes");
                existingRequest.setVisibleToClient(true);
                existingRequest.setSubmittedDocuments(new ArrayList<>());

                DocumentRequestRequestDTO updateDTO = new DocumentRequestRequestDTO();
                updateDTO.setCustomTitle("Updated Title");
                // Other fields are null - should not be updated

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(existingRequest));
                when(repository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                // Act
                DocumentRequestResponseDTO result = service.updateDocumentRequest(requestId, updateDTO);

                // Assert
                assertThat(result.getCustomTitle()).isEqualTo("Updated Title");
                assertThat(result.getBrokerNotes()).isEqualTo("Original Notes"); // Unchanged
        }

        // ========== deleteDocumentRequest Tests ==========

        @Test
        void deleteDocumentRequest_WithValidId_DeletesSuccessfully() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));

                // Act
                service.deleteDocumentRequest(requestId);

                // Assert
                verify(repository).delete(request);
        }

        @Test
        void deleteDocumentRequest_NotFound_ThrowsNotFoundException() {
                // Arrange
                UUID requestId = UUID.randomUUID();
                when(repository.findByRequestId(requestId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> service.deleteDocumentRequest(requestId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document request not found");
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

                SubmittedDocument submittedDoc = SubmittedDocument.builder()
                                .documentId(UUID.randomUUID())
                                .storageObject(StorageObject.builder().s3Key("path/to/other.pdf").build())
                                .build();

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(requestId);
                request.setTransactionRef(new TransactionRef(transactionId, clientId, TransactionSide.BUY_SIDE));
                request.setSubmittedDocuments(List.of(submittedDoc));

                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setBrokerId(userId);
                tx.setClientId(UUID.randomUUID());

                when(repository.findByRequestId(requestId)).thenReturn(Optional.of(request));
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

                // Act & Assert
                assertThatThrownBy(() -> service.getDocumentDownloadUrl(requestId, documentId, userId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Submitted document not found");
        }
}
