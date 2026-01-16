package com.example.courtierprobackend.documents.businesslayer;
import com.example.courtierprobackend.documents.datalayer.enums.*;

import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.SubmittedDocument;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.datalayer.valueobjects.UploadedBy;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.SubmittedDocumentResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.example.courtierprobackend.transactions.businesslayer.TransactionAccessUtils;
import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import org.springframework.context.MessageSource;
import java.util.Locale;
import com.example.courtierprobackend.transactions.datalayer.DocumentConditionLink;
import com.example.courtierprobackend.transactions.datalayer.repositories.DocumentConditionLinkRepository;

@Service
@RequiredArgsConstructor
public class DocumentRequestServiceImpl implements DocumentRequestService {
        private static final Logger logger = LoggerFactory.getLogger(DocumentRequestServiceImpl.class);

        private final DocumentRequestRepository repository;
        private final S3StorageService storageService;
        private final EmailService emailService;
        private final NotificationService notificationService;
        private final TransactionRepository transactionRepository;
        private final UserAccountRepository userAccountRepository;
        private final TimelineService timelineService;
        private final MessageSource messageSource;
        private final DocumentConditionLinkRepository documentConditionLinkRepository;

        /**
         * Helper to find a UserAccount by internal UUID.
         * Now uses UUID directly since database enforces UUID type.
         */
        private Optional<UserAccount> resolveUserAccount(UUID id) {
                if (id == null)
                        return Optional.empty();
                return userAccountRepository.findById(id);
        }

