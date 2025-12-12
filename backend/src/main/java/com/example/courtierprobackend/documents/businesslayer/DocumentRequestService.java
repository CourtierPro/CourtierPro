package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface DocumentRequestService {

    List<DocumentRequestResponseDTO> getDocumentsForTransaction(UUID transactionId, UUID userId);

    DocumentRequestResponseDTO getDocumentRequest(UUID requestId, UUID userId);

    DocumentRequestResponseDTO createDocumentRequest(UUID transactionId, DocumentRequestRequestDTO requestDTO);

    DocumentRequestResponseDTO updateDocumentRequest(UUID requestId, DocumentRequestRequestDTO requestDTO);

    void deleteDocumentRequest(UUID requestId);

    DocumentRequestResponseDTO submitDocument(UUID transactionId, UUID requestId, MultipartFile file, UUID uploaderId, UploadedByRefEnum uploaderType) throws IOException;

    List<DocumentRequestResponseDTO> getAllDocumentsForUser(UUID userId);

    String getDocumentDownloadUrl(UUID requestId, UUID documentId, UUID userId);

    DocumentRequestResponseDTO reviewDocument(UUID transactionId, UUID requestId, DocumentReviewRequestDTO reviewDTO, UUID brokerId);
}
