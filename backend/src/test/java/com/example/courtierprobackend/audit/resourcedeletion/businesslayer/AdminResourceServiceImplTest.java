package com.example.courtierprobackend.audit.resourcedeletion.businesslayer;

import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditLog;
import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditRepository;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.DeletionPreviewResponse;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.ResourceListResponse;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.SubmittedDocument;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminResourceServiceImplTest {

        @Mock
        private TransactionRepository transactionRepository;
        @Mock
        private DocumentRequestRepository documentRequestRepository;
        @Mock
        private AdminDeletionAuditRepository auditRepository;
        @Mock
        private S3StorageService s3StorageService;
        @Mock
        private UserAccountRepository userAccountRepository;
        @Mock
        private TimelineEntryRepository timelineEntryRepository;
        @Mock
        private com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;

        private AdminResourceServiceImpl service;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                service = new AdminResourceServiceImpl(
                                transactionRepository,
                                documentRequestRepository,
                                timelineEntryRepository,
                                auditRepository,
                                s3StorageService,
                                objectMapper,
                                userAccountRepository,
                                notificationService);
        }

        // ========== listResources Tests ==========

        @Test
        void listResources_WithTransactions_ReturnsResourceList() {
                UUID txId = UUID.randomUUID();
                Transaction tx = createTestTransaction(txId);

                when(transactionRepository.findAll()).thenReturn(List.of(tx));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, false);

                assertThat(result.getResourceType()).isEqualTo("TRANSACTION");
                assertThat(result.getTotalCount()).isEqualTo(1);
                assertThat(result.getDeletedCount()).isEqualTo(0);
                assertThat(result.getItems()).hasSize(1);
                assertThat(result.getItems().get(0).getId()).isEqualTo(txId);
        }

        @Test
        void listResources_IncludingDeleted_UsesIncludingDeletedQuery() {
                when(transactionRepository.findAllIncludingDeleted()).thenReturn(List.of());

                service.listResources(AdminDeletionAuditLog.ResourceType.TRANSACTION, true);

                verify(transactionRepository).findAllIncludingDeleted();
                verify(transactionRepository, never()).findAll();
        }

        @Test
        void listResources_WithDocumentRequests_ReturnsResourceList() {
                UUID reqId = UUID.randomUUID();
                DocumentRequest doc = createTestDocumentRequest(reqId);

                when(documentRequestRepository.findAll()).thenReturn(List.of(doc));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, false);

                assertThat(result.getResourceType()).isEqualTo("DOCUMENT_REQUEST");
                assertThat(result.getTotalCount()).isEqualTo(1);
                assertThat(result.getItems().get(0).getId()).isEqualTo(reqId);
        }

        @Test
        void listResources_WithNullFields_HandlesGracefully() {
                // Transaction with null fields
                Transaction tx = new Transaction();
                tx.setTransactionId(UUID.randomUUID());
                tx.setOpenedAt(LocalDateTime.now());
                // null client, broker, address, status, side

                // DocumentRequest with null fields
                DocumentRequest doc = new DocumentRequest();
                doc.setRequestId(UUID.randomUUID());
                doc.setLastUpdatedAt(LocalDateTime.now());
                // null transaction ref, doc type, custom title, status

                when(transactionRepository.findAll()).thenReturn(List.of(tx));
                when(documentRequestRepository.findAll()).thenReturn(List.of(doc));

                // Test Transaction mapping
                ResourceListResponse txResult = service.listResources(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, false);
                ResourceListResponse.ResourceItem txItem = txResult.getItems().get(0);
                assertThat(txItem.getClientEmail()).isNull();
                assertThat(txItem.getBrokerEmail()).isNull();
                assertThat(txItem.getAddress()).isNull();
                assertThat(txItem.getStatus()).isNull();
                assertThat(txItem.getSide()).isNull();

                // Test DocumentRequest mapping
                ResourceListResponse docResult = service.listResources(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, false);
                ResourceListResponse.ResourceItem docItem = docResult.getItems().get(0);
                assertThat(docItem.getTransactionId()).isNull();
                assertThat(docItem.getDocType()).isNull(); // Falls back to title which is null
                assertThat(docItem.getSubmittedDocCount()).isEqualTo(0);
        }

        // ========== previewDeletion Tests ==========

        @Test
        void previewDeletion_ForTransaction_ReturnsLinkedResources() {
                UUID txId = UUID.randomUUID();
                UUID docReqId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);

                DocumentRequest docReq = createTestDocumentRequest(docReqId);
                docReq.setSubmittedDocuments(List.of(createTestSubmittedDocument(submittedDocId)));

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of(docReq));

                DeletionPreviewResponse result = service.previewDeletion(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, txId);

                assertThat(result.getResourceId()).isEqualTo(txId);
                assertThat(result.getResourceType()).isEqualTo("TRANSACTION");
                assertThat(result.getLinkedResources()).hasSizeGreaterThan(0);
                assertThat(result.getS3FilesToDelete()).hasSize(1);
        }

        @Test
        void previewDeletion_ForNonExistentTransaction_ThrowsNotFoundException() {
                UUID txId = UUID.randomUUID();
                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.previewDeletion(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, txId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void previewDeletion_ForDocumentRequest_ReturnsLinkedResources() {
                UUID reqId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                DocumentRequest docReq = createTestDocumentRequest(reqId);
                docReq.setSubmittedDocuments(List.of(createTestSubmittedDocument(submittedDocId)));

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId))
                                .thenReturn(Optional.of(docReq));

                DeletionPreviewResponse result = service.previewDeletion(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId);

                assertThat(result.getResourceId()).isEqualTo(reqId);
                assertThat(result.getResourceType()).isEqualTo("DOCUMENT_REQUEST");
                assertThat(result.getLinkedResources()).hasSize(1);
        }

        @Test
        void previewDeletion_WithNullFields_HandlesGracefully() {
                UUID txId = UUID.randomUUID();
                Transaction tx = createTestTransaction(txId);

                // Add timeline entry with null type (fallback)
                TimelineEntry entry = new TimelineEntry();
                entry.setId(UUID.randomUUID());
                // type is null

                // Add document request with minimal fields
                DocumentRequest docReq = new DocumentRequest();
                docReq.setRequestId(UUID.randomUUID());

                // Add submitted document with null storage object
                SubmittedDocument subDoc = new SubmittedDocument();
                subDoc.setDocumentId(UUID.randomUUID());
                docReq.setSubmittedDocuments(List.of(subDoc));

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(Optional.of(tx));
                when(timelineEntryRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(List.of(entry));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(List.of(docReq));

                DeletionPreviewResponse result = service.previewDeletion(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, txId);

                assertThat(result.getLinkedResources()).extracting("summary")
                                .contains("Unknown Timeline Entry", "Unknown (UNKNOWN)", "Unknown file");
        }

        // ========== deleteResource Tests ==========

        @Test
        void deleteResource_DocumentRequest_S3DeletionFailure_LogsErrorAndContinues() {
                UUID reqId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                DocumentRequest docReq = createTestDocumentRequest(reqId);
                SubmittedDocument submittedDoc = createTestSubmittedDocument(submittedDocId);
                docReq.setSubmittedDocuments(new ArrayList<>(List.of(submittedDoc)));

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId)).thenReturn(Optional.of(docReq));
                when(documentRequestRepository.save(any(DocumentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

                doThrow(new RuntimeException("S3 Error")).when(s3StorageService).deleteFile(anyString());

                service.deleteResource(AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, adminId);

                assertThat(docReq.getDeletedAt()).isNotNull();
                assertThat(submittedDoc.getDeletedAt()).isNotNull();
                verify(auditRepository).save(any(AdminDeletionAuditLog.class));
        }

        @Test
        void deleteResource_ForTransaction_SoftDeletesAndCreatesAuditLog() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of());
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                assertThat(tx.getDeletedAt()).isNotNull();
                assertThat(tx.getDeletedBy()).isEqualTo(adminId);
                verify(auditRepository).save(any(AdminDeletionAuditLog.class));
                // Verify notifications sent to both Broker and Client
                String expectedMessage = "Transaction for 123 Main St has been cancelled by an administrator.";
                verify(notificationService).createNotification(
                                eq(tx.getBrokerId().toString()),
                                eq("Transaction Cancelled"),
                                eq(expectedMessage),
                                eq(txId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.TRANSACTION_CANCELLED));
                verify(notificationService).createNotification(
                                eq(tx.getClientId().toString()),
                                eq("Transaction Cancelled"),
                                eq(expectedMessage),
                                eq(txId.toString()),
                                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.TRANSACTION_CANCELLED));
        }

        @Test
        void deleteResource_ForAlreadyDeletedTransaction_ThrowsBadRequestException() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                tx.setDeletedAt(LocalDateTime.now());

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));

                assertThatThrownBy(() -> service.deleteResource(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("already deleted");
        }

        @Test
        void deleteResource_WithS3Files_HardDeletesFiles() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                UUID docReqId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);

                DocumentRequest docReq = createTestDocumentRequest(docReqId);
                SubmittedDocument submittedDoc = createTestSubmittedDocument(submittedDocId);
                docReq.setSubmittedDocuments(new ArrayList<>(List.of(submittedDoc)));

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of(docReq));
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                verify(s3StorageService).deleteFile("path/to/file.pdf");
                assertThat(submittedDoc.getDeletedAt()).isNotNull();
                assertThat(docReq.getDeletedAt()).isNotNull();
        }

        @Test
        void deleteResource_Transaction_NotificationFailure_LogsErrorAndContinues() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                tx.setClientId(UUID.randomUUID());
                tx.setBrokerId(UUID.randomUUID());

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of());
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                doThrow(new RuntimeException("Notification Error")).when(notificationService)
                                .createNotification(anyString(), anyString(), anyString(), anyString(), any());

                service.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                assertThat(tx.getDeletedAt()).isNotNull();
                verify(auditRepository).save(any(AdminDeletionAuditLog.class));
        }

        @Test
        void deleteResource_S3DeletionFailure_LogsErrorAndContinues() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                UUID docReqId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                DocumentRequest docReq = createTestDocumentRequest(docReqId);
                SubmittedDocument submittedDoc = createTestSubmittedDocument(submittedDocId);
                docReq.setSubmittedDocuments(new ArrayList<>(List.of(submittedDoc)));

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(List.of(docReq));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

                // Simulate S3 exception
                doThrow(new RuntimeException("S3 Error")).when(s3StorageService).deleteFile(anyString());

                service.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                // Verification: Should still create audit log and soft delete despite S3 error
                assertThat(tx.getDeletedAt()).isNotNull();
                assertThat(submittedDoc.getDeletedAt()).isNotNull();
                verify(auditRepository).save(any(AdminDeletionAuditLog.class));
        }

        @Test
        void deleteResource_ForDocumentRequest_SoftDeletesWithCascade() {
                UUID reqId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                DocumentRequest docReq = createTestDocumentRequest(reqId);
                SubmittedDocument submittedDoc = createTestSubmittedDocument(submittedDocId);
                docReq.setSubmittedDocuments(new ArrayList<>(List.of(submittedDoc)));

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId))
                                .thenReturn(Optional.of(docReq));
                when(documentRequestRepository.save(any(DocumentRequest.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.deleteResource(AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, adminId);

                assertThat(docReq.getDeletedAt()).isNotNull();
                assertThat(docReq.getDeletedBy()).isEqualTo(adminId);
                assertThat(submittedDoc.getDeletedAt()).isNotNull();
                verify(s3StorageService).deleteFile("path/to/file.pdf");
                verify(auditRepository).save(any(AdminDeletionAuditLog.class));
        }

        // ========== restoreResource Tests ==========

        @Test
        void restoreResource_ForDeletedTransaction_RestoresWithCascade() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                tx.setDeletedAt(LocalDateTime.now());
                tx.setDeletedBy(adminId);

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of());
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.restoreResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                assertThat(tx.getDeletedAt()).isNull();
                assertThat(tx.getDeletedBy()).isNull();
        }

        @Test
        void restoreResource_ForNonDeletedTransaction_ThrowsBadRequestException() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                // Not deleted - deletedAt is null

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));

                assertThatThrownBy(() -> service.restoreResource(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("not deleted");
        }

        @Test
        void restoreResource_ForDocumentRequest_RestoresMetadata() {
                UUID reqId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                DocumentRequest docReq = createTestDocumentRequest(reqId);
                docReq.setDeletedAt(LocalDateTime.now());
                docReq.setDeletedBy(adminId);

                SubmittedDocument submittedDoc = createTestSubmittedDocument(submittedDocId);
                submittedDoc.setDeletedAt(LocalDateTime.now());
                submittedDoc.setDeletedBy(adminId);
                docReq.setSubmittedDocuments(List.of(submittedDoc));

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId))
                                .thenReturn(Optional.of(docReq));
                when(documentRequestRepository.save(any(DocumentRequest.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.restoreResource(AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, adminId);

                assertThat(docReq.getDeletedAt()).isNull();
                assertThat(docReq.getDeletedBy()).isNull();
                assertThat(submittedDoc.getDeletedAt()).isNull();
        }

        @Test
        void restoreResource_ParentTransactionDeleted_ThrowsBadRequest() {
                UUID reqId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                UUID txId = UUID.randomUUID();

                DocumentRequest docReq = createTestDocumentRequest(reqId);
                docReq.setTransactionRef(new TransactionRef(txId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                docReq.setDeletedAt(LocalDateTime.now()); // It is deleted

                Transaction parentTx = createTestTransaction(txId);
                parentTx.setDeletedAt(LocalDateTime.now()); // Parent is ALSO deleted

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId)).thenReturn(Optional.of(docReq));
                when(transactionRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(Optional.of(parentTx));

                assertThatThrownBy(() -> service.restoreResource(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, adminId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("parent transaction");
        }

        @Test
        void restoreResource_WithNonDeletedTimelineEntries_SkipsRestoration() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                Transaction tx = createTestTransaction(txId);
                tx.setDeletedAt(LocalDateTime.now());

                TimelineEntry activeEntry = createTestTimelineEntry();
                // deletedAt is null by default

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(Optional.of(tx));
                when(timelineEntryRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of(activeEntry));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

                service.restoreResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                // Active entry should remain untouched (checking verification effectively,
                // though no save needed)
                assertThat(activeEntry.getDeletedAt()).isNull();
                verify(timelineEntryRepository, never()).save(activeEntry);
        }

        // ========== getAuditHistory Tests ==========

        @Test
        void getAuditHistory_ReturnsOrderedList() {
                AdminDeletionAuditLog log1 = AdminDeletionAuditLog.builder()
                                .id(1L)
                                .action(AdminDeletionAuditLog.ActionType.DELETE)
                                .timestamp(LocalDateTime.now().minusDays(1))
                                .adminId(UUID.randomUUID())
                                .resourceType(AdminDeletionAuditLog.ResourceType.TRANSACTION)
                                .resourceId(UUID.randomUUID())
                                .build();

                AdminDeletionAuditLog log2 = AdminDeletionAuditLog.builder()
                                .id(2L)
                                .action(AdminDeletionAuditLog.ActionType.RESTORE)
                                .timestamp(LocalDateTime.now())
                                .adminId(UUID.randomUUID())
                                .resourceType(AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST)
                                .resourceId(UUID.randomUUID())
                                .build();

                when(auditRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(log2, log1));

                List<AdminDeletionAuditLog> result = service.getAuditHistory();

                assertThat(result).hasSize(2);
                assertThat(result.get(0).getId()).isEqualTo(2L);
        }

        // ========== Helper Methods ==========

        private Transaction createTestTransaction(UUID transactionId) {
                Transaction tx = new Transaction();
                tx.setTransactionId(transactionId);
                tx.setClientId(UUID.randomUUID());
                tx.setBrokerId(UUID.randomUUID());
                tx.setPropertyAddress(new PropertyAddress("123 Main St", "Montreal", "QC", "H2X1A1"));
                tx.setStatus(TransactionStatus.ACTIVE);
                tx.setOpenedAt(LocalDateTime.now());
                return tx;
        }

        private DocumentRequest createTestDocumentRequest(UUID requestId) {
                DocumentRequest doc = new DocumentRequest();
                doc.setRequestId(requestId);
                doc.setTransactionRef(
                                new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));
                doc.setDocType(DocumentTypeEnum.BANK_STATEMENT);
                doc.setCustomTitle("Bank Statement");
                doc.setStatus(DocumentStatusEnum.REQUESTED);
                doc.setLastUpdatedAt(LocalDateTime.now());
                doc.setSubmittedDocuments(new ArrayList<>());
                return doc;
        }

        private SubmittedDocument createTestSubmittedDocument(UUID documentId) {
                return SubmittedDocument.builder()
                                .documentId(documentId)
                                .storageObject(StorageObject.builder()
                                                .s3Key("path/to/file.pdf")
                                                .fileName("document.pdf")
                                                .mimeType("application/pdf")
                                                .sizeBytes(1024L)
                                                .build())
                                .uploadedAt(LocalDateTime.now())
                                .build();
        }

        private TimelineEntry createTestTimelineEntry() {
                TimelineEntry entry = new TimelineEntry();
                entry.setId(UUID.randomUUID());
                entry.setType(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.STAGE_CHANGE);
                return entry;
        }
        // ========== Unsupported Type Tests ==========

        @Test
        void listResources_WithUnsupportedType_ThrowsBadRequest() {
                assertThatThrownBy(() -> service.listResources(AdminDeletionAuditLog.ResourceType.SUBMITTED_DOCUMENT,
                                false))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("not supported");
        }

        @Test
        void previewDeletion_WithUnsupportedType_ThrowsBadRequest() {
                UUID id = UUID.randomUUID();
                assertThatThrownBy(() -> service.previewDeletion(AdminDeletionAuditLog.ResourceType.SUBMITTED_DOCUMENT,
                                id))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("not supported");
        }

        @Test
        void deleteResource_WithUnsupportedType_ThrowsBadRequest() {
                UUID id = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                assertThatThrownBy(() -> service.deleteResource(AdminDeletionAuditLog.ResourceType.SUBMITTED_DOCUMENT,
                                id, adminId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("not supported");
        }

        @Test
        void restoreResource_WithUnsupportedType_ThrowsBadRequest() {
                UUID id = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                assertThatThrownBy(() -> service.restoreResource(AdminDeletionAuditLog.ResourceType.SUBMITTED_DOCUMENT,
                                id, adminId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("not supported");
        }

        // ========== Serialization Failure Tests ==========

        @Test
        void createAuditLog_JsonProcessingException_LogsError()
                        throws com.fasterxml.jackson.core.JsonProcessingException {
                // Mock ObjectMapper to throw exception
                ObjectMapper mockMapper = mock(ObjectMapper.class);
                AdminResourceServiceImpl errorService = new AdminResourceServiceImpl(
                                transactionRepository,
                                documentRequestRepository,
                                timelineEntryRepository,
                                auditRepository,
                                s3StorageService,
                                mockMapper,
                                userAccountRepository,
                                notificationService);

                when(mockMapper.writeValueAsString(any()))
                                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Fail") {
                                });

                // Trigger an audit log creation via deleteTransaction
                UUID txId = UUID.randomUUID();
                Transaction tx = createTestTransaction(txId);
                when(transactionRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any())).thenReturn(tx);

                // Should not throw, just log error
                errorService.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, UUID.randomUUID());

                // Verify save was NOT called because createAuditLog catches exception before
                // saving?
                // Wait, looking at code: catch is around auditRepository.save(log) OR around
                // construction?
                // Code:
                // try {
                // AdminDeletionAuditLog log = ... .cascadedDeletions(mapper.write...) ...
                // auditRepository.save(log);
                // } catch (JsonProcessingException e) { log.error }
                // So auditRepository.save will NOT be called.

                verify(auditRepository, never()).save(any());
        }

        @Test
        void buildTransactionSnapshot_JsonProcessingException_ReturnsEmptyJson()
                        throws com.fasterxml.jackson.core.JsonProcessingException {
                ObjectMapper mockMapper = mock(ObjectMapper.class);
                AdminResourceServiceImpl errorService = new AdminResourceServiceImpl(
                                transactionRepository,
                                documentRequestRepository,
                                timelineEntryRepository,
                                auditRepository,
                                s3StorageService,
                                mockMapper,
                                userAccountRepository,
                                notificationService);

                // Use doAnswer to conditionally fail only for the Map (snapshot) and succeed
                // for List (cascaded)
                when(mockMapper.writeValueAsString(any())).thenAnswer(invocation -> {
                        Object arg = invocation.getArgument(0);
                        if (arg instanceof java.util.Map) {
                                throw new com.fasterxml.jackson.core.JsonProcessingException("Fail") {
                                };
                        }
                        if (arg instanceof java.util.List) {
                                return "[]";
                        }
                        return "{}";
                });

                UUID txId = UUID.randomUUID();
                Transaction tx = createTestTransaction(txId);
                when(transactionRepository.findByTransactionIdIncludingDeleted(txId)).thenReturn(Optional.of(tx));
                when(transactionRepository.save(any())).thenReturn(tx);

                errorService.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, UUID.randomUUID());

                // Verify audit log IS saved, and snapshot is "{}"
                org.mockito.ArgumentCaptor<AdminDeletionAuditLog> captor = org.mockito.ArgumentCaptor
                                .forClass(AdminDeletionAuditLog.class);
                verify(auditRepository).save(captor.capture());
                assertThat(captor.getValue().getResourceSnapshot()).isEqualTo("{}");
        }

        @Test
        void listResources_CalculatesDeletedCount_Correctly() {
                Transaction t1 = createTestTransaction(UUID.randomUUID());
                Transaction t2 = createTestTransaction(UUID.randomUUID());
                t2.setDeletedAt(LocalDateTime.now());

                when(transactionRepository.findAllIncludingDeleted()).thenReturn(List.of(t1, t2));

                ResourceListResponse response = service.listResources(AdminDeletionAuditLog.ResourceType.TRANSACTION,
                                true);

                assertThat(response.getTotalCount()).isEqualTo(2);
                assertThat(response.getDeletedCount()).isEqualTo(1);
        }

        @Test
        void deleteResource_WithMissingUsers_SkipsNotification() {
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                tx.setClientId(null);
                tx.setBrokerId(null);

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of());
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                // Assert
                assertThat(tx.getDeletedAt()).isNotNull();
                verify(auditRepository).save(any(AdminDeletionAuditLog.class));
                // Verify NO notifications sent
                verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(),
                                anyString(),
                                any());
        }

        @Test
        void buildDocumentRequestSnapshot_JsonProcessingException_ReturnsEmptyJson()
                        throws com.fasterxml.jackson.core.JsonProcessingException {
                ObjectMapper mockMapper = mock(ObjectMapper.class);
                AdminResourceServiceImpl errorService = new AdminResourceServiceImpl(
                                transactionRepository,
                                documentRequestRepository,
                                timelineEntryRepository,
                                auditRepository,
                                s3StorageService,
                                mockMapper,
                                userAccountRepository,
                                notificationService);

                // Use doAnswer to conditionally fail only for the Map (snapshot) and succeed for List (cascaded)
                // The snapshot argument is a Map<String, Object>
                when(mockMapper.writeValueAsString(any())).thenAnswer(invocation -> {
                        Object arg = invocation.getArgument(0);
                        if (arg instanceof java.util.Map) {
                                throw new com.fasterxml.jackson.core.JsonProcessingException("Fail") {
                                };
                        }
                        if (arg instanceof java.util.List) {
                                return "[]";
                        }
                        return "{}";
                });

                UUID reqId = UUID.randomUUID();
                DocumentRequest docReq = createTestDocumentRequest(reqId);
                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId)).thenReturn(Optional.of(docReq));
                when(documentRequestRepository.save(any())).thenReturn(docReq);

                errorService.deleteResource(AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, UUID.randomUUID());

                // Verify audit log IS saved, and snapshot is "{}"
                org.mockito.ArgumentCaptor<AdminDeletionAuditLog> captor = org.mockito.ArgumentCaptor
                                .forClass(AdminDeletionAuditLog.class);
                verify(auditRepository).save(captor.capture());
                assertThat(captor.getValue().getResourceSnapshot()).isEqualTo("{}");
        }

        // ========== Additional Coverage Tests ==========

        @Test
        void listTransactions_WithUserEmails_IncludesEmails() {
                // Coverage for lines 62, 66
                UUID txId = UUID.randomUUID();
                UUID clientId = UUID.randomUUID();
                UUID brokerId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                tx.setClientId(clientId);
                tx.setBrokerId(brokerId);

                com.example.courtierprobackend.user.dataaccesslayer.UserAccount client = 
                        new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
                client.setId(clientId);
                client.setEmail("client@test.com");

                com.example.courtierprobackend.user.dataaccesslayer.UserAccount broker = 
                        new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
                broker.setId(brokerId);
                broker.setEmail("broker@test.com");

                when(transactionRepository.findAll()).thenReturn(List.of(tx));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, false);

                assertThat(result.getItems()).hasSize(1);
                assertThat(result.getItems().get(0).getClientEmail()).isEqualTo("client@test.com");
                assertThat(result.getItems().get(0).getBrokerEmail()).isEqualTo("broker@test.com");
        }

        @Test
        void listTransactions_WithNullSide_DisplaysNull() {
                // Coverage for line 84
                UUID txId = UUID.randomUUID();
                Transaction tx = createTestTransaction(txId);
                tx.setSide(null);

                when(transactionRepository.findAll()).thenReturn(List.of(tx));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, false);

                assertThat(result.getItems().get(0).getSide()).isNull();
        }

        @Test
        void listDocumentRequests_WithDeletedItems_CountsCorrectly() {
                // Coverage for lines 100, 103
                UUID reqId1 = UUID.randomUUID();
                UUID reqId2 = UUID.randomUUID();

                DocumentRequest doc1 = createTestDocumentRequest(reqId1);
                DocumentRequest doc2 = createTestDocumentRequest(reqId2);
                doc2.setDeletedAt(LocalDateTime.now());

                when(documentRequestRepository.findAllIncludingDeleted()).thenReturn(List.of(doc1, doc2));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, true);

                assertThat(result.getTotalCount()).isEqualTo(2);
                assertThat(result.getDeletedCount()).isEqualTo(1);
        }

        @Test
        void listDocumentRequests_WithNullTransactionRef_HandlesGracefully() {
                // Coverage for line 103
                UUID reqId = UUID.randomUUID();
                DocumentRequest doc = createTestDocumentRequest(reqId);
                doc.setTransactionRef(null);

                when(documentRequestRepository.findAll()).thenReturn(List.of(doc));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, false);

                assertThat(result.getItems().get(0).getTransactionId()).isNull();
        }

        @Test
        void listDocumentRequests_WithSubmittedDocuments_CountsCorrectly() {
                // Coverage for line 120
                UUID reqId = UUID.randomUUID();
                DocumentRequest doc = createTestDocumentRequest(reqId);
                doc.setSubmittedDocuments(List.of(
                        createTestSubmittedDocument(UUID.randomUUID()),
                        createTestSubmittedDocument(UUID.randomUUID())
                ));

                when(documentRequestRepository.findAll()).thenReturn(List.of(doc));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, false);

                assertThat(result.getItems().get(0).getSubmittedDocCount()).isEqualTo(2);
        }

        @Test
        void deleteDocumentRequest_AlreadyDeleted_ThrowsBadRequest() {
                // Coverage for lines 329, 332
                UUID reqId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                DocumentRequest doc = createTestDocumentRequest(reqId);
                doc.setDeletedAt(LocalDateTime.now());

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId))
                                .thenReturn(Optional.of(doc));

                assertThatThrownBy(() -> service.deleteResource(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, adminId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("already deleted");
        }

        @Test
        void restoreTransaction_NotFound_ThrowsNotFound() {
                // Coverage for line 375
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.restoreResource(
                                AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void restoreTransaction_WithDeletedTimelineEntries_RestoresEntries() {
                // Coverage for lines 388-390
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                tx.setDeletedAt(LocalDateTime.now());
                tx.setDeletedBy(adminId);

                TimelineEntry entry = createTestTimelineEntry();
                entry.setTransactionId(txId);
                entry.setDeletedAt(LocalDateTime.now());
                entry.setDeletedBy(adminId);

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(timelineEntryRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of(entry));
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(timelineEntryRepository.save(any(TimelineEntry.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.restoreResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                assertThat(entry.getDeletedAt()).isNull();
                assertThat(entry.getDeletedBy()).isNull();
                verify(timelineEntryRepository).save(entry);
        }

        @Test
        void restoreTransaction_WithDeletedDocumentRequestsAndSubmissions_RestoresCascade() {
                // Coverage for lines 397-407
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();
                UUID docReqId = UUID.randomUUID();
                UUID submittedDocId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);
                tx.setDeletedAt(LocalDateTime.now());
                tx.setDeletedBy(adminId);

                DocumentRequest docReq = createTestDocumentRequest(docReqId);
                docReq.setTransactionRef(new TransactionRef(txId, UUID.randomUUID(), TransactionSide.BUY_SIDE));
                docReq.setDeletedAt(LocalDateTime.now());
                docReq.setDeletedBy(adminId);

                SubmittedDocument submittedDoc = createTestSubmittedDocument(submittedDocId);
                submittedDoc.setDeletedAt(LocalDateTime.now());
                submittedDoc.setDeletedBy(adminId);
                docReq.setSubmittedDocuments(new ArrayList<>(List.of(submittedDoc)));

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(timelineEntryRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of());
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of(docReq));
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.restoreResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                assertThat(tx.getDeletedAt()).isNull();
                assertThat(docReq.getDeletedAt()).isNull();
                assertThat(submittedDoc.getDeletedAt()).isNull();
                // Note: Service modifies docReq in-place but doesn't explicitly save it (relies on cascading)
        }

        @Test
        void restoreDocumentRequest_NotDeleted_ThrowsBadRequest() {
                // Coverage for lines 421, 424
                UUID reqId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                DocumentRequest doc = createTestDocumentRequest(reqId);
                // Not deleted, deletedAt is null

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId))
                                .thenReturn(Optional.of(doc));

                assertThatThrownBy(() -> service.restoreResource(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, adminId))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("not deleted");
        }

        @Test
        void restoreDocumentRequest_NotFound_ThrowsNotFound() {
                // Coverage for NotFoundException in restoreDocumentRequest
                UUID reqId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.restoreResource(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId, adminId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Document request not found");
        }

        @Test
        void buildDocumentRequestSummary_WithNullDocType_FallsBackToTitle() {
                // Coverage for lines 472, 482-483
                UUID reqId = UUID.randomUUID();
                DocumentRequest doc = createTestDocumentRequest(reqId);
                doc.setDocType(null);
                doc.setCustomTitle("Custom Title");

                when(documentRequestRepository.findAll()).thenReturn(List.of(doc));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, false);

                assertThat(result.getItems().get(0).getDocType()).isEqualTo("Custom Title");
        }

        @Test
        void buildDocumentRequestSummary_WithNullDocTypeAndNullTitle_FallsBackToUnknown() {
                // Coverage for fallback when both are null
                UUID reqId = UUID.randomUUID();
                DocumentRequest doc = createTestDocumentRequest(reqId);
                doc.setDocType(null);
                doc.setCustomTitle(null);

                when(documentRequestRepository.findAll()).thenReturn(List.of(doc));

                ResourceListResponse result = service.listResources(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, false);

                assertThat(result.getItems().get(0).getDocType()).isNull();
        }

        @Test
        void deleteTransaction_WithTimelineEntries_SoftDeletesEntries() {
                // Coverage for lines 287-292
                UUID txId = UUID.randomUUID();
                UUID adminId = UUID.randomUUID();

                Transaction tx = createTestTransaction(txId);

                TimelineEntry entry1 = createTestTimelineEntry();
                entry1.setTransactionId(txId);
                TimelineEntry entry2 = createTestTimelineEntry();
                entry2.setTransactionId(txId);

                when(transactionRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(Optional.of(tx));
                when(timelineEntryRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of(entry1, entry2));
                when(documentRequestRepository.findByTransactionIdIncludingDeleted(txId))
                                .thenReturn(List.of());
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(timelineEntryRepository.save(any(TimelineEntry.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                service.deleteResource(AdminDeletionAuditLog.ResourceType.TRANSACTION, txId, adminId);

                assertThat(entry1.getDeletedAt()).isNotNull();
                assertThat(entry1.getDeletedBy()).isEqualTo(adminId);
                assertThat(entry2.getDeletedAt()).isNotNull();
                verify(timelineEntryRepository, times(2)).save(any(TimelineEntry.class));
        }

        @Test
        void previewDocumentRequestDeletion_WithNullStorageObject_HandlesGracefully() {
                // Coverage for lines 198, 207
                UUID reqId = UUID.randomUUID();

                DocumentRequest docReq = createTestDocumentRequest(reqId);
                SubmittedDocument subDoc = new SubmittedDocument();
                subDoc.setDocumentId(UUID.randomUUID());
                subDoc.setStorageObject(null);
                docReq.setSubmittedDocuments(List.of(subDoc));

                when(documentRequestRepository.findByRequestIdIncludingDeleted(reqId))
                                .thenReturn(Optional.of(docReq));

                DeletionPreviewResponse result = service.previewDeletion(
                                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, reqId);

                assertThat(result.getLinkedResources()).hasSize(1);
                assertThat(result.getS3FilesToDelete()).isEmpty();
        }
}