        @Override
        public List<DocumentRequestResponseDTO> getDocumentsForTransaction(UUID transactionId, UUID userId) {
                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                TransactionAccessUtils.verifyTransactionAccess(tx, userId);

                return repository.findByTransactionRef_TransactionId(transactionId).stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public List<DocumentRequestResponseDTO> getAllDocumentsForUser(UUID userId) {
                return repository.findByUserId(userId).stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public DocumentRequestResponseDTO getDocumentRequest(UUID requestId, UUID userId) {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                Transaction tx = transactionRepository
                                .findByTransactionId(request.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                TransactionAccessUtils.verifyTransactionAccess(tx, userId);

                return mapToResponseDTO(request);
        }

        @Transactional
        @Override
        public DocumentRequestResponseDTO createDocumentRequest(UUID transactionId,
                        DocumentRequestRequestDTO requestDTO) {
                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                DocumentRequest request = new DocumentRequest();
                request.setRequestId(UUID.randomUUID());
                request.setTransactionRef(new TransactionRef(transactionId, tx.getClientId(), tx.getSide()));
                request.setDocType(requestDTO.getDocType());
                request.setCustomTitle(requestDTO.getCustomTitle());
                request.setStatus(DocumentStatusEnum.REQUESTED);
                request.setExpectedFrom(requestDTO.getExpectedFrom());
                request.setVisibleToClient(
                                requestDTO.getVisibleToClient() != null ? requestDTO.getVisibleToClient() : true);
                request.setBrokerNotes(requestDTO.getBrokerNotes());
                request.setLastUpdatedAt(LocalDateTime.now());
                // Add stage mapping
                request.setStage(requestDTO.getStage());

                DocumentRequest savedRequest = repository.save(request);

                // Save condition links if provided
                if (requestDTO.getConditionIds() != null && !requestDTO.getConditionIds().isEmpty()) {
                        for (UUID conditionId : requestDTO.getConditionIds()) {
                                DocumentConditionLink link = DocumentConditionLink.builder()
                                        .conditionId(conditionId)
                                        .documentRequestId(savedRequest.getRequestId())
                                        .build();
                                documentConditionLinkRepository.save(link);
                        }
                }

                // Add timeline entry for document requested
                timelineService.addEntry(
                                transactionId,
                                tx.getBrokerId(),
                                TimelineEntryType.DOCUMENT_REQUESTED,
                                "Document requested: " + requestDTO.getDocType(),
                                requestDTO.getDocType() != null ? requestDTO.getDocType().toString() : null);

                // Notify client via email
                // Resolve Client and Broker robustly
                UserAccount client = resolveUserAccount(tx.getClientId()).orElse(null);
                UserAccount broker = resolveUserAccount(tx.getBrokerId()).orElse(null);

                if (client != null && broker != null) {
                        String documentName = requestDTO.getCustomTitle() != null ? requestDTO.getCustomTitle()
                                        : requestDTO.getDocType().toString();
                        String clientName = client.getFirstName() + " " + client.getLastName();
                        String brokerName = broker.getFirstName() + " " + broker.getLastName();
                        String docType = requestDTO.getDocType().toString();
                        String clientLanguage = client.getPreferredLanguage();

                        emailService.sendDocumentRequestedNotification(
                                        client.getEmail(),
                                        clientName,
                                        brokerName,
                                        documentName,
                                        docType,
                                        clientLanguage);

                        // In-app Notification for Client
                        try {
                                // Prepare old/new values for subtitle logic
                                Locale locale = isFrench(clientLanguage) ? Locale.FRENCH : Locale.ENGLISH;
                                String localizedDocType = messageSource.getMessage(
                                                "document.type." + requestDTO.getDocType(), null,
                                                requestDTO.getDocType().name(), locale);
                                String displayDocName = requestDTO.getCustomTitle() != null
                                                ? requestDTO.getCustomTitle()
                                                : localizedDocType;

                        } catch (Exception e) {
                                logger.error("Failed to send in-app notification for document request", e);
                        }
                }

                return mapToResponseDTO(savedRequest);
        }

        @Transactional
        @Override
        public DocumentRequestResponseDTO updateDocumentRequest(UUID requestId, DocumentRequestRequestDTO requestDTO) {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));
        // Helper: normalize string (null, empty, whitespace)
        java.util.function.Function<String, String> normStr = s -> s == null ? "" : s.trim();
        // Helper: normalize enum or string (case-insensitive string compare)
        java.util.function.Function<Object, String> normEnum = o -> o == null ? "" : o.toString().trim().toLowerCase();

        // Build a candidate entity with merged values (normalized)
        DocumentTypeEnum candidateDocType = requestDTO.getDocType() != null ? requestDTO.getDocType() : request.getDocType();
        String candidateCustomTitle = normStr.apply(requestDTO.getCustomTitle()).isEmpty() ? null : requestDTO.getCustomTitle();
        if (candidateCustomTitle == null && request.getCustomTitle() != null) candidateCustomTitle = request.getCustomTitle();
        DocumentPartyEnum candidateExpectedFrom = requestDTO.getExpectedFrom() != null ? requestDTO.getExpectedFrom() : request.getExpectedFrom();
        Boolean candidateVisibleToClient = requestDTO.getVisibleToClient() != null ? requestDTO.getVisibleToClient() : request.isVisibleToClient();
        String candidateBrokerNotes = normStr.apply(requestDTO.getBrokerNotes()).isEmpty() ? null : requestDTO.getBrokerNotes();
        if (candidateBrokerNotes == null && request.getBrokerNotes() != null) candidateBrokerNotes = request.getBrokerNotes();
        StageEnum candidateStage = requestDTO.getStage() != null ? requestDTO.getStage() : request.getStage();

        // Compare normalized candidate to normalized current entity
        boolean isIdentical =
            normEnum.apply(request.getDocType()).equals(normEnum.apply(candidateDocType)) &&
            normStr.apply(request.getCustomTitle()).equals(normStr.apply(candidateCustomTitle)) &&
            normEnum.apply(request.getExpectedFrom()).equals(normEnum.apply(candidateExpectedFrom)) &&
            request.isVisibleToClient() == candidateVisibleToClient &&
            normStr.apply(request.getBrokerNotes()).equals(normStr.apply(candidateBrokerNotes)) &&
            normEnum.apply(request.getStage()).equals(normEnum.apply(candidateStage));
        if (isIdentical) {
            return mapToResponseDTO(request);
        }
        // ...existing code for updating fields and triggering side effects...




                // Only update fields if the normalized value is different from the current normalized value
                if (!normEnum.apply(request.getDocType()).equals(normEnum.apply(requestDTO.getDocType()))) {
                        request.setDocType(requestDTO.getDocType());
                }
                if (!normStr.apply(request.getCustomTitle()).equals(normStr.apply(requestDTO.getCustomTitle()))) {
                        // If both are null/empty, set to null for consistency
                        String newVal = normStr.apply(requestDTO.getCustomTitle());
                        request.setCustomTitle(newVal.isEmpty() ? null : requestDTO.getCustomTitle());
                }
                if (!normEnum.apply(request.getExpectedFrom()).equals(normEnum.apply(requestDTO.getExpectedFrom()))) {
                        request.setExpectedFrom(requestDTO.getExpectedFrom());
                }
                if (requestDTO.getVisibleToClient() != null && request.isVisibleToClient() != requestDTO.getVisibleToClient()) {
                        request.setVisibleToClient(requestDTO.getVisibleToClient());
                }
                if (!normStr.apply(request.getBrokerNotes()).equals(normStr.apply(requestDTO.getBrokerNotes()))) {
                        String newVal = normStr.apply(requestDTO.getBrokerNotes());
                        request.setBrokerNotes(newVal.isEmpty() ? null : requestDTO.getBrokerNotes());
                }
                if (!normEnum.apply(request.getStage()).equals(normEnum.apply(requestDTO.getStage()))) {
                        request.setStage(requestDTO.getStage());
                }

                // Only update lastUpdatedAt, save, and send notifications/timeline if something changed
                request.setLastUpdatedAt(LocalDateTime.now());
                DocumentRequest savedRequest = repository.save(request);
                final UUID timelineTransactionId = savedRequest.getTransactionRef().getTransactionId();
                try {
                        Transaction txForTimeline = transactionRepository.findByTransactionId(timelineTransactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found for timeline entry: " + timelineTransactionId));

                        // Get client language for localization
                        UserAccount clientForTimeline = resolveUserAccount(txForTimeline.getClientId()).orElse(null);
                        UserAccount brokerForTimeline = resolveUserAccount(txForTimeline.getBrokerId()).orElse(null);
                        String clientLanguage = clientForTimeline != null ? clientForTimeline.getPreferredLanguage() : null;

                        // Prepare document name and actor for note
                        // Ensure locale is defined before use
                        clientLanguage = clientForTimeline != null ? clientForTimeline.getPreferredLanguage() : null;
                        Locale locale = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr") ? Locale.FRENCH : Locale.ENGLISH;
                        String localizedDocType = messageSource.getMessage(
                                "document.type." + savedRequest.getDocType(), null,
                                savedRequest.getDocType().name(), locale);
                        String documentName = (savedRequest.getCustomTitle() != null && !savedRequest.getCustomTitle().isEmpty()) ? savedRequest.getCustomTitle() : localizedDocType;
                        String brokerName = brokerForTimeline != null ? brokerForTimeline.getFirstName() + " " + brokerForTimeline.getLastName() : "";
                        // Send a stable, language-agnostic note key and params for frontend i18n
                        String note = String.format("document.details.updated.note|%s|%s", documentName, brokerName);

                        timelineService.addEntry(
                                savedRequest.getTransactionRef().getTransactionId(),
                                txForTimeline.getBrokerId(),
                                TimelineEntryType.DOCUMENT_REQUEST_UPDATED,
                                note, // note: key and params for frontend i18n
                                "document.details.updated" // title: event key for frontend i18n
                        );

                    // Send notification and email to client
                    UserAccount client = resolveUserAccount(txForTimeline.getClientId()).orElse(null);
                    UserAccount broker = resolveUserAccount(txForTimeline.getBrokerId()).orElse(null);
                    if (client != null && broker != null) {
                        String clientName = client.getFirstName() + " " + client.getLastName();
                        String docType = savedRequest.getDocType().toString();

                        // Email (distinct message for edit)
                        emailService.sendDocumentEditedNotification(
                                client.getEmail(),
                                clientName,
                                brokerName,
                                documentName,
                                docType,
                                clientLanguage
                        );

                        // In-app notification (distinct message for edit)
                        localizedDocType = messageSource.getMessage(
                                "document.type." + savedRequest.getDocType(), null,
                                savedRequest.getDocType().name(), locale);
                        String displayDocName = savedRequest.getCustomTitle() != null ? savedRequest.getCustomTitle() : localizedDocType;
                        String title = messageSource.getMessage("notification.document.edited.title", null, locale);
                        String message = messageSource.getMessage("notification.document.edited.message",
                                new Object[] { brokerName, displayDocName }, locale);
                        notificationService.createNotification(
                                client.getId().toString(),
                                title,
                                message,
                                savedRequest.getTransactionRef().getTransactionId().toString(),
                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REQUEST
                        );
                    }
                } catch (Exception e) {
                    logger.warn("Could not add timeline entry or send notification/email for document request update", e);
                }

                savedRequest = repository.save(request);

                // Update condition links if provided
                if (requestDTO.getConditionIds() != null) {
                        documentConditionLinkRepository.deleteByDocumentRequestId(requestId);
                        for (UUID conditionId : requestDTO.getConditionIds()) {
                                DocumentConditionLink link = DocumentConditionLink.builder()
                                        .conditionId(conditionId)
                                        .documentRequestId(savedRequest.getRequestId())
                                        .build();
                                documentConditionLinkRepository.save(link);
                        }
                }

                return mapToResponseDTO(savedRequest);
        }

        @Transactional
        @Override
        public void deleteDocumentRequest(UUID requestId) {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));
                repository.delete(request);
        }

