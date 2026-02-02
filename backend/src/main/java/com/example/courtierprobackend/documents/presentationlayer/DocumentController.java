package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentService;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import com.example.courtierprobackend.security.UserContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transactions/{transactionId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    // -------- UserId extraction (internal UUID from UserContextFilter) --------

    @GetMapping
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<DocumentResponseDTO>> getDocuments(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getDocumentsForTransaction(transactionId, userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> createDocument(
            @PathVariable UUID transactionId,
            @RequestBody DocumentRequestDTO requestDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createDocument(transactionId, requestDTO, brokerId));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<DocumentResponseDTO> getDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getDocument(documentId, userId));
    }

    @PutMapping("/{documentId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> updateDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestBody DocumentRequestDTO requestDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updateDocument(documentId, requestDTO, brokerId));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.deleteDocument(documentId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/submit")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<DocumentResponseDTO> submitDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) throws IOException {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        UploadedByRefEnum uploaderType = UploadedByRefEnum.CLIENT;

        // Check role from JWT to determine uploader type
        if (jwt != null) {
            List<String> roles = jwt.getClaimAsStringList("https://courtierpro.dev/roles");
            if (roles != null && roles.contains("BROKER")) {
                uploaderType = UploadedByRefEnum.BROKER;
            }
        }

        return ResponseEntity.ok(service.submitDocument(transactionId, documentId, file, userId, uploaderType));
    }

    @GetMapping("/{documentId}/versions/{versionId}/download")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<Map<String, String>> getDocumentDownloadUrl(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        String url = service.getDocumentDownloadUrl(documentId, versionId, userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PatchMapping("/{documentId}/review")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> reviewDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestBody @Valid DocumentReviewRequestDTO reviewDTO,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, null);
        return ResponseEntity.ok(service.reviewDocument(transactionId, documentId, reviewDTO, brokerId));
    }

    /**
     * Transitions a draft document to REQUESTED status.
     * Sends email notification to the client.
     */
    @PostMapping("/{documentId}/send")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> sendDocumentRequest(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.sendDocumentRequest(documentId, brokerId));
    }

    /**
     * Uploads a file to a document without changing its status.
     * Used for attaching files to draft documents.
     */
    @PostMapping("/{documentId}/upload")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> uploadFileToDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) throws IOException {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.uploadFileToDocument(transactionId, documentId, file, brokerId, UploadedByRefEnum.BROKER));
    }

    /**
     * Shares an UPLOAD flow draft document with the client.
     * Transitions from DRAFT to SUBMITTED status.
     */
    @PostMapping("/{documentId}/share")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentResponseDTO> shareDocumentWithClient(
            @PathVariable UUID transactionId,
            @PathVariable UUID documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.shareDocumentWithClient(documentId, brokerId));
    }
}

