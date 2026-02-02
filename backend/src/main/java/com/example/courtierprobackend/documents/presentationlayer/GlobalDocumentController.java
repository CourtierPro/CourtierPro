package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentResponseDTO;
import com.example.courtierprobackend.security.UserContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GlobalDocumentController {

    private final DocumentService service;

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getAllDocumentsForUser(HttpServletRequest request) {
        UUID internalId = UserContextUtils.resolveUserId(request);
        return ResponseEntity.ok(service.getAllDocumentsForUser(internalId));
    }

    @GetMapping("/outstanding")
    public ResponseEntity<List<com.example.courtierprobackend.documents.presentationlayer.models.OutstandingDocumentDTO>> getOutstandingDocuments(
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request);
        return ResponseEntity.ok(service.getOutstandingDocumentSummary(brokerId));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/remind")
    public ResponseEntity<Void> sendReminder(@org.springframework.web.bind.annotation.PathVariable UUID id,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request);
        service.sendDocumentReminder(id, brokerId);
        return ResponseEntity.ok().build();
    }
}