        @Transactional
        @Override
        public DocumentRequestResponseDTO submitDocument(UUID transactionId, UUID requestId, MultipartFile file,
                        UUID uploaderId, UploadedByRefEnum uploaderType) throws IOException {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                if (!request.getTransactionRef().getTransactionId().equals(transactionId)) {
                        throw new BadRequestException(
                                        "Document request does not belong to transaction: " + transactionId);
                }

                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                TransactionAccessUtils.verifyTransactionAccess(tx, uploaderId);

                StorageObject storageObject = storageService.uploadFile(file, transactionId, requestId);

                UploadedBy uploadedBy = UploadedBy.builder()
                                .uploaderType(uploaderType)
                                .uploaderId(uploaderId)
                                .party(request.getExpectedFrom())
                                .build();

                SubmittedDocument submission = SubmittedDocument.builder()
                                .documentId(UUID.randomUUID())
                                .uploadedAt(LocalDateTime.now())
                                .uploadedBy(uploadedBy)
                                .storageObject(storageObject)
                                .documentRequest(request)
                                .build();

                request.getSubmittedDocuments().add(submission);
                request.setStatus(DocumentStatusEnum.SUBMITTED);
                request.setLastUpdatedAt(LocalDateTime.now());

                DocumentRequest savedRequest = repository.save(request);
                // Add timeline entry for document submitted
                timelineService.addEntry(
                                transactionId,
                                uploaderId,
                                TimelineEntryType.DOCUMENT_SUBMITTED,
                                "Document submitted: "
                                                + (savedRequest.getCustomTitle() != null ? savedRequest.getCustomTitle()
                                                                : savedRequest.getDocType()),
                                savedRequest.getDocType() != null ? savedRequest.getDocType().toString() : null);

                // Notify broker
                UserAccount broker = resolveUserAccount(tx.getBrokerId())
                                .orElseThrow(() -> new NotFoundException("Broker not found: " + tx.getBrokerId()));

                // Get uploader name
                String uploaderName = "Unknown Client";
                if (uploaderType == UploadedByRefEnum.CLIENT) {
                        uploaderName = resolveUserAccount(uploaderId)
                                        .map(u -> u.getFirstName() + " " + u.getLastName())
                                        .orElse("Unknown Client");
                }

                String documentName = savedRequest.getCustomTitle() != null ? savedRequest.getCustomTitle()
                                : savedRequest.getDocType().toString();
                String docType = savedRequest.getDocType().toString();
                String brokerLanguage = broker.getPreferredLanguage() != null ? broker.getPreferredLanguage() : "en";

                emailService.sendDocumentSubmittedNotification(savedRequest, broker.getEmail(), uploaderName,
                                documentName, docType, brokerLanguage);

                // In-app Notification for Broker
                try {
                        Locale locale = isFrench(brokerLanguage) ? Locale.FRENCH : Locale.ENGLISH;
                        String localizedDocType = messageSource.getMessage(
                                        "document.type." + savedRequest.getDocType(), null,
                                        savedRequest.getDocType().name(), locale);
                        String displayDocName = savedRequest.getCustomTitle() != null
                                        ? savedRequest.getCustomTitle()
                                        : localizedDocType;

                        String title = messageSource.getMessage("notification.document.submitted.title", null, locale);
                        String message = messageSource.getMessage("notification.document.submitted.message",
                                        new Object[] { uploaderName, displayDocName }, locale);
                        notificationService.createNotification(
                                        broker.getId().toString(),
                                        title,
                                        message,
                                        transactionId.toString(),
                                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_SUBMITTED);
                } catch (Exception e) {
                        logger.error("Failed to send in-app notification for document submission", e);
                }

                return mapToResponseDTO(savedRequest);
        }

