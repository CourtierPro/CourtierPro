package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.security.UserContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GlobalDocumentController {

    private final DocumentRequestService service;

    @GetMapping
    public ResponseEntity<List<DocumentRequestResponseDTO>> getAllDocumentsForUser(HttpServletRequest request) {
        Object internalIdObj = request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR);
        if (internalIdObj instanceof UUID) {
             return ResponseEntity.ok(service.getAllDocumentsForUser((UUID)internalIdObj));
        } else if (internalIdObj instanceof String) {
             return ResponseEntity.ok(service.getAllDocumentsForUser(UUID.fromString((String)internalIdObj)));
        }
        
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User context not resolved");
    }
}
