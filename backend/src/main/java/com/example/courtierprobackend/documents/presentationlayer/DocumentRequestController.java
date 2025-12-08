package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
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

@RestController
@RequestMapping("/transactions/{transactionId}/documents")
@RequiredArgsConstructor
public class DocumentRequestController {

    private final DocumentRequestService service;

    // -------- UserId extraction (PROD = Auth0, DEV = x-broker-id / x-user-id) --------
    private String resolveUserId(Jwt jwt, String headerId) {

        // DEV mode: header
        if (org.springframework.util.StringUtils.hasText(headerId)) {
            return headerId;
        }

        // PROD mode: Auth0 token
        if (jwt != null) {
            String fromToken = jwt.getClaimAsString("sub");
            if (org.springframework.util.StringUtils.hasText(fromToken)) {
                return fromToken;
            }
        }

        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Unable to resolve user id from token or header"
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<DocumentRequestResponseDTO>> getDocuments(
            @PathVariable String transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = resolveUserId(jwt, brokerHeader);
        return ResponseEntity.ok(service.getDocumentsForTransaction(transactionId, userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentRequestResponseDTO> createDocumentRequest(
            @PathVariable String transactionId,
            @RequestBody DocumentRequestRequestDTO requestDTO
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDocumentRequest(transactionId, requestDTO));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<DocumentRequestResponseDTO> getDocumentRequest(
            @PathVariable String transactionId,
            @PathVariable String requestId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = resolveUserId(jwt, brokerHeader);
        return ResponseEntity.ok(service.getDocumentRequest(requestId, userId));
    }

    @PutMapping("/{requestId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<DocumentRequestResponseDTO> updateDocumentRequest(
            @PathVariable String transactionId,
            @PathVariable String requestId,
            @RequestBody DocumentRequestRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(service.updateDocumentRequest(requestId, requestDTO));
    }

    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> deleteDocumentRequest(
            @PathVariable String transactionId,
            @PathVariable String requestId
    ) {
        service.deleteDocumentRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requestId}/submit")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<DocumentRequestResponseDTO> submitDocument(
            @PathVariable String transactionId,
            @PathVariable String requestId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        String userId = resolveUserId(jwt, brokerHeader);
        UploadedByRefEnum uploaderType = UploadedByRefEnum.CLIENT; // Default to client for this endpoint
        
        // If the user has BROKER role, we might want to set uploaderType to BROKER, 
        // but for now let's assume if they are hitting this endpoint they are acting as the uploader.
        // Ideally we should check the role from the token to determine uploaderType.
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
    public ResponseEntity<java.util.Map<String, String>> getDocumentDownloadUrl(
            @PathVariable String transactionId,
            @PathVariable String requestId,
            @PathVariable String documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = resolveUserId(jwt, brokerHeader);
        String url = service.getDocumentDownloadUrl(requestId, documentId, userId);
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }
}
