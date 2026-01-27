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
import com.example.courtierprobackend.transactions.util.TransactionAccessUtils;
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
        private final com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository participantRepository;

        private void verifyViewAccess(Transaction tx, UUID userId) {
                String userEmail = userAccountRepository.findById(userId)
                                .map(UserAccount::getEmail)
                                .orElse(null);
                java.util.List<com.example.courtierprobackend.transactions.datalayer.TransactionParticipant> participants = participantRepository
                                .findByTransactionId(tx.getTransactionId());
                TransactionAccessUtils.verifyViewAccess(tx, userId, userEmail, participants,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.VIEW_DOCUMENTS);
        }

        private void verifyEditAccess(Transaction tx, UUID userId) {
                // For submitDocument (Client or Broker or Co-Broker with EDIT)
                if (tx.getClientId().equals(userId))
                        return;

                verifyBrokerOrCoManager(tx, userId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);
        }

        private void verifyBrokerOrCoManager(Transaction tx, UUID userId,
                        com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission requiredPermission) {
                String userEmail = userAccountRepository.findById(userId)
                                .map(UserAccount::getEmail)
                                .orElse(null);
                java.util.List<com.example.courtierprobackend.transactions.datalayer.TransactionParticipant> participants = participantRepository
                                .findByTransactionId(tx.getTransactionId());
                TransactionAccessUtils.verifyBrokerOrCoManagerAccess(tx, userId, userEmail, participants,
                                requiredPermission);
        }

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

                verifyViewAccess(tx, userId);

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

                verifyViewAccess(tx, userId);

                return mapToResponseDTO(request);
        }

        @Transactional
        @Override
        public DocumentRequestResponseDTO createDocumentRequest(UUID transactionId,
                        DocumentRequestRequestDTO requestDTO, UUID userId) {
                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                verifyBrokerOrCoManager(tx, userId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                // Verify broker or co-manager access
                // Since creating a request is managing documents, we require EDIT_DOCUMENTS
                // But wait, the controller doesn't pass userId to createDocumentRequest!
                // The controller uses @RequestBody RequestDTO.
                // WE FOUND A BUG IN CONTROLLER TOO?
                // DocumentRequestController line 49: createDocumentRequest(@PathVariable txId,
                // @RequestBody dto)
                // It does NOT resolve userId!
                // I need to fix Controller first to pass userId!
                // (Fixed in previous step)

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
                                        requestDTO.getBrokerNotes(),
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
        public DocumentRequestResponseDTO updateDocumentRequest(UUID requestId, DocumentRequestRequestDTO requestDTO,
                        UUID userId) {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                Transaction tx = transactionRepository
                                .findByTransactionId(request.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, userId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);
                // Helper: normalize string (null, empty, whitespace)
                java.util.function.Function<String, String> normStr = s -> s == null ? "" : s.trim();
                // Helper: normalize enum or string (case-insensitive string compare)
                java.util.function.Function<Object, String> normEnum = o -> o == null ? ""
                                : o.toString().trim().toLowerCase();

                // Build a candidate entity with merged values (normalized)
                DocumentTypeEnum candidateDocType = requestDTO.getDocType() != null ? requestDTO.getDocType()
                                : request.getDocType();
                String candidateCustomTitle = normStr.apply(requestDTO.getCustomTitle()).isEmpty() ? null
                                : requestDTO.getCustomTitle();
                if (candidateCustomTitle == null && request.getCustomTitle() != null)
                        candidateCustomTitle = request.getCustomTitle();
                DocumentPartyEnum candidateExpectedFrom = requestDTO.getExpectedFrom() != null
                                ? requestDTO.getExpectedFrom()
                                : request.getExpectedFrom();
                Boolean candidateVisibleToClient = requestDTO.getVisibleToClient() != null
                                ? requestDTO.getVisibleToClient()
                                : request.isVisibleToClient();
                String candidateBrokerNotes = normStr.apply(requestDTO.getBrokerNotes()).isEmpty() ? null
                                : requestDTO.getBrokerNotes();
                if (candidateBrokerNotes == null && request.getBrokerNotes() != null)
                        candidateBrokerNotes = request.getBrokerNotes();
                StageEnum candidateStage = requestDTO.getStage() != null ? requestDTO.getStage() : request.getStage();

                // Compare normalized candidate to normalized current entity
                boolean isIdentical = normEnum.apply(request.getDocType()).equals(normEnum.apply(candidateDocType)) &&
                                normStr.apply(request.getCustomTitle()).equals(normStr.apply(candidateCustomTitle)) &&
                                normEnum.apply(request.getExpectedFrom()).equals(normEnum.apply(candidateExpectedFrom))
                                &&
                                request.isVisibleToClient() == candidateVisibleToClient &&
                                normStr.apply(request.getBrokerNotes()).equals(normStr.apply(candidateBrokerNotes)) &&
                                normEnum.apply(request.getStage()).equals(normEnum.apply(candidateStage));
                if (isIdentical) {
                        return mapToResponseDTO(request);
                }
                // ...existing code for updating fields and triggering side effects...

                // Only update fields if the normalized value is different from the current
                // normalized value
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
                if (requestDTO.getVisibleToClient() != null
                                && request.isVisibleToClient() != requestDTO.getVisibleToClient()) {
                        request.setVisibleToClient(requestDTO.getVisibleToClient());
                }
                if (!normStr.apply(request.getBrokerNotes()).equals(normStr.apply(requestDTO.getBrokerNotes()))) {
                        String newVal = normStr.apply(requestDTO.getBrokerNotes());
                        request.setBrokerNotes(newVal.isEmpty() ? null : requestDTO.getBrokerNotes());
                }
                if (!normEnum.apply(request.getStage()).equals(normEnum.apply(requestDTO.getStage()))) {
                        request.setStage(requestDTO.getStage());
                }

                // Only update lastUpdatedAt, save, and send notifications/timeline if something
                // changed
                request.setLastUpdatedAt(LocalDateTime.now());
                DocumentRequest savedRequest = repository.save(request);
                final UUID timelineTransactionId = savedRequest.getTransactionRef().getTransactionId();
                try {
                        Transaction txForTimeline = transactionRepository.findByTransactionId(timelineTransactionId)
                                        .orElseThrow(() -> new NotFoundException(
                                                        "Transaction not found for timeline entry: "
                                                                        + timelineTransactionId));

                        // Get client language for localization
                        UserAccount clientForTimeline = resolveUserAccount(txForTimeline.getClientId()).orElse(null);
                        UserAccount brokerForTimeline = resolveUserAccount(txForTimeline.getBrokerId()).orElse(null);
                        String clientLanguage = clientForTimeline != null ? clientForTimeline.getPreferredLanguage()
                                        : null;

                        // Prepare document name and actor for note
                        // Ensure locale is defined before use
                        clientLanguage = clientForTimeline != null ? clientForTimeline.getPreferredLanguage() : null;
                        Locale locale = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr") ? Locale.FRENCH
                                        : Locale.ENGLISH;
                        String localizedDocType = messageSource.getMessage(
                                        "document.type." + savedRequest.getDocType(), null,
                                        savedRequest.getDocType().name(), locale);
                        String documentName = (savedRequest.getCustomTitle() != null
                                        && !savedRequest.getCustomTitle().isEmpty()) ? savedRequest.getCustomTitle()
                                                        : localizedDocType;
                        String brokerName = brokerForTimeline != null
                                        ? brokerForTimeline.getFirstName() + " " + brokerForTimeline.getLastName()
                                        : "";
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
                                                clientLanguage);

                                // In-app notification (distinct message for edit)
                                localizedDocType = messageSource.getMessage(
                                                "document.type." + savedRequest.getDocType(), null,
                                                savedRequest.getDocType().name(), locale);
                                String displayDocName = savedRequest.getCustomTitle() != null
                                                ? savedRequest.getCustomTitle()
                                                : localizedDocType;
                                String title = messageSource.getMessage("notification.document.edited.title", null,
                                                locale);
                                String message = messageSource.getMessage("notification.document.edited.message",
                                                new Object[] { brokerName, displayDocName }, locale);
                                notificationService.createNotification(
                                                client.getId().toString(),
                                                title,
                                                message,
                                                savedRequest.getTransactionRef().getTransactionId().toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REQUEST);
                        }
                } catch (Exception e) {
                        logger.warn("Could not add timeline entry or send notification/email for document request update",
                                        e);
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
        public void deleteDocumentRequest(UUID requestId, UUID userId) {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                Transaction tx = transactionRepository
                                .findByTransactionId(request.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, userId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);
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

                verifyEditAccess(tx, uploaderId);

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
                                .submittedDocuments(
                                                request.getSubmittedDocuments() != null
                                                                ? request.getSubmittedDocuments().stream()
                                                                                .map(this::mapToSubmittedDocumentDTO)
                                                                                .collect(Collectors.toList())
                                                                : null)
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

                verifyViewAccess(tx, userId);

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
                // Verify broker access is done below after fetching transaction
                // or we can fetch tx first.

                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                if (!request.getTransactionRef().getTransactionId().equals(transactionId)) {
                        throw new BadRequestException("Document does not belong to this transaction");
                }

                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                verifyBrokerOrCoManager(tx, brokerId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

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
                // Send email notification
                // Transaction tx is already fetched above at line 567

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
                                        client.getFirstName() + " " + client.getLastName(),
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

        @Override
        public java.util.List<com.example.courtierprobackend.documents.presentationlayer.models.OutstandingDocumentDTO> getOutstandingDocumentSummary(
                        UUID brokerId) {
                List<DocumentRequest> requests = repository.findOutstandingDocumentsForBroker(brokerId);

                return requests.stream().map(req -> {
                        Transaction tx = transactionRepository
                                        .findByTransactionId(req.getTransactionRef().getTransactionId()).orElse(null);
                        if (tx == null)
                                return null;

                        UserAccount client = resolveUserAccount(tx.getClientId()).orElse(null);
                        String clientName = client != null ? client.getFirstName() + " " + client.getLastName()
                                        : "Unknown";
                        String clientEmail = client != null ? client.getEmail() : null;

                        String address = tx.getPropertyAddress() != null ? String.format("%s, %s, %s, %s",
                                        tx.getPropertyAddress().getStreet(),
                                        tx.getPropertyAddress().getCity(),
                                        tx.getPropertyAddress().getProvince(),
                                        tx.getPropertyAddress().getPostalCode())
                                        : "No Address";

                        Integer daysOutstanding = null;
                        if (req.getCreatedAt() != null) {
                                daysOutstanding = (int) java.time.temporal.ChronoUnit.DAYS.between(req.getCreatedAt(),
                                                LocalDateTime.now());
                        }

                        return com.example.courtierprobackend.documents.presentationlayer.models.OutstandingDocumentDTO
                                        .builder()
                                        .id(req.getRequestId())
                                        .title(req.getCustomTitle() != null ? req.getCustomTitle()
                                                        : req.getDocType().toString())
                                        .transactionAddress(address)
                                        .clientName(clientName)
                                        .clientEmail(clientEmail)
                                        .dueDate(req.getDueDate())
                                        .daysOutstanding(daysOutstanding)
                                        .status(req.getStatus().toString())
                                        .build();
                })
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toList());
        }

        @Override
        public void sendDocumentReminder(UUID requestId, UUID brokerId) {
                DocumentRequest request = repository.findByRequestId(requestId)
                                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

                Transaction tx = transactionRepository
                                .findByTransactionId(request.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, brokerId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                if (request.getStatus() != DocumentStatusEnum.REQUESTED
                                && request.getStatus() != DocumentStatusEnum.NEEDS_REVISION) {
                        throw new BadRequestException("Can only remind for outstanding documents");
                }

                UserAccount client = resolveUserAccount(tx.getClientId())
                                .orElseThrow(() -> new NotFoundException("Client not found"));
                UserAccount broker = resolveUserAccount(tx.getBrokerId())
                                .orElseThrow(() -> new NotFoundException("Broker not found"));

                String documentName = request.getCustomTitle() != null ? request.getCustomTitle()
                                : request.getDocType().toString();

                emailService.sendDocumentRequestedNotification(
                                client.getEmail(),
                                client.getFirstName() + " " + client.getLastName(),
                                broker.getFirstName() + " " + broker.getLastName(),
                                documentName,
                                request.getDocType().toString(),
                                request.getBrokerNotes(),
                                client.getPreferredLanguage());
        }

        private boolean isFrench(String lang) {
                return lang != null && lang.equalsIgnoreCase("fr");
        }
}
