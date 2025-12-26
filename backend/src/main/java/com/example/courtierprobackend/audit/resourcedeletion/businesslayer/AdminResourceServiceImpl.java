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
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminResourceServiceImpl implements AdminResourceService {

    private final TransactionRepository transactionRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final TimelineEntryRepository timelineEntryRepository;
    private final AdminDeletionAuditRepository auditRepository;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;
    private final com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;

    @Override
    public ResourceListResponse listResources(AdminDeletionAuditLog.ResourceType type, boolean includeDeleted) {
        return switch (type) {
            case TRANSACTION -> listTransactions(includeDeleted);
            case DOCUMENT_REQUEST -> listDocumentRequests(includeDeleted);
            default -> throw new BadRequestException("Resource type not supported for listing: " + type);
        };
    }

    private ResourceListResponse listTransactions(boolean includeDeleted) {
        List<Transaction> transactions = includeDeleted
                ? transactionRepository.findAllIncludingDeleted()
                : transactionRepository.findAll();

        long deletedCount = transactions.stream().filter(t -> t.getDeletedAt() != null).count();

        List<ResourceListResponse.ResourceItem> items = transactions.stream()
                .map(t -> {
                    String clientEmail = t.getClientId() != null
                            ? userAccountRepository.findById(t.getClientId())
                                    .map(u -> u.getEmail()).orElse(null)
                            : null;
                    String brokerEmail = t.getBrokerId() != null
                            ? userAccountRepository.findById(t.getBrokerId())
                                    .map(u -> u.getEmail()).orElse(null)
                            : null;
                    String address = t.getPropertyAddress() != null
                            ? t.getPropertyAddress().getStreet() + ", " + t.getPropertyAddress().getCity()
                            : null;

                    return ResourceListResponse.ResourceItem.builder()
                            .id(t.getTransactionId())
                            .summary(buildTransactionSummary(t))
                            .createdAt(t.getOpenedAt())
                            .deletedAt(t.getDeletedAt())
                            .deletedBy(t.getDeletedBy())
                            .isDeleted(t.getDeletedAt() != null)
                            .clientId(t.getClientId())
                            .clientEmail(clientEmail)
                            .brokerId(t.getBrokerId())
                            .brokerEmail(brokerEmail)
                            .status(t.getStatus() != null ? t.getStatus().name() : null)
                            .side(t.getSide() != null ? t.getSide().name() : null)
                            .address(address)
                            .build();
                })
                .collect(Collectors.toList());

        return ResourceListResponse.builder()
                .resourceType("TRANSACTION")
                .totalCount(items.size())
                .deletedCount((int) deletedCount)
                .items(items)
                .build();
    }

    private ResourceListResponse listDocumentRequests(boolean includeDeleted) {
        List<DocumentRequest> requests = includeDeleted
                ? documentRequestRepository.findAllIncludingDeleted()
                : documentRequestRepository.findAll();

        long deletedCount = requests.stream().filter(d -> d.getDeletedAt() != null).count();

        List<ResourceListResponse.ResourceItem> items = requests.stream()
                .map(d -> {
                    UUID transactionId = d.getTransactionRef() != null
                            ? d.getTransactionRef().getTransactionId()
                            : null;

                    return ResourceListResponse.ResourceItem.builder()
                            .id(d.getRequestId())
                            .summary(buildDocumentRequestSummary(d))
                            .createdAt(d.getLastUpdatedAt())
                            .deletedAt(d.getDeletedAt())
                            .deletedBy(d.getDeletedBy())
                            .isDeleted(d.getDeletedAt() != null)
                            .transactionId(transactionId)
                            .docType(d.getDocType() != null ? d.getDocType().name() : d.getCustomTitle())
                            .submittedDocCount(d.getSubmittedDocuments() != null ? d.getSubmittedDocuments().size() : 0)
                            .build();
                })
                .collect(Collectors.toList());

        return ResourceListResponse.builder()
                .resourceType("DOCUMENT_REQUEST")
                .totalCount(items.size())
                .deletedCount((int) deletedCount)
                .items(items)
                .build();
    }

    @Override
    public DeletionPreviewResponse previewDeletion(AdminDeletionAuditLog.ResourceType type, UUID resourceId) {
        return switch (type) {
            case TRANSACTION -> previewTransactionDeletion(resourceId);
            case DOCUMENT_REQUEST -> previewDocumentRequestDeletion(resourceId);
            default -> throw new BadRequestException("Resource type not supported for deletion: " + type);
        };
    }

    private DeletionPreviewResponse previewTransactionDeletion(UUID transactionId) {
        Transaction transaction = transactionRepository.findByTransactionIdIncludingDeleted(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        List<DeletionPreviewResponse.LinkedResource> linked = new ArrayList<>();
        List<DeletionPreviewResponse.S3FileToDelete> s3Files = new ArrayList<>();

        // Timeline entries
        // Timeline entries
        List<TimelineEntry> timeline = timelineEntryRepository.findByTransactionIdIncludingDeleted(transactionId);
        for (TimelineEntry entry : timeline) {
            linked.add(DeletionPreviewResponse.LinkedResource.builder()
                    .type("TIMELINE_ENTRY")
                    .id(null)
                    .summary(entry.getType() != null ? entry.getType().name() : "Unknown Timeline Entry")
                    .build());
        }

        // Document requests and their submitted documents
        List<DocumentRequest> docRequests = documentRequestRepository
                .findByTransactionIdIncludingDeleted(transactionId);
        for (DocumentRequest dr : docRequests) {
            linked.add(DeletionPreviewResponse.LinkedResource.builder()
                    .type("DOCUMENT_REQUEST")
                    .id(dr.getRequestId())
                    .summary(buildDocumentRequestSummary(dr))
                    .build());

            for (SubmittedDocument sd : dr.getSubmittedDocuments()) {
                linked.add(DeletionPreviewResponse.LinkedResource.builder()
                        .type("SUBMITTED_DOCUMENT")
                        .id(sd.getDocumentId())
                        .summary(sd.getStorageObject() != null ? sd.getStorageObject().getFileName() : "Unknown file")
                        .build());

                if (sd.getStorageObject() != null && sd.getStorageObject().getS3Key() != null) {
                    s3Files.add(DeletionPreviewResponse.S3FileToDelete.builder()
                            .fileName(sd.getStorageObject().getFileName())
                            .mimeType(sd.getStorageObject().getMimeType())
                            .sizeBytes(sd.getStorageObject().getSizeBytes())
                            .build());
                }
            }
        }

        return DeletionPreviewResponse.builder()
                .resourceId(transactionId)
                .resourceType("TRANSACTION")
                .resourceSummary(buildTransactionSummary(transaction))
                .linkedResources(linked)
                .s3FilesToDelete(s3Files)
                .build();
    }

    private DeletionPreviewResponse previewDocumentRequestDeletion(UUID requestId) {
        DocumentRequest request = documentRequestRepository.findByRequestIdIncludingDeleted(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        List<DeletionPreviewResponse.LinkedResource> linked = new ArrayList<>();
        List<DeletionPreviewResponse.S3FileToDelete> s3Files = new ArrayList<>();

        for (SubmittedDocument sd : request.getSubmittedDocuments()) {
            linked.add(DeletionPreviewResponse.LinkedResource.builder()
                    .type("SUBMITTED_DOCUMENT")
                    .id(sd.getDocumentId())
                    .summary(sd.getStorageObject() != null ? sd.getStorageObject().getFileName() : "Unknown file")
                    .build());

            if (sd.getStorageObject() != null && sd.getStorageObject().getS3Key() != null) {
                s3Files.add(DeletionPreviewResponse.S3FileToDelete.builder()
                        .fileName(sd.getStorageObject().getFileName())
                        .mimeType(sd.getStorageObject().getMimeType())
                        .sizeBytes(sd.getStorageObject().getSizeBytes())
                        .build());
            }
        }

        return DeletionPreviewResponse.builder()
                .resourceId(requestId)
                .resourceType("DOCUMENT_REQUEST")
                .resourceSummary(buildDocumentRequestSummary(request))
                .linkedResources(linked)
                .s3FilesToDelete(s3Files)
                .build();
    }

    @Override
    @Transactional
    public void deleteResource(AdminDeletionAuditLog.ResourceType type, UUID resourceId, UUID adminId) {
        log.info("Admin {} deleting {} with ID {}", adminId, type, resourceId);

        switch (type) {
            case TRANSACTION -> deleteTransaction(resourceId, adminId);
            case DOCUMENT_REQUEST -> deleteDocumentRequest(resourceId, adminId);
            default -> throw new BadRequestException("Resource type not supported for deletion: " + type);
        }
    }

    private void deleteTransaction(UUID transactionId, UUID adminId) {
        Transaction transaction = transactionRepository.findByTransactionIdIncludingDeleted(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (transaction.getDeletedAt() != null) {
            throw new BadRequestException("Transaction is already deleted");
        }

        // Notify users before deletion (Best effort)
        try {
            String title = "Transaction Cancelled";
            String propertyRef = transaction.getPropertyAddress() != null
                    ? transaction.getPropertyAddress().getStreet()
                    : "ID: " + transactionId.toString();

            String message = String.format("Transaction for %s has been cancelled by an administrator.", propertyRef);

            // Notify Broker
            if (transaction.getBrokerId() != null) {
                notificationService.createNotification(
                        transaction.getBrokerId().toString(),
                        title,
                        message,
                        transactionId.toString(),
                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.TRANSACTION_CANCELLED);
            }

            // Notify Client
            if (transaction.getClientId() != null) {
                notificationService.createNotification(
                        transaction.getClientId().toString(),
                        title,
                        message,
                        transactionId.toString(),
                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.TRANSACTION_CANCELLED);
            }
        } catch (Exception e) {
            log.error("Failed to send deletion notifications for transaction {}", transactionId, e);
            // Proceed with deletion even if notification fails
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> cascaded = new ArrayList<>();

        // Soft-delete timeline entries
        // Soft-delete timeline entries
        List<TimelineEntry> timeline = timelineEntryRepository.findByTransactionIdIncludingDeleted(transactionId);
        for (TimelineEntry entry : timeline) {
            entry.setDeletedAt(now);
            entry.setDeletedBy(adminId);
            timelineEntryRepository.save(entry);
            cascaded.add("TimelineEntry:" + entry.getId());
        }

        // Soft-delete document requests and hard-delete S3 files
        List<DocumentRequest> docRequests = documentRequestRepository
                .findByTransactionIdIncludingDeleted(transactionId);
        for (DocumentRequest dr : docRequests) {
            for (SubmittedDocument sd : dr.getSubmittedDocuments()) {
                // Hard-delete S3 file
                if (sd.getStorageObject() != null && sd.getStorageObject().getS3Key() != null) {
                    try {
                        s3StorageService.deleteFile(sd.getStorageObject().getS3Key());
                    } catch (Exception e) {
                        log.error("Failed to delete S3 file: {}", sd.getStorageObject().getS3Key(), e);
                    }
                }
                sd.setDeletedAt(now);
                sd.setDeletedBy(adminId);
                cascaded.add("SubmittedDocument:" + sd.getDocumentId());
            }
            dr.setDeletedAt(now);
            dr.setDeletedBy(adminId);
            cascaded.add("DocumentRequest:" + dr.getRequestId());
        }

        // Soft-delete the transaction
        transaction.setDeletedAt(now);
        transaction.setDeletedBy(adminId);
        transactionRepository.save(transaction);

        // Create audit log
        createAuditLog(AdminDeletionAuditLog.ActionType.DELETE,
                AdminDeletionAuditLog.ResourceType.TRANSACTION, transactionId, adminId,
                buildTransactionSnapshot(transaction), cascaded);
    }

    private void deleteDocumentRequest(UUID requestId, UUID adminId) {
        DocumentRequest request = documentRequestRepository.findByRequestIdIncludingDeleted(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        if (request.getDeletedAt() != null) {
            throw new BadRequestException("Document request is already deleted");
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> cascaded = new ArrayList<>();

        // Hard-delete S3 files and soft-delete submitted documents
        for (SubmittedDocument sd : request.getSubmittedDocuments()) {
            if (sd.getStorageObject() != null && sd.getStorageObject().getS3Key() != null) {
                try {
                    s3StorageService.deleteFile(sd.getStorageObject().getS3Key());
                } catch (Exception e) {
                    log.error("Failed to delete S3 file: {}", sd.getStorageObject().getS3Key(), e);
                }
            }
            sd.setDeletedAt(now);
            sd.setDeletedBy(adminId);
            cascaded.add("SubmittedDocument:" + sd.getDocumentId());
        }

        request.setDeletedAt(now);
        request.setDeletedBy(adminId);
        documentRequestRepository.save(request);

        createAuditLog(AdminDeletionAuditLog.ActionType.DELETE,
                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, requestId, adminId,
                buildDocumentRequestSnapshot(request), cascaded);
    }

    @Override
    @Transactional
    public void restoreResource(AdminDeletionAuditLog.ResourceType type, UUID resourceId, UUID adminId) {
        log.info("Admin {} restoring {} with ID {}", adminId, type, resourceId);

        switch (type) {
            case TRANSACTION -> restoreTransaction(resourceId, adminId);
            case DOCUMENT_REQUEST -> restoreDocumentRequest(resourceId, adminId);
            default -> throw new BadRequestException("Resource type not supported for restoration: " + type);
        }
    }

    private void restoreTransaction(UUID transactionId, UUID adminId) {
        Transaction transaction = transactionRepository.findByTransactionIdIncludingDeleted(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (transaction.getDeletedAt() == null) {
            throw new BadRequestException("Transaction is not deleted");
        }

        List<String> cascaded = new ArrayList<>();

        // Restore timeline entries
        // Restore timeline entries
        List<TimelineEntry> timeline = timelineEntryRepository.findByTransactionIdIncludingDeleted(transactionId);
        for (TimelineEntry entry : timeline) {
            if (entry.getDeletedAt() != null) { // Only restore if it was deleted
                entry.setDeletedAt(null);
                entry.setDeletedBy(null);
                timelineEntryRepository.save(entry);
            }
        }

        // Restore document requests (S3 files cannot be recovered)
        List<DocumentRequest> docRequests = documentRequestRepository
                .findByTransactionIdIncludingDeleted(transactionId);
        for (DocumentRequest dr : docRequests) {
            for (SubmittedDocument sd : dr.getSubmittedDocuments()) {
                sd.setDeletedAt(null);
                sd.setDeletedBy(null);
                cascaded.add("SubmittedDocument:" + sd.getDocumentId());
                // Note: S3 files are gone, metadata is restored but files are not recoverable
            }
            dr.setDeletedAt(null);
            dr.setDeletedBy(null);
            cascaded.add("DocumentRequest:" + dr.getRequestId());
        }

        transaction.setDeletedAt(null);
        transaction.setDeletedBy(null);
        transactionRepository.save(transaction);

        // Create restore audit log
        createAuditLog(AdminDeletionAuditLog.ActionType.RESTORE,
                AdminDeletionAuditLog.ResourceType.TRANSACTION, transactionId, adminId,
                buildTransactionSnapshot(transaction), cascaded);
    }

    private void restoreDocumentRequest(UUID requestId, UUID adminId) {
        DocumentRequest request = documentRequestRepository.findByRequestIdIncludingDeleted(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        if (request.getDeletedAt() == null) {
            throw new BadRequestException("Document request is not deleted");
        }

        // Check if parent transaction is deleted - can't restore orphaned document
        // requests
        UUID transactionId = request.getTransactionRef().getTransactionId();
        Transaction parentTransaction = transactionRepository.findByTransactionIdIncludingDeleted(transactionId)
                .orElse(null);

        if (parentTransaction != null && parentTransaction.getDeletedAt() != null) {
            throw new BadRequestException(
                    "Cannot restore document request: its parent transaction " + transactionId + " is deleted. " +
                            "Restore the transaction first.");
        }

        List<String> cascaded = new ArrayList<>();

        for (SubmittedDocument sd : request.getSubmittedDocuments()) {
            sd.setDeletedAt(null);
            sd.setDeletedBy(null);
            cascaded.add("SubmittedDocument:" + sd.getDocumentId());
        }

        request.setDeletedAt(null);
        request.setDeletedBy(null);
        documentRequestRepository.save(request);

        // Create restore audit log
        createAuditLog(AdminDeletionAuditLog.ActionType.RESTORE,
                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, requestId, adminId,
                buildDocumentRequestSnapshot(request), cascaded);
    }

    @Override
    public List<AdminDeletionAuditLog> getAuditHistory() {
        return auditRepository.findAllByOrderByTimestampDesc();
    }

    // Helper methods

    private String buildTransactionSummary(Transaction t) {
        return t.getPropertyAddress() != null
                ? t.getPropertyAddress().getStreet() + ", " + t.getPropertyAddress().getCity()
                : "No address";
    }

    private String buildDocumentRequestSummary(DocumentRequest d) {
        String title = d.getCustomTitle() != null ? d.getCustomTitle()
                : (d.getDocType() != null ? d.getDocType().name() : "Unknown");
        return String.format("%s (%s)", title, d.getStatus() != null ? d.getStatus().name() : "UNKNOWN");
    }

    private String buildTransactionSnapshot(Transaction t) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("transactionId", t.getTransactionId());
            snapshot.put("clientId", t.getClientId());
            snapshot.put("brokerId", t.getBrokerId());
            snapshot.put("status", t.getStatus() != null ? t.getStatus().name() : null);
            snapshot.put("side", t.getSide() != null ? t.getSide().name() : null);
            if (t.getPropertyAddress() != null) {
                snapshot.put("address", t.getPropertyAddress().getStreet() + ", " + t.getPropertyAddress().getCity());
            }
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transaction snapshot", e);
            return "{}";
        }
    }

    private String buildDocumentRequestSnapshot(DocumentRequest d) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("requestId", d.getRequestId());
            snapshot.put("docType", d.getDocType() != null ? d.getDocType().name() : null);
            snapshot.put("customTitle", d.getCustomTitle());
            snapshot.put("status", d.getStatus() != null ? d.getStatus().name() : null);
            snapshot.put("submittedDocumentCount", d.getSubmittedDocuments().size());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize document request snapshot", e);
            return "{}";
        }
    }

    private void createAuditLog(AdminDeletionAuditLog.ActionType action,
            AdminDeletionAuditLog.ResourceType type,
            UUID resourceId,
            UUID adminId,
            String snapshot,
            List<String> cascaded) {
        try {
            AdminDeletionAuditLog log = AdminDeletionAuditLog.builder()
                    .action(action)
                    .timestamp(LocalDateTime.now())
                    .adminId(adminId)
                    .resourceType(type)
                    .resourceId(resourceId)
                    .resourceSnapshot(snapshot)
                    .cascadedDeletions(objectMapper.writeValueAsString(cascaded))
                    .build();
            auditRepository.save(log);
        } catch (JsonProcessingException e) {
            AdminResourceServiceImpl.log.error("Failed to serialize cascaded operations", e);
        }
    }
}
