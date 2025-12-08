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

    public List<DocumentRequestResponseDTO> getDocumentsForTransaction(String transactionId, String userId) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (!tx.getBrokerId().equals(userId) && !tx.getClientId().equals(userId)) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        return repository.findByTransactionRef_TransactionId(transactionId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentRequestResponseDTO> getAllDocumentsForUser(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public DocumentRequestResponseDTO getDocumentRequest(String requestId, String userId) {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        Transaction tx = transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!tx.getBrokerId().equals(userId) && !tx.getClientId().equals(userId)) {
            throw new NotFoundException("You do not have access to this document request");
        }

        return mapToResponseDTO(request);
    }

    @Transactional
    public DocumentRequestResponseDTO createDocumentRequest(String transactionId, DocumentRequestRequestDTO requestDTO) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        DocumentRequest request = new DocumentRequest();
        request.setRequestId(UUID.randomUUID().toString());
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
        UserAccount client = userAccountRepository.findByAuth0UserId(tx.getClientId())
                .orElse(null);
        UserAccount broker = userAccountRepository.findByAuth0UserId(tx.getBrokerId())
                .orElse(null);

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
    public DocumentRequestResponseDTO updateDocumentRequest(String requestId, DocumentRequestRequestDTO requestDTO) {
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
    public void deleteDocumentRequest(String requestId) {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));
        repository.delete(request);
    }

    @Transactional
    public DocumentRequestResponseDTO submitDocument(String transactionId, String requestId, MultipartFile file, String uploaderId, UploadedByRefEnum uploaderType) throws IOException {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        if (!request.getTransactionRef().getTransactionId().equals(transactionId)) {
            throw new InvalidInputException("Document request does not belong to transaction: " + transactionId);
        }

        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (!tx.getBrokerId().equals(uploaderId) && !tx.getClientId().equals(uploaderId)) {
            throw new NotFoundException("You do not have permission to upload documents for this transaction");
        }

        StorageObject storageObject = storageService.uploadFile(file, transactionId, requestId);

        UploadedBy uploadedBy = UploadedBy.builder()
                .uploaderType(uploaderType)
                .uploaderId(uploaderId)
                .party(request.getExpectedFrom()) // Assuming uploader matches expected party for now
                .build();

        SubmittedDocument submission = SubmittedDocument.builder()
                .documentId(UUID.randomUUID().toString())
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
        // Transaction tx is already fetched above
        
        UserAccount broker = userAccountRepository.findByAuth0UserId(tx.getBrokerId())
                .orElseThrow(() -> new NotFoundException("Broker not found: " + tx.getBrokerId()));

        // Get uploader name (Client)
        String uploaderName = "Unknown Client";
        if (uploaderType == UploadedByRefEnum.CLIENT) {
             uploaderName = userAccountRepository.findByAuth0UserId(uploaderId)
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
    public String getDocumentDownloadUrl(String requestId, String documentId, String userId) {
        DocumentRequest request = repository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Document request not found: " + requestId));

        Transaction tx = transactionRepository.findByTransactionId(request.getTransactionRef().getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!tx.getBrokerId().equals(userId) && !tx.getClientId().equals(userId)) {
            throw new NotFoundException("You do not have access to this document");
        }

        SubmittedDocument submittedDocument = request.getSubmittedDocuments().stream()
                .filter(doc -> doc.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Submitted document not found: " + documentId));

        return storageService.generatePresignedUrl(submittedDocument.getStorageObject().getS3Key());
    }
}
