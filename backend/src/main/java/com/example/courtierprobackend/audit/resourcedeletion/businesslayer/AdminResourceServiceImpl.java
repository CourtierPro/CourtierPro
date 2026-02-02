package com.example.courtierprobackend.audit.resourcedeletion.businesslayer;

import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditLog;
import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditRepository;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.DeletionPreviewResponse;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.ResourceListResponse;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.documents.datalayer.DocumentVersion;
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
    private final DocumentRepository documentRepository;
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
            case DOCUMENT_REQUEST -> listDocuments(includeDeleted);
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

    private ResourceListResponse listDocuments(boolean includeDeleted) {
        List<Document> documents = includeDeleted
                ? documentRepository.findAllIncludingDeleted()
                : documentRepository.findAll();

        long deletedCount = documents.stream().filter(d -> d.getDeletedAt() != null).count();

        List<ResourceListResponse.ResourceItem> items = documents.stream()
                .map(d -> {
                    UUID transactionId = d.getTransactionRef() != null
                            ? d.getTransactionRef().getTransactionId()
                            : null;

                    return ResourceListResponse.ResourceItem.builder()
                            .id(d.getDocumentId())
                            .summary(buildDocumentSummary(d))
                            .createdAt(d.getLastUpdatedAt())
                            .deletedAt(d.getDeletedAt())
                            .deletedBy(d.getDeletedBy())
                            .isDeleted(d.getDeletedAt() != null)
                            .transactionId(transactionId)
                            .docType(d.getDocType() != null ? d.getDocType().name() : d.getCustomTitle())
                            .submittedDocCount(d.getVersions() != null ? d.getVersions().size() : 0)
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
            case DOCUMENT_REQUEST -> previewDocumentDeletion(resourceId);
            default -> throw new BadRequestException("Resource type not supported for deletion: " + type);
        };
    }

    private DeletionPreviewResponse previewTransactionDeletion(UUID transactionId) {
        Transaction transaction = transactionRepository.findByTransactionIdIncludingDeleted(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        List<DeletionPreviewResponse.LinkedResource> linked = new ArrayList<>();
        List<DeletionPreviewResponse.S3FileToDelete> s3Files = new ArrayList<>();

        // Timeline entries
        List<TimelineEntry> timeline = timelineEntryRepository.findByTransactionIdIncludingDeleted(transactionId);
        for (TimelineEntry entry : timeline) {
            linked.add(DeletionPreviewResponse.LinkedResource.builder()
                    .type("TIMELINE_ENTRY")
                    .id(null)
                    .summary(entry.getType() != null ? entry.getType().name() : "Unknown Timeline Entry")
                    .build());
        }

        // Documents and their versions
        List<Document> documents = documentRepository
                .findByTransactionIdIncludingDeleted(transactionId);
        for (Document doc : documents) {
            linked.add(DeletionPreviewResponse.LinkedResource.builder()
                    .type("DOCUMENT")
                    .id(doc.getDocumentId())
                    .summary(buildDocumentSummary(doc))
                    .build());

            for (DocumentVersion version : doc.getVersions()) {
                linked.add(DeletionPreviewResponse.LinkedResource.builder()
                        .type("DOCUMENT_VERSION")
                        .id(version.getVersionId())
                        .summary(version.getStorageObject() != null ? version.getStorageObject().getFileName() : "Unknown file")
                        .build());

                if (version.getStorageObject() != null && version.getStorageObject().getS3Key() != null) {
                    s3Files.add(DeletionPreviewResponse.S3FileToDelete.builder()
                            .fileName(version.getStorageObject().getFileName())
                            .mimeType(version.getStorageObject().getMimeType())
                            .sizeBytes(version.getStorageObject().getSizeBytes())
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

    private DeletionPreviewResponse previewDocumentDeletion(UUID documentId) {
        Document document = documentRepository.findByDocumentIdIncludingDeleted(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

        List<DeletionPreviewResponse.LinkedResource> linked = new ArrayList<>();
        List<DeletionPreviewResponse.S3FileToDelete> s3Files = new ArrayList<>();

        for (DocumentVersion version : document.getVersions()) {
            linked.add(DeletionPreviewResponse.LinkedResource.builder()
                    .type("DOCUMENT_VERSION")
                    .id(version.getVersionId())
                    .summary(version.getStorageObject() != null ? version.getStorageObject().getFileName() : "Unknown file")
                    .build());

            if (version.getStorageObject() != null && version.getStorageObject().getS3Key() != null) {
                s3Files.add(DeletionPreviewResponse.S3FileToDelete.builder()
                        .fileName(version.getStorageObject().getFileName())
                        .mimeType(version.getStorageObject().getMimeType())
                        .sizeBytes(version.getStorageObject().getSizeBytes())
                        .build());
            }
        }

        return DeletionPreviewResponse.builder()
                .resourceId(documentId)
                .resourceType("DOCUMENT_REQUEST")
                .resourceSummary(buildDocumentSummary(document))
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
            case DOCUMENT_REQUEST -> deleteDocument(resourceId, adminId);
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
        List<TimelineEntry> timeline = timelineEntryRepository.findByTransactionIdIncludingDeleted(transactionId);
        for (TimelineEntry entry : timeline) {
            entry.setDeletedAt(now);
            entry.setDeletedBy(adminId);
            timelineEntryRepository.save(entry);
            cascaded.add("TimelineEntry:" + entry.getId());
        }

        // Soft-delete documents and hard-delete S3 files
        List<Document> documents = documentRepository
                .findByTransactionIdIncludingDeleted(transactionId);
        for (Document doc : documents) {
            for (DocumentVersion version : doc.getVersions()) {
                // Hard-delete S3 file
                if (version.getStorageObject() != null && version.getStorageObject().getS3Key() != null) {
                    try {
                        s3StorageService.deleteFile(version.getStorageObject().getS3Key());
                    } catch (Exception e) {
                        log.error("Failed to delete S3 file: {}", version.getStorageObject().getS3Key(), e);
                    }
                }
                version.setDeletedAt(now);
                version.setDeletedBy(adminId);
                cascaded.add("DocumentVersion:" + version.getVersionId());
            }
            doc.setDeletedAt(now);
            doc.setDeletedBy(adminId);
            cascaded.add("Document:" + doc.getDocumentId());
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

    private void deleteDocument(UUID documentId, UUID adminId) {
        Document document = documentRepository.findByDocumentIdIncludingDeleted(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

        if (document.getDeletedAt() != null) {
            throw new BadRequestException("Document is already deleted");
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> cascaded = new ArrayList<>();

        // Hard-delete S3 files and soft-delete document versions
        for (DocumentVersion version : document.getVersions()) {
            if (version.getStorageObject() != null && version.getStorageObject().getS3Key() != null) {
                try {
                    s3StorageService.deleteFile(version.getStorageObject().getS3Key());
                } catch (Exception e) {
                    log.error("Failed to delete S3 file: {}", version.getStorageObject().getS3Key(), e);
                }
            }
            version.setDeletedAt(now);
            version.setDeletedBy(adminId);
            cascaded.add("DocumentVersion:" + version.getVersionId());
        }

        document.setDeletedAt(now);
        document.setDeletedBy(adminId);
        documentRepository.save(document);

        createAuditLog(AdminDeletionAuditLog.ActionType.DELETE,
                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, documentId, adminId,
                buildDocumentSnapshot(document), cascaded);
    }

    @Override
    @Transactional
    public void restoreResource(AdminDeletionAuditLog.ResourceType type, UUID resourceId, UUID adminId) {
        log.info("Admin {} restoring {} with ID {}", adminId, type, resourceId);

        switch (type) {
            case TRANSACTION -> restoreTransaction(resourceId, adminId);
            case DOCUMENT_REQUEST -> restoreDocument(resourceId, adminId);
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
        List<TimelineEntry> timeline = timelineEntryRepository.findByTransactionIdIncludingDeleted(transactionId);
        for (TimelineEntry entry : timeline) {
            if (entry.getDeletedAt() != null) { // Only restore if it was deleted
                entry.setDeletedAt(null);
                entry.setDeletedBy(null);
                timelineEntryRepository.save(entry);
            }
        }

        // Restore documents (S3 files cannot be recovered)
        List<Document> documents = documentRepository
                .findByTransactionIdIncludingDeleted(transactionId);
        for (Document doc : documents) {
            for (DocumentVersion version : doc.getVersions()) {
                version.setDeletedAt(null);
                version.setDeletedBy(null);
                cascaded.add("DocumentVersion:" + version.getVersionId());
                // Note: S3 files are gone, metadata is restored but files are not recoverable
            }
            doc.setDeletedAt(null);
            doc.setDeletedBy(null);
            cascaded.add("Document:" + doc.getDocumentId());
        }

        transaction.setDeletedAt(null);
        transaction.setDeletedBy(null);
        transactionRepository.save(transaction);

        // Create restore audit log
        createAuditLog(AdminDeletionAuditLog.ActionType.RESTORE,
                AdminDeletionAuditLog.ResourceType.TRANSACTION, transactionId, adminId,
                buildTransactionSnapshot(transaction), cascaded);
    }

    private void restoreDocument(UUID documentId, UUID adminId) {
        Document document = documentRepository.findByDocumentIdIncludingDeleted(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

        if (document.getDeletedAt() == null) {
            throw new BadRequestException("Document is not deleted");
        }

        // Check if parent transaction is deleted - can't restore orphaned documents
        UUID transactionId = document.getTransactionRef().getTransactionId();
        Transaction parentTransaction = transactionRepository.findByTransactionIdIncludingDeleted(transactionId)
                .orElse(null);

        if (parentTransaction != null && parentTransaction.getDeletedAt() != null) {
            throw new BadRequestException(
                    "Cannot restore document: its parent transaction " + transactionId + " is deleted. " +
                            "Restore the transaction first.");
        }

        List<String> cascaded = new ArrayList<>();

        for (DocumentVersion version : document.getVersions()) {
            version.setDeletedAt(null);
            version.setDeletedBy(null);
            cascaded.add("DocumentVersion:" + version.getVersionId());
        }

        document.setDeletedAt(null);
        document.setDeletedBy(null);
        documentRepository.save(document);

        // Create restore audit log
        createAuditLog(AdminDeletionAuditLog.ActionType.RESTORE,
                AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST, documentId, adminId,
                buildDocumentSnapshot(document), cascaded);
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

    private String buildDocumentSummary(Document d) {
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

    private String buildDocumentSnapshot(Document d) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("documentId", d.getDocumentId());
            snapshot.put("docType", d.getDocType() != null ? d.getDocType().name() : null);
            snapshot.put("customTitle", d.getCustomTitle());
            snapshot.put("status", d.getStatus() != null ? d.getStatus().name() : null);
            snapshot.put("versionCount", d.getVersions().size());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize document snapshot", e);
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
