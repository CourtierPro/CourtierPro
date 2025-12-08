package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class GlobalDocumentController {

    private final DocumentRequestService service;

    @GetMapping
    public ResponseEntity<List<DocumentRequestResponseDTO>> getAllDocumentsForUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(service.getAllDocumentsForUser(userId));
    }
}
