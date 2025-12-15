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
import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
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

    private AdminResourceServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AdminResourceServiceImpl(
                transactionRepository,
                documentRequestRepository,
                auditRepository,
                s3StorageService,
                objectMapper,
                userAccountRepository
        );
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

    // ========== previewDeletion Tests ==========

    @Test
    void previewDeletion_ForTransaction_ReturnsLinkedResources() {
        UUID txId = UUID.randomUUID();
        UUID docReqId = UUID.randomUUID();
        UUID submittedDocId = UUID.randomUUID();

        Transaction tx = createTestTransaction(txId);
        tx.setTimeline(List.of(createTestTimelineEntry()));

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

    // ========== deleteResource Tests ==========

    @Test
    void deleteResource_ForTransaction_SoftDeletesAndCreatesAuditLog() {
        UUID txId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        Transaction tx = createTestTransaction(txId);
        tx.setTimeline(new ArrayList<>());

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
        tx.setTimeline(new ArrayList<>());

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
        tx.setTimeline(new ArrayList<>());

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
        doc.setTransactionRef(new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));
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
        entry.setId(1L);
        entry.setTitle("Test Entry");
        return entry;
    }
}
