package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.enums.*;

import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentVersion;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.datalayer.valueobjects.UploadedBy;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentVersionResponseDTO;
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
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
        private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);

        private final DocumentRepository repository;
        private final S3StorageService storageService;
        private final EmailService emailService;
        private final NotificationService notificationService;
        private final TransactionRepository transactionRepository;
        private final UserAccountRepository userAccountRepository;
        private final TimelineService timelineService;
        private final MessageSource messageSource;
        private final DocumentConditionLinkRepository documentConditionLinkRepository;
        private final TransactionParticipantRepository participantRepository;

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
        public List<DocumentResponseDTO> getDocumentsForTransaction(UUID transactionId, UUID userId) {
                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                verifyViewAccess(tx, userId);

                // Check if user is a client (not the broker or a participant with broker access)
                boolean isClient = tx.getClientId().equals(userId);

                return repository.findByTransactionRef_TransactionId(transactionId).stream()
                                // Filter out DRAFT documents for clients since they should not see drafts
                                .filter(doc -> !isClient || doc.getStatus() != DocumentStatusEnum.DRAFT)
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public List<DocumentResponseDTO> getAllDocumentsForUser(UUID userId) {
                return repository.findByUserId(userId).stream()
                                .filter(doc -> {
                                        // If user is client, filter out DRAFT documents
                                        Transaction tx = transactionRepository
                                                        .findByTransactionId(doc.getTransactionRef().getTransactionId())
                                                        .orElse(null);
                                        if (tx != null && tx.getClientId().equals(userId)) {
                                                return doc.getStatus() != DocumentStatusEnum.DRAFT;
                                        }
                                        return true;
                                })
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public DocumentResponseDTO getDocument(UUID documentId, UUID userId) {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                Transaction tx = transactionRepository
                                .findByTransactionId(document.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyViewAccess(tx, userId);

                return mapToResponseDTO(document);
        }

        @Transactional
        @Override
        public DocumentResponseDTO createDocument(UUID transactionId,
                        DocumentRequestDTO requestDTO, UUID userId) {
                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                verifyBrokerOrCoManager(tx, userId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                // Determine document status: use provided status if DRAFT or REQUESTED, else default to REQUESTED
                DocumentStatusEnum status = DocumentStatusEnum.REQUESTED;
                if (requestDTO.getStatus() != null) {
                        if (requestDTO.getStatus() == DocumentStatusEnum.DRAFT 
                                        || requestDTO.getStatus() == DocumentStatusEnum.REQUESTED) {
                                status = requestDTO.getStatus();
                        }
                        // Other statuses are ignored, default to REQUESTED
                }

                Document document = new Document();
                document.setDocumentId(UUID.randomUUID());
                document.setTransactionRef(new TransactionRef(transactionId, tx.getClientId(), tx.getSide()));
                document.setDocType(requestDTO.getDocType());
                document.setCustomTitle(requestDTO.getCustomTitle());
                document.setStatus(status);
                document.setExpectedFrom(requestDTO.getExpectedFrom());
                document.setVisibleToClient(
                                requestDTO.getVisibleToClient() != null ? requestDTO.getVisibleToClient() : true);
                document.setBrokerNotes(requestDTO.getBrokerNotes());
                document.setLastUpdatedAt(LocalDateTime.now());
                document.setCreatedAt(LocalDateTime.now());
                // Add stage mapping
                document.setStage(requestDTO.getStage());
                document.setDueDate(requestDTO.getDueDate());
                // Set flow type, default to REQUEST if not provided
                document.setFlow(requestDTO.getFlow() != null ? requestDTO.getFlow() : DocumentFlowEnum.REQUEST);

                Document savedDocument = repository.save(document);

                // Save condition links if provided
                if (requestDTO.getConditionIds() != null && !requestDTO.getConditionIds().isEmpty()) {
                        for (UUID conditionId : requestDTO.getConditionIds()) {
                                DocumentConditionLink link = DocumentConditionLink.builder()
                                                .conditionId(conditionId)
                                                .documentId(savedDocument.getDocumentId())
                                                .build();
                                documentConditionLinkRepository.save(link);
                        }
                }

                // Only send notifications and add timeline entry if not a draft
                if (status != DocumentStatusEnum.DRAFT) {
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
                }

                return mapToResponseDTO(savedDocument);
        }

        @Transactional
        @Override
        public DocumentResponseDTO updateDocument(UUID documentId, DocumentRequestDTO requestDTO,
                        UUID userId) {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                Transaction tx = transactionRepository
                                .findByTransactionId(document.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, userId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);
                // Helper: normalize string (null, empty, whitespace)
                java.util.function.Function<String, String> normStr = s -> s == null ? "" : s.trim();
                // Helper: normalize enum or string (case-insensitive string compare)
                java.util.function.Function<Object, String> normEnum = o -> o == null ? ""
                                : o.toString().trim().toLowerCase();

                // Build a candidate entity with merged values (normalized)
                // PREVENT ACCIDENTAL CHANGES: For non-draft documents, docType and expectedFrom are immutable via update
                boolean isDraft = document.getStatus() == DocumentStatusEnum.DRAFT;

                DocumentTypeEnum candidateDocType = requestDTO.getDocType() != null ? requestDTO.getDocType()
                                : document.getDocType();
                if (!isDraft) candidateDocType = document.getDocType(); // Immutable if not draft

                String candidateCustomTitle = normStr.apply(requestDTO.getCustomTitle()).isEmpty() ? null
                                : requestDTO.getCustomTitle();
                if (candidateCustomTitle == null && document.getCustomTitle() != null)
                        candidateCustomTitle = document.getCustomTitle();
                
                DocumentPartyEnum candidateExpectedFrom = requestDTO.getExpectedFrom() != null
                                ? requestDTO.getExpectedFrom()
                                : document.getExpectedFrom();
                if (!isDraft) candidateExpectedFrom = document.getExpectedFrom(); // Immutable if not draft

                boolean candidateVisibleToClient = requestDTO.getVisibleToClient() != null
                                ? requestDTO.getVisibleToClient()
                                : document.isVisibleToClient();
                String candidateBrokerNotes = normStr.apply(requestDTO.getBrokerNotes()).isEmpty() ? null
                                : requestDTO.getBrokerNotes();
                if (candidateBrokerNotes == null && document.getBrokerNotes() != null)
                        candidateBrokerNotes = document.getBrokerNotes();
                StageEnum candidateStage = requestDTO.getStage() != null ? requestDTO.getStage() : document.getStage();

                // Compare normalized candidate to normalized current entity
                boolean isIdentical = normEnum.apply(document.getDocType()).equals(normEnum.apply(candidateDocType)) &&
                                normStr.apply(document.getCustomTitle()).equals(normStr.apply(candidateCustomTitle)) &&
                                normEnum.apply(document.getExpectedFrom()).equals(normEnum.apply(candidateExpectedFrom))
                                &&
                                document.isVisibleToClient() == candidateVisibleToClient &&
                                normStr.apply(document.getBrokerNotes()).equals(normStr.apply(candidateBrokerNotes)) &&
                                normEnum.apply(document.getStage()).equals(normEnum.apply(candidateStage));
                if (isIdentical) {
                        return mapToResponseDTO(document);
                }
                // ...existing code for updating fields and triggering side effects...

                // Only update fields if the normalized value is different from the current
                // normalized value
                if (!normEnum.apply(document.getDocType()).equals(normEnum.apply(candidateDocType))) {
                        document.setDocType(candidateDocType);
                }
                if (!normStr.apply(document.getCustomTitle()).equals(normStr.apply(candidateCustomTitle))) {
                        // If both are null/empty, set to null for consistency
                        String newVal = normStr.apply(candidateCustomTitle);
                        document.setCustomTitle(newVal.isEmpty() ? null : candidateCustomTitle);
                }
                if (!normEnum.apply(document.getExpectedFrom()).equals(normEnum.apply(candidateExpectedFrom))) {
                        document.setExpectedFrom(candidateExpectedFrom);
                }
                if (requestDTO.getVisibleToClient() != null
                                && document.isVisibleToClient() != requestDTO.getVisibleToClient()) {
                        document.setVisibleToClient(requestDTO.getVisibleToClient());
                }
                if (!normStr.apply(document.getBrokerNotes()).equals(normStr.apply(requestDTO.getBrokerNotes()))) {
                        String newVal = normStr.apply(requestDTO.getBrokerNotes());
                        document.setBrokerNotes(newVal.isEmpty() ? null : requestDTO.getBrokerNotes());
                }
                if (!normEnum.apply(document.getStage()).equals(normEnum.apply(requestDTO.getStage()))) {
                        document.setStage(requestDTO.getStage());
                }
                if (requestDTO.getDueDate() != null && !requestDTO.getDueDate().equals(document.getDueDate())) {
                        document.setDueDate(requestDTO.getDueDate());
                }

                // Only update lastUpdatedAt, save, and send notifications/timeline if something
                // changed
                document.setLastUpdatedAt(LocalDateTime.now());
                Document savedDocument = repository.save(document);
                final UUID timelineTransactionId = savedDocument.getTransactionRef().getTransactionId();
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
                                        "document.type." + savedDocument.getDocType(), null,
                                        savedDocument.getDocType().name(), locale);
                        String documentName = (savedDocument.getCustomTitle() != null
                                        && !savedDocument.getCustomTitle().isEmpty()) ? savedDocument.getCustomTitle()
                                                        : localizedDocType;
                        String brokerName = brokerForTimeline != null
                                        ? brokerForTimeline.getFirstName() + " " + brokerForTimeline.getLastName()
                                        : "";
                        // Send a stable, language-agnostic note key and params for frontend i18n
                        String note = String.format("document.details.updated.note|%s|%s", documentName, brokerName);

                        timelineService.addEntry(
                                        savedDocument.getTransactionRef().getTransactionId(),
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
                                String docType = savedDocument.getDocType().toString();

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
                                                "document.type." + savedDocument.getDocType(), null,
                                                savedDocument.getDocType().name(), locale);
                                String displayDocName = savedDocument.getCustomTitle() != null
                                                ? savedDocument.getCustomTitle()
                                                : localizedDocType;
                                String title = messageSource.getMessage("notification.document.edited.title", null,
                                                locale);
                                String message = messageSource.getMessage("notification.document.edited.message",
                                                new Object[] { brokerName, displayDocName }, locale);
                                notificationService.createNotification(
                                                client.getId().toString(),
                                                title,
                                                message,
                                                savedDocument.getTransactionRef().getTransactionId().toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REQUEST);
                        }
                } catch (Exception e) {
                        logger.warn("Could not add timeline entry or send notification/email for document update",
                                        e);
                }

                savedDocument = repository.save(document);

                // Update condition links if provided
                if (requestDTO.getConditionIds() != null) {
                        documentConditionLinkRepository.deleteByDocumentId(documentId);
                        for (UUID conditionId : requestDTO.getConditionIds()) {
                                DocumentConditionLink link = DocumentConditionLink.builder()
                                                .conditionId(conditionId)
                                                .documentId(savedDocument.getDocumentId())
                                                .build();
                                documentConditionLinkRepository.save(link);
                        }
                }

                return mapToResponseDTO(savedDocument);
        }

        @Transactional
        @Override
        public void deleteDocument(UUID documentId, UUID userId) {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                Transaction tx = transactionRepository
                                .findByTransactionId(document.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, userId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                if (document.getStatus() != DocumentStatusEnum.DRAFT) {
                        throw new BadRequestException("Only draft documents can be deleted");
                }
                
                repository.delete(document);
        }

        @Transactional
        @Override
        public DocumentResponseDTO submitDocument(UUID transactionId, UUID documentId, MultipartFile file,
                        UUID uploaderId, UploadedByRefEnum uploaderType) throws IOException {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                if (!document.getTransactionRef().getTransactionId().equals(transactionId)) {
                        throw new BadRequestException(
                                        "Document does not belong to transaction: " + transactionId);
                }

                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                verifyEditAccess(tx, uploaderId);

                StorageObject storageObject = storageService.uploadFile(file, transactionId, documentId);

                UploadedBy uploadedBy = UploadedBy.builder()
                                .uploaderType(uploaderType)
                                .uploaderId(uploaderId)
                                .party(document.getExpectedFrom())
                                .build();

                DocumentVersion version = DocumentVersion.builder()
                                .versionId(UUID.randomUUID())
                                .uploadedAt(LocalDateTime.now())
                                .uploadedBy(uploadedBy)
                                .storageObject(storageObject)
                                .document(document)
                                .build();

                document.getVersions().add(version);
                document.setStatus(DocumentStatusEnum.SUBMITTED);
                document.setLastUpdatedAt(LocalDateTime.now());

                Document savedDocument = repository.save(document);
                // Add timeline entry for document submitted
                timelineService.addEntry(
                                transactionId,
                                uploaderId,
                                TimelineEntryType.DOCUMENT_SUBMITTED,
                                "Document submitted: "
                                                + (savedDocument.getCustomTitle() != null ? savedDocument.getCustomTitle()
                                                                : savedDocument.getDocType()),
                                savedDocument.getDocType() != null ? savedDocument.getDocType().toString() : null);

                // Notification logic
                if (uploaderType == UploadedByRefEnum.BROKER) {
                        // Broker uploaded for Client -> Notify Client
                        UserAccount client = resolveUserAccount(tx.getClientId())
                                        .orElseThrow(() -> new NotFoundException("Client not found: " + tx.getClientId()));
                        UserAccount broker = resolveUserAccount(tx.getBrokerId())
                                        .orElseThrow(() -> new NotFoundException("Broker not found: " + tx.getBrokerId()));

                        String clientName = client.getFirstName() + " " + client.getLastName();
                        String brokerName = broker.getFirstName() + " " + broker.getLastName();
                        String documentName = savedDocument.getCustomTitle() != null ? savedDocument.getCustomTitle()
                                        : savedDocument.getDocType().toString();
                        String docType = savedDocument.getDocType().toString();
                        String clientLanguage = client.getPreferredLanguage() != null ? client.getPreferredLanguage()
                                        : "en";

                        // Send Email to Client
                        emailService.sendDocumentSubmittedNotification(savedDocument, client.getEmail(), clientName,
                                        documentName, docType, clientLanguage);

                        // In-app Notification for Client
                        try {
                                Locale locale = isFrench(clientLanguage) ? Locale.FRENCH : Locale.ENGLISH;
                                String localizedDocType = messageSource.getMessage(
                                                "document.type." + savedDocument.getDocType(), null,
                                                savedDocument.getDocType().name(), locale);
                                String displayDocName = savedDocument.getCustomTitle() != null
                                                ? savedDocument.getCustomTitle()
                                                : localizedDocType;

                                String title = messageSource.getMessage("notification.document.submitted.title", null,
                                                locale);
                                // For client notification: "{Broker Name} submitted document: {Doc Name}"
                                String message = messageSource.getMessage("notification.document.submitted.message",
                                                new Object[] { brokerName, displayDocName }, locale);

                                notificationService.createNotification(
                                                client.getId().toString(),
                                                title,
                                                message,
                                                transactionId.toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_SUBMITTED);
                        } catch (Exception e) {
                                logger.error("Failed to send in-app notification for document submission to client", e);
                        }

                } else {
                        // Client (or other) uploaded -> Notify Broker
                        UserAccount broker = resolveUserAccount(tx.getBrokerId())
                                        .orElseThrow(() -> new NotFoundException("Broker not found: " + tx.getBrokerId()));

                        // Get uploader name
                        String uploaderName = "Unknown Client";
                        if (uploaderType == UploadedByRefEnum.CLIENT) {
                                uploaderName = resolveUserAccount(uploaderId)
                                                .map(u -> u.getFirstName() + " " + u.getLastName())
                                                .orElse("Unknown Client");
                        }

                        String documentName = savedDocument.getCustomTitle() != null ? savedDocument.getCustomTitle()
                                        : savedDocument.getDocType().toString();
                        String docType = savedDocument.getDocType().toString();
                        String brokerLanguage = broker.getPreferredLanguage() != null ? broker.getPreferredLanguage()
                                        : "en";

                        emailService.sendDocumentSubmittedNotification(savedDocument, broker.getEmail(), uploaderName,
                                        documentName, docType, brokerLanguage);

                        // In-app Notification for Broker
                        try {
                                Locale locale = isFrench(brokerLanguage) ? Locale.FRENCH : Locale.ENGLISH;
                                String localizedDocType = messageSource.getMessage(
                                                "document.type." + savedDocument.getDocType(), null,
                                                savedDocument.getDocType().name(), locale);
                                String displayDocName = savedDocument.getCustomTitle() != null
                                                ? savedDocument.getCustomTitle()
                                                : localizedDocType;

                                String title = messageSource.getMessage("notification.document.submitted.title", null,
                                                locale);
                                // For broker notification: "{Client Name} submitted document: {Doc Name}"
                                String message = messageSource.getMessage("notification.document.submitted.message",
                                                new Object[] { uploaderName, displayDocName }, locale);
                                notificationService.createNotification(
                                                broker.getId().toString(),
                                                title,
                                                message,
                                                transactionId.toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_SUBMITTED);
                        } catch (Exception e) {
                                logger.error("Failed to send in-app notification for document submission to broker", e);
                        }
                }

                return mapToResponseDTO(savedDocument);
        }

        @Transactional
        @Override
        public DocumentResponseDTO uploadFileToDocument(UUID transactionId, UUID documentId, MultipartFile file,
                        UUID uploaderId, UploadedByRefEnum uploaderType) throws IOException {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                if (!document.getTransactionRef().getTransactionId().equals(transactionId)) {
                        throw new BadRequestException(
                                        "Document does not belong to transaction: " + transactionId);
                }

                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                verifyEditAccess(tx, uploaderId);

                StorageObject storageObject = storageService.uploadFile(file, transactionId, documentId);

                UploadedBy uploadedBy = UploadedBy.builder()
                                .uploaderType(uploaderType)
                                .uploaderId(uploaderId)
                                .party(document.getExpectedFrom())
                                .build();

                DocumentVersion version = DocumentVersion.builder()
                                .versionId(UUID.randomUUID())
                                .uploadedAt(LocalDateTime.now())
                                .uploadedBy(uploadedBy)
                                .storageObject(storageObject)
                                .document(document)
                                .build();

                document.getVersions().add(version);
                // Note: We do NOT change the status here - the document keeps its current status
                document.setLastUpdatedAt(LocalDateTime.now());

                Document savedDocument = repository.save(document);

                return mapToResponseDTO(savedDocument);
        }

        private DocumentResponseDTO mapToResponseDTO(Document document) {
                return DocumentResponseDTO.builder()
                                .documentId(document.getDocumentId())
                                .transactionRef(document.getTransactionRef())
                                .docType(document.getDocType())
                                .customTitle(document.getCustomTitle())
                                .status(document.getStatus())
                                .expectedFrom(document.getExpectedFrom())
                                .versions(
                                                document.getVersions() != null
                                                                ? document.getVersions().stream()
                                                                                .map(this::mapToVersionDTO)
                                                                                .collect(Collectors.toList())
                                                                : null)
                                .brokerNotes(document.getBrokerNotes())
                                .lastUpdatedAt(document.getLastUpdatedAt())
                                .visibleToClient(document.isVisibleToClient())
                                .stage(document.getStage())
				.flow(document.getFlow())
                                .build();
        }

        private DocumentVersionResponseDTO mapToVersionDTO(DocumentVersion version) {
                return DocumentVersionResponseDTO.builder()
                                .versionId(version.getVersionId())
                                .uploadedAt(version.getUploadedAt())
                                .uploadedBy(version.getUploadedBy())
                                .storageObject(version.getStorageObject())
                                .build();
        }

        @Override
        public String getDocumentDownloadUrl(UUID documentId, UUID versionId, UUID userId) {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                Transaction tx = transactionRepository
                                .findByTransactionId(document.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyViewAccess(tx, userId);

                DocumentVersion version = document.getVersions().stream()
                                .filter(v -> v.getVersionId().equals(versionId))
                                .findFirst()
                                .orElseThrow(() -> new NotFoundException(
                                                "Document version not found: " + versionId));

                return storageService.generatePresignedUrl(version.getStorageObject().getS3Key());
        }

        @Transactional
        @Override
        public DocumentResponseDTO reviewDocument(UUID transactionId, UUID documentId,
                        DocumentReviewRequestDTO reviewDTO, UUID brokerId) {
                // Verify broker access is done below after fetching transaction
                // or we can fetch tx first.

                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                if (!document.getTransactionRef().getTransactionId().equals(transactionId)) {
                        throw new BadRequestException("Document does not belong to this transaction");
                }

                Transaction tx = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                verifyBrokerOrCoManager(tx, brokerId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                if (document.getStatus() != DocumentStatusEnum.SUBMITTED) {
                        throw new BadRequestException("Only submitted documents can be reviewed");
                }

                document.setStatus(reviewDTO.getDecision());
                document.setBrokerNotes(reviewDTO.getComments());
                document.setLastUpdatedAt(LocalDateTime.now());

                Document updated = repository.save(document);
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
                // Transaction tx is already fetched above

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
                // Pass current time to filter for overdue documents
                List<Document> documents = repository.findOutstandingDocumentsForBroker(brokerId,
                                LocalDateTime.now());

                return documents.stream().map(doc -> {
                        Transaction tx = transactionRepository
                                        .findByTransactionId(doc.getTransactionRef().getTransactionId()).orElse(null);
                        if (tx == null)
                                return null;

                        UserAccount client = resolveUserAccount(tx.getClientId()).orElse(null);
                        String clientName = client != null ? client.getFirstName() + " " + client.getLastName()
                                        : "Unknown";
                        String clientEmail = client != null ? client.getEmail() : null;

                        String address = "No Address";
                        if (tx.getPropertyAddress() != null) {
                                address = java.util.stream.Stream.of(
                                                tx.getPropertyAddress().getStreet(),
                                                tx.getPropertyAddress().getCity(),
                                                tx.getPropertyAddress().getProvince(),
                                                tx.getPropertyAddress().getPostalCode())
                                                .filter(s -> s != null && !s.trim().isEmpty())
                                                .collect(Collectors.joining(", "));
                        }

                        Integer daysOutstanding = 0;
                        if (doc.getDueDate() != null) {
                                // Calculate days PAST due date
                                daysOutstanding = (int) java.time.temporal.ChronoUnit.DAYS.between(doc.getDueDate(),
                                                LocalDateTime.now());
                        } else {
                                // Fallback for legacy/migrated data without due date (though query excludes
                                // them now)
                                LocalDateTime startDate = doc.getCreatedAt() != null ? doc.getCreatedAt()
                                                : doc.getLastUpdatedAt();
                                if (startDate != null) {
                                        daysOutstanding = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate,
                                                        LocalDateTime.now());
                                }
                        }

                        return com.example.courtierprobackend.documents.presentationlayer.models.OutstandingDocumentDTO
                                        .builder()
                                        .id(doc.getDocumentId())
                                        .title(doc.getCustomTitle() != null ? doc.getCustomTitle()
                                                        : doc.getDocType().toString())
                                        .transactionAddress(address)
                                        .clientName(clientName)
                                        .clientEmail(clientEmail)
                                        .dueDate(doc.getDueDate())
                                        .daysOutstanding(daysOutstanding)
                                        .status(doc.getStatus().toString())
                                        .build();
                })
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toList());
        }

        @Override
        public void sendDocumentReminder(UUID documentId, UUID brokerId) {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                Transaction tx = transactionRepository
                                .findByTransactionId(document.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, brokerId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                if (document.getStatus() != DocumentStatusEnum.REQUESTED
                                && document.getStatus() != DocumentStatusEnum.NEEDS_REVISION) {
                        throw new BadRequestException("Can only remind for outstanding documents");
                }

                UserAccount client = resolveUserAccount(tx.getClientId())
                                .orElseThrow(() -> new NotFoundException("Client not found"));
                UserAccount broker = resolveUserAccount(tx.getBrokerId())
                                .orElseThrow(() -> new NotFoundException("Broker not found"));

                String documentName = document.getCustomTitle() != null ? document.getCustomTitle()
                                : document.getDocType().toString();

                emailService.sendDocumentRequestedNotification(
                                client.getEmail(),
                                client.getFirstName() + " " + client.getLastName(),
                                broker.getFirstName() + " " + broker.getLastName(),
                                documentName,
                                document.getDocType().toString(),
                                document.getBrokerNotes(),
                                client.getPreferredLanguage());
        }

        @Transactional
        @Override
        public DocumentResponseDTO sendDocumentRequest(UUID documentId, UUID brokerId) {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                Transaction tx = transactionRepository
                                .findByTransactionId(document.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, brokerId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                if (document.getStatus() != DocumentStatusEnum.DRAFT) {
                        throw new BadRequestException("Only draft documents can be sent as requests");
                }

                // Transition to REQUESTED status
                document.setStatus(DocumentStatusEnum.REQUESTED);
                document.setLastUpdatedAt(LocalDateTime.now());
                Document savedDocument = repository.save(document);

                // Add timeline entry for document requested
                timelineService.addEntry(
                                tx.getTransactionId(),
                                tx.getBrokerId(),
                                TimelineEntryType.DOCUMENT_REQUESTED,
                                "Document requested: " + document.getDocType(),
                                document.getDocType() != null ? document.getDocType().toString() : null);

                // Notify client via email
                UserAccount client = resolveUserAccount(tx.getClientId()).orElse(null);
                UserAccount broker = resolveUserAccount(tx.getBrokerId()).orElse(null);

                if (client != null && broker != null) {
                        String documentName = document.getCustomTitle() != null ? document.getCustomTitle()
                                        : document.getDocType().toString();
                        String clientName = client.getFirstName() + " " + client.getLastName();
                        String brokerName = broker.getFirstName() + " " + broker.getLastName();
                        String docType = document.getDocType().toString();
                        String clientLanguage = client.getPreferredLanguage();

                        emailService.sendDocumentRequestedNotification(
                                        client.getEmail(),
                                        clientName,
                                        brokerName,
                                        documentName,
                                        docType,
                                        document.getBrokerNotes(),
                                        clientLanguage);

                        // In-app Notification for Client
                        try {
                                Locale locale = isFrench(clientLanguage) ? Locale.FRENCH : Locale.ENGLISH;
                                String localizedDocType = messageSource.getMessage(
                                                "document.type." + document.getDocType(), null,
                                                document.getDocType().name(), locale);
                                String displayDocName = document.getCustomTitle() != null
                                                ? document.getCustomTitle()
                                                : localizedDocType;
                                String title = messageSource.getMessage("notification.document.requested.title", null, locale);
                                String message = messageSource.getMessage("notification.document.requested.message",
                                                new Object[] { brokerName, displayDocName }, locale);
                                notificationService.createNotification(
                                                client.getId().toString(),
                                                title,
                                                message,
                                                tx.getTransactionId().toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_REQUEST);
                        } catch (Exception e) {
                                logger.error("Failed to send in-app notification for document request", e);
                        }
                }

                return mapToResponseDTO(savedDocument);
        }

        @Transactional
        @Override
        public DocumentResponseDTO shareDocumentWithClient(UUID documentId, UUID brokerId) {
                Document document = repository.findByDocumentId(documentId)
                                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

                Transaction tx = transactionRepository
                                .findByTransactionId(document.getTransactionRef().getTransactionId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                verifyBrokerOrCoManager(tx, brokerId,
                                com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS);

                if (document.getStatus() != DocumentStatusEnum.DRAFT) {
                        throw new BadRequestException("Only draft documents can be shared with client");
                }

                if (document.getFlow() != DocumentFlowEnum.UPLOAD) {
                        throw new BadRequestException("Only UPLOAD flow documents can be shared using this endpoint");
                }

                // Transition to SUBMITTED status and make visible to client
                document.setStatus(DocumentStatusEnum.SUBMITTED);
                document.setVisibleToClient(true);
                document.setLastUpdatedAt(LocalDateTime.now());
                Document savedDocument = repository.save(document);

                // Add timeline entry for document shared
                timelineService.addEntry(
                                tx.getTransactionId(),
                                tx.getBrokerId(),
                                TimelineEntryType.DOCUMENT_SUBMITTED,
                                "Document shared with client: " + (document.getCustomTitle() != null ? document.getCustomTitle() : document.getDocType()),
                                document.getDocType() != null ? document.getDocType().toString() : null);

                // Notify client via email
                UserAccount client = resolveUserAccount(tx.getClientId()).orElse(null);
                UserAccount broker = resolveUserAccount(tx.getBrokerId()).orElse(null);

                if (client != null && broker != null) {
                        String documentName = document.getCustomTitle() != null ? document.getCustomTitle()
                                        : document.getDocType().toString();
                        String brokerName = broker.getFirstName() + " " + broker.getLastName();
                        String docType = document.getDocType().toString();
                        String clientLanguage = client.getPreferredLanguage();

                        emailService.sendDocumentSubmittedNotification(savedDocument, client.getEmail(), brokerName,
                                        documentName, docType, clientLanguage);

                        // In-app Notification for Client
                        try {
                                Locale locale = isFrench(clientLanguage) ? Locale.FRENCH : Locale.ENGLISH;
                                String localizedDocType = messageSource.getMessage(
                                                "document.type." + document.getDocType(), null,
                                                document.getDocType().name(), locale);
                                String displayDocName = document.getCustomTitle() != null
                                                ? document.getCustomTitle()
                                                : localizedDocType;
                                String title = messageSource.getMessage("notification.document.shared.title", null,
                                                "Document Shared", locale);
                                String message = messageSource.getMessage("notification.document.shared.message",
                                                new Object[] { brokerName, displayDocName },
                                                brokerName + " shared a document: " + displayDocName, locale);
                                notificationService.createNotification(
                                                client.getId().toString(),
                                                title,
                                                message,
                                                tx.getTransactionId().toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.DOCUMENT_SUBMITTED);
                        } catch (Exception e) {
                                logger.error("Failed to send in-app notification for document share", e);
                        }
                }

                return mapToResponseDTO(savedDocument);
        }

        private boolean isFrench(String lang) {
                return lang != null && lang.equalsIgnoreCase("fr");
        }
}
