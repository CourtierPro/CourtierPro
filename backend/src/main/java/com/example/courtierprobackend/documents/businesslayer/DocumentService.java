package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface DocumentService {

        List<DocumentResponseDTO> getDocumentsForTransaction(UUID transactionId, UUID userId);

        DocumentResponseDTO getDocument(UUID documentId, UUID userId);

        DocumentResponseDTO createDocument(UUID transactionId, DocumentRequestDTO requestDTO,
                        UUID userId);

        DocumentResponseDTO updateDocument(UUID documentId, DocumentRequestDTO requestDTO,
                        UUID userId);

        void deleteDocument(UUID documentId, UUID userId);

        DocumentResponseDTO submitDocument(UUID transactionId, UUID documentId, MultipartFile file,
                        UUID uploaderId,
                        UploadedByRefEnum uploaderType) throws IOException;

        List<DocumentResponseDTO> getAllDocumentsForUser(UUID userId);

        String getDocumentDownloadUrl(UUID documentId, UUID versionId, UUID userId);

        DocumentResponseDTO reviewDocument(UUID transactionId, UUID documentId,
                        DocumentReviewRequestDTO reviewDTO,
                        UUID brokerId);

        java.util.List<com.example.courtierprobackend.documents.presentationlayer.models.OutstandingDocumentDTO> getOutstandingDocumentSummary(
                        UUID brokerId);

        void sendDocumentReminder(UUID documentId, UUID brokerId);

        /**
         * Transitions a document from DRAFT to REQUESTED status.
         * Sends email notification to the client and adds a timeline entry.
         * @param documentId the document to send
         * @param brokerId the broker making the request
         * @return the updated document
         */
        DocumentResponseDTO sendDocumentRequest(UUID documentId, UUID brokerId);
}
