package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalDocumentController.
 */
@ExtendWith(MockitoExtension.class)
class GlobalDocumentControllerTest {

    @Mock
    private DocumentRequestService service;
    @Mock
    private Jwt jwt;

    private GlobalDocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new GlobalDocumentController(service);
    }

    @Test
    void getAllDocumentsForUser_ReturnsDocuments() {
        // Arrange
        when(jwt.getSubject()).thenReturn("user-1");
        List<DocumentRequestResponseDTO> docs = List.of(
                DocumentRequestResponseDTO.builder().requestId("r1").build(),
                DocumentRequestResponseDTO.builder().requestId("r2").build()
        );
        when(service.getAllDocumentsForUser("user-1")).thenReturn(docs);

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getAllDocumentsForUser(jwt);

        // Assert
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getAllDocumentsForUser_WithNoDocuments_ReturnsEmptyList() {
        // Arrange
        when(jwt.getSubject()).thenReturn("user-1");
        when(service.getAllDocumentsForUser("user-1")).thenReturn(List.of());

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getAllDocumentsForUser(jwt);

        // Assert
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllDocumentsForUser_DelegatesCorrectlyToService() {
        // Arrange
        when(jwt.getSubject()).thenReturn("user-123");
        when(service.getAllDocumentsForUser("user-123")).thenReturn(List.of());

        // Act
        controller.getAllDocumentsForUser(jwt);

        // Assert
        verify(service).getAllDocumentsForUser("user-123");
    }
}