        private DocumentRequestResponseDTO mapToResponseDTO(DocumentRequest request) {
                return DocumentRequestResponseDTO.builder()
                        .requestId(request.getRequestId())
                        .transactionRef(request.getTransactionRef())
                        .docType(request.getDocType())
                        .customTitle(request.getCustomTitle())
                        .status(request.getStatus())
                        .expectedFrom(request.getExpectedFrom())
                        .submittedDocuments(request.getSubmittedDocuments() != null ?
                                request.getSubmittedDocuments().stream().map(this::mapToSubmittedDocumentDTO).collect(Collectors.toList()) : null)
                        .brokerNotes(request.getBrokerNotes())
                        .lastUpdatedAt(request.getLastUpdatedAt())
                        .visibleToClient(request.isVisibleToClient())
                        .stage(request.getStage()) // Add stage field to response
                        .build();
        }

        private SubmittedDocumentResponseDTO mapToSubmittedDocumentDTO(SubmittedDocument submission) {
                return SubmittedDocumentResponseDTO.builder()
                                .documentId(submission.getDocumentId())
                                .uploadedAt(submission.getUploadedAt())
                                .uploadedBy(submission.getUploadedBy())
                                .storageObject(submission.getStorageObject())
                                .build();
        }

        @Override
        public String getDocumentDownloadUrl(UUID requestId, UUID documentId, UUID userId) {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                Transaction tx = transactionRepository
                                .findByTransactionId(request.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                TransactionAccessUtils.verifyTransactionAccess(tx, userId);

                SubmittedDocument submittedDocument = request.getSubmittedDocuments().stream()
                                .filter(doc -> doc.getDocumentId().equals(documentId))
                                .findFirst()
                                .orElseThrow(() -> new NotFoundException(
                                                "Submitted document not found: " + documentId));

                return storageService.generatePresignedUrl(submittedDocument.getStorageObject().getS3Key());
        }

        @Transactional
        @Override
        public DocumentRequestResponseDTO reviewDocument(UUID transactionId, UUID requestId,
                        DocumentReviewRequestDTO reviewDTO, UUID brokerId) {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                if (!request.getTransactionRef().getTransactionId().equals(transactionId)) {
                        throw new BadRequestException("Document does not belong to this transaction");
                }

                if (request.getStatus() != DocumentStatusEnum.SUBMITTED) {
                        throw new BadRequestException("Only submitted documents can be reviewed");
                }

                request.setStatus(reviewDTO.getDecision());
                request.setBrokerNotes(reviewDTO.getComments());
                request.setLastUpdatedAt(LocalDateTime.now());

                DocumentRequest updated = repository.save(request);
                // Add timeline entry for document reviewed
                TimelineEntryType timelineType;
                if (reviewDTO.getDecision() == DocumentStatusEnum.NEEDS_REVISION) {
                        timelineType = TimelineEntryType.DOCUMENT_NEEDS_REVISION;
                } else if (reviewDTO.getDecision() == DocumentStatusEnum.APPROVED) {
                        timelineType = TimelineEntryType.DOCUMENT_APPROVED;
                } else {
                        // fallback for other statuses, default to DOCUMENT_APPROVED for now
                        timelineType = TimelineEntryType.DOCUMENT_APPROVED;
                }
                timelineService.addEntry(
                                transactionId,
                                brokerId,
                                timelineType,
                                "Document reviewed: "
                                                + (updated.getCustomTitle() != null ? updated.getCustomTitle()
                                                                : updated.getDocType())
                                                + ", decision: " + reviewDTO.getDecision(),
                                updated.getDocType() != null ? updated.getDocType().toString() : null);

                // Send email notification
                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                UserAccount client = resolveUserAccount(tx.getClientId()).orElse(null);
                UserAccount broker = resolveUserAccount(tx.getBrokerId()).orElse(null);

                if (client != null && broker != null) {
                        String documentName = updated.getCustomTitle() != null ? updated.getCustomTitle()
                                        : updated.getDocType().toString();
                        String brokerName = broker.getFirstName() + " " + broker.getLastName();
                        String docType = updated.getDocType().toString();
                        String clientLanguage = client.getPreferredLanguage() != null ? client.getPreferredLanguage()
                                        : "en";

                        emailService.sendDocumentStatusUpdatedNotification(
                                        updated,
                                        client.getEmail(),
                                        brokerName,
                                        documentName,
                                        docType,
                                        clientLanguage);

                        // In-app Notification for Client
                        // Trigger for APPROVED, NEEDS_REVISION, or REJECTED
                        if (updated.getStatus() == DocumentStatusEnum.APPROVED
                                        || updated.getStatus() == DocumentStatusEnum.NEEDS_REVISION
                                        || updated.getStatus() == DocumentStatusEnum.REJECTED) {
                                try {
                                        Locale locale = isFrench(clientLanguage) ? Locale.FRENCH : Locale.ENGLISH;
                                        String localizedDocType = messageSource.getMessage(
                                                        "document.type." + updated.getDocType(), null,
                                                        updated.getDocType().name(), locale);
                                        String displayDocName = updated.getCustomTitle() != null
                                                        ? updated.getCustomTitle()
                                                        : localizedDocType;

                                        String titleKey;
                                        String messageKey;
                                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory category;

                                        if (updated.getStatus() == DocumentStatusEnum.APPROVED) {
                                                titleKey = "notification.document.reviewed.title";
                                                messageKey = "notification.document.reviewed.approved";
                                                category = com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_APPROVED;
                                        } else if (updated.getStatus() == DocumentStatusEnum.NEEDS_REVISION) {
                                                titleKey = "notification.document.reviewed.title";
                                                messageKey = "notification.document.reviewed.needs_revision";
                                                category = com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REVISION;
                                        } else { // REJECTED
                                                titleKey = "notification.document.rejected.title";
                                                messageKey = "notification.document.rejected.message";
                                                category = com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REJECTED;
                                        }

                                        String title = messageSource.getMessage(titleKey,
                                                        new Object[] { displayDocName }, locale);
                                        String message;

                                        if (updated.getStatus() == DocumentStatusEnum.REJECTED) {
                                                message = messageSource.getMessage(messageKey,
                                                                new Object[] { displayDocName, brokerName }, locale);
                                        } else {
                                                message = messageSource.getMessage(messageKey,
                                                                new Object[] { displayDocName }, locale);

                                        }

                                        notificationService.createNotification(
                                                        client.getId().toString(),
                                                        title,
                                                        message,
                                                        transactionId.toString(),
                                                        category);
                                } catch (Exception e) {
                                        logger.error("Failed to send in-app notification for document review", e);
                                }
                        }
                } else {
                        logger.warn("Cannot send document review notification: client or broker account could not be resolved for transaction {}",
                                        updated.getTransactionRef().getTransactionId());
                }

                return mapToResponseDTO(updated);
        }

        private boolean isFrench(String lang) {
                return lang != null && lang.equalsIgnoreCase("fr");
        }
}
