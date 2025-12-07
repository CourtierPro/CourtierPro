package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentRequestService {

    List<DocumentRequestResponseDTO> getDocumentsForTransaction(String transactionId);

    DocumentRequestResponseDTO getDocumentRequest(String requestId);

    DocumentRequestResponseDTO createDocumentRequest(String transactionId, DocumentRequestRequestDTO requestDTO);

    DocumentRequestResponseDTO updateDocumentRequest(String requestId, DocumentRequestRequestDTO requestDTO);

    void deleteDocumentRequest(String requestId);

    DocumentRequestResponseDTO submitDocument(String transactionId, String requestId, MultipartFile file, String uploaderId, UploadedByRefEnum uploaderType) throws IOException;
}
