package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.SubmittedDocument;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.datalayer.valueobjects.UploadedBy;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.SubmittedDocumentResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentRequestServiceImpl implements DocumentRequestService {

    private final DocumentRequestRepository repository;
    private final S3StorageService storageService;
    private final EmailService emailService;
    private final TransactionRepository transactionRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Helper to find a UserAccount by internal UUID.
     * Now uses UUID directly since database enforces UUID type.
     */
    private Optional<UserAccount> resolveUserAccount(UUID id) {
        if (id == null) return Optional.empty();
        return userAccountRepository.findById(id);
    }

    /**
     * Checks if the given userId (Internal UUID) has access to the transaction.
     */
    private boolean hasAccess(Transaction tx, UUID userId) {
        if (userId == null) return false;
        
        try {
            // Compare with broker and client UUIDs directly
            boolean isBroker = tx.getBrokerId() != null && tx.getBrokerId().equals(userId);
            boolean isClient = tx.getClientId() != null && tx.getClientId().equals(userId);
            
            return isBroker || isClient;
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            return false;
        }
    }

    @Override
    public List<DocumentRequestResponseDTO> getDocumentsForTransaction(UUID transactionId, UUID userId) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (!hasAccess(tx, userId)) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        return repository.findByTransactionRef_TransactionId(transactionId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentRequestResponseDTO> getAllDocumentsForUser(UUID userId) {
        // This query relies on the DB storing the exact ID. 
        // If we want to be fully robust, we might need to query by both Auth0 and UUID if possible,
        // but for now, we assume the repository query handles what's in the DB.
        // Given the requirement is mostly about Transaction access, this one might stay as is 
        // OR we resolve the user first.
        return repository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentRequestResponseDTO getDocumentRequest(UUID requestId, UUID userId) {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        Transaction tx = transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!hasAccess(tx, userId)) {
            throw new NotFoundException("You do not have access to this document request");
        }

        return mapToResponseDTO(request);
    }

    @Transactional
    public DocumentRequestResponseDTO createDocumentRequest(UUID transactionId, DocumentRequestRequestDTO requestDTO) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        DocumentRequest request = new DocumentRequest();
        request.setRequestId(UUID.randomUUID());
        request.setTransactionRef(new TransactionRef(transactionId, tx.getClientId(), tx.getSide()));
        request.setDocType(requestDTO.getDocType());
        request.setCustomTitle(requestDTO.getCustomTitle());
        request.setStatus(DocumentStatusEnum.REQUESTED);
        request.setExpectedFrom(requestDTO.getExpectedFrom());
        request.setVisibleToClient(requestDTO.getVisibleToClient() != null ? requestDTO.getVisibleToClient() : true);
        request.setBrokerNotes(requestDTO.getBrokerNotes());
        request.setLastUpdatedAt(LocalDateTime.now());

        DocumentRequest savedRequest = repository.save(request);

        // Notify client via email
        // Resolve Client and Broker robustly
        UserAccount client = resolveUserAccount(tx.getClientId()).orElse(null);
        UserAccount broker = resolveUserAccount(tx.getBrokerId()).orElse(null);

        if (client != null && broker != null) {
            String documentName = requestDTO.getCustomTitle() != null ? 
                    requestDTO.getCustomTitle() : requestDTO.getDocType().toString();
            String clientName = client.getFirstName() + " " + client.getLastName();
            String brokerName = broker.getFirstName() + " " + broker.getLastName();
            
            emailService.sendDocumentRequestedNotification(
                    client.getEmail(), 
                    clientName, 
                    brokerName, 
                    documentName
            );
        }

        return mapToResponseDTO(savedRequest);
    }

    @Transactional
    public DocumentRequestResponseDTO updateDocumentRequest(UUID requestId, DocumentRequestRequestDTO requestDTO) {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        if (requestDTO.getDocType() != null) request.setDocType(requestDTO.getDocType());
        if (requestDTO.getCustomTitle() != null) request.setCustomTitle(requestDTO.getCustomTitle());
        if (requestDTO.getExpectedFrom() != null) request.setExpectedFrom(requestDTO.getExpectedFrom());
        if (requestDTO.getVisibleToClient() != null) request.setVisibleToClient(requestDTO.getVisibleToClient());
        if (requestDTO.getBrokerNotes() != null) request.setBrokerNotes(requestDTO.getBrokerNotes());
        
        request.setLastUpdatedAt(LocalDateTime.now());

        return mapToResponseDTO(repository.save(request));
    }

    @Transactional
    public void deleteDocumentRequest(UUID requestId) {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));
        repository.delete(request);
    }

    @Transactional
    public DocumentRequestResponseDTO submitDocument(UUID transactionId, UUID requestId, MultipartFile file, UUID uploaderId, UploadedByRefEnum uploaderType) throws IOException {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        if (!request.getTransactionRef().getTransactionId().equals(transactionId)) {
            throw new InvalidInputException("Document request does not belong to transaction: " + transactionId);
        }

        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (!hasAccess(tx, uploaderId)) {
            throw new NotFoundException("You do not have permission to upload documents for this transaction");
        }

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

        emailService.sendDocumentSubmittedNotification(savedRequest, broker.getEmail(), uploaderName, savedRequest.getCustomTitle() != null ? savedRequest.getCustomTitle() : savedRequest.getDocType().toString()); 

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
                .submittedDocuments(request.getSubmittedDocuments().stream()
                        .map(this::mapToSubmittedDocumentDTO)
                        .collect(Collectors.toList()))
                .brokerNotes(request.getBrokerNotes())
                .lastUpdatedAt(request.getLastUpdatedAt())
                .visibleToClient(request.isVisibleToClient())
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

        Transaction tx = transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!hasAccess(tx, userId)) {
            throw new NotFoundException("You do not have access to this document");
        }

        SubmittedDocument submittedDocument = request.getSubmittedDocuments().stream()
                .filter(doc -> doc.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Submitted document not found: " + documentId));

        return storageService.generatePresignedUrl(submittedDocument.getStorageObject().getS3Key());
    }
}
