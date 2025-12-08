package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/transactions/{transactionId}/documents")
@RequiredArgsConstructor
public class DocumentRequestController {

    private final DocumentRequestService service;

    @GetMapping
    public ResponseEntity<List<DocumentRequestResponseDTO>> getDocuments(@PathVariable String transactionId) {
        return ResponseEntity.ok(service.getDocumentsForTransaction(transactionId));
    }

    @PostMapping
    public ResponseEntity<DocumentRequestResponseDTO> createDocumentRequest(
            @PathVariable String transactionId,
            @RequestBody DocumentRequestRequestDTO requestDTO
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDocumentRequest(transactionId, requestDTO));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<DocumentRequestResponseDTO> getDocumentRequest(
            @PathVariable String transactionId,
            @PathVariable String requestId
    ) {
        return ResponseEntity.ok(service.getDocumentRequest(requestId));
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<DocumentRequestResponseDTO> updateDocumentRequest(
            @PathVariable String transactionId,
            @PathVariable String requestId,
            @RequestBody DocumentRequestRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(service.updateDocumentRequest(requestId, requestDTO));
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteDocumentRequest(
            @PathVariable String transactionId,
            @PathVariable String requestId
    ) {
        service.deleteDocumentRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requestId}/submit")
    public ResponseEntity<DocumentRequestResponseDTO> submitDocument(
            @PathVariable String transactionId,
            @PathVariable String requestId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        // Extract user ID from JWT or header (simplified for now)
        String userId = jwt != null ? jwt.getSubject() : "anonymous";
        UploadedByRefEnum uploaderType = UploadedByRefEnum.CLIENT; // Default to client for this endpoint

        return ResponseEntity.ok(service.submitDocument(transactionId, requestId, file, userId, uploaderType));
    }

    @GetMapping("/{requestId}/documents/{documentId}/download")
    public ResponseEntity<java.util.Map<String, String>> getDocumentDownloadUrl(
            @PathVariable String transactionId,
            @PathVariable String requestId,
            @PathVariable String documentId
    ) {
        String url = service.getDocumentDownloadUrl(requestId, documentId);
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }
}
