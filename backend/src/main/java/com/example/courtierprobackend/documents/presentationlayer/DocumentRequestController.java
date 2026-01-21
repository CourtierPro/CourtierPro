package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
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
public class DocumentRequestController {

    private final DocumentRequestService service;

    // -------- UserId extraction (internal UUID from UserContextFilter) --------

    @GetMapping
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<DocumentRequestResponseDTO>> getDocuments(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getDocumentsForTransaction(transactionId, userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentRequestResponseDTO> createDocumentRequest(
            @PathVariable UUID transactionId,
            @RequestBody DocumentRequestRequestDTO requestDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createDocumentRequest(transactionId, requestDTO, brokerId));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<DocumentRequestResponseDTO> getDocumentRequest(
            @PathVariable UUID transactionId,
            @PathVariable UUID requestId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getDocumentRequest(requestId, userId));
    }

    @PutMapping("/{requestId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentRequestResponseDTO> updateDocumentRequest(
            @PathVariable UUID transactionId,
            @PathVariable UUID requestId,
            @RequestBody DocumentRequestRequestDTO requestDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updateDocumentRequest(requestId, requestDTO, brokerId));
    }

    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> deleteDocumentRequest(
            @PathVariable UUID transactionId,
            @PathVariable UUID requestId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.deleteDocumentRequest(requestId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requestId}/submit")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<DocumentRequestResponseDTO> submitDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID requestId,
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

        return ResponseEntity.ok(service.submitDocument(transactionId, requestId, file, userId, uploaderType));
    }

    @GetMapping("/{requestId}/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<Map<String, String>> getDocumentDownloadUrl(
            @PathVariable UUID transactionId,
            @PathVariable UUID requestId,
            @PathVariable UUID documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        String url = service.getDocumentDownloadUrl(requestId, documentId, userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PatchMapping("/{requestId}/review")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentRequestResponseDTO> reviewDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID requestId,
            @RequestBody @Valid DocumentReviewRequestDTO reviewDTO,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, null);
        return ResponseEntity.ok(service.reviewDocument(transactionId, requestId, reviewDTO, brokerId));
    }
}