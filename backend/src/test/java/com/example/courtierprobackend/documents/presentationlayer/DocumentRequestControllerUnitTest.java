package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentRequestController.
 */
@ExtendWith(MockitoExtension.class)
class DocumentRequestControllerUnitTest {

    @Mock
    private DocumentRequestService service;

    private DocumentRequestController controller;

    @BeforeEach
    void setUp() {
        controller = new DocumentRequestController(service);
    }

    // ========== resolveUserId Tests ==========

    @Test
    void getDocuments_WithJwt_ResolvesUserIdFromToken() {
        // Arrange
        String transactionId = "TX-123";
        String userId = "auth0|user123";
        Jwt jwt = createJwt(userId);
        
        DocumentRequestResponseDTO doc = DocumentRequestResponseDTO.builder().requestId("REQ-1").build();
        when(service.getDocumentsForTransaction(transactionId, userId)).thenReturn(List.of(doc));

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getDocuments(transactionId, null, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(service).getDocumentsForTransaction(transactionId, userId);
    }

    @Test
    void getDocuments_WithHeader_ResolvesUserIdFromHeader() {
        // Arrange
        String transactionId = "TX-123";
        String headerId = "broker-from-header";
        
        DocumentRequestResponseDTO doc = DocumentRequestResponseDTO.builder().requestId("REQ-1").build();
        when(service.getDocumentsForTransaction(transactionId, headerId)).thenReturn(List.of(doc));

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getDocuments(transactionId, headerId, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).getDocumentsForTransaction(transactionId, headerId);
    }

    @Test
    void getDocuments_WithNoIdSource_ThrowsForbidden() {
        // Arrange
        String transactionId = "TX-123";

        // Act & Assert
        assertThatThrownBy(() -> controller.getDocuments(transactionId, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("Unable to resolve user id");
                });
    }

    @Test
    void getDocuments_WithEmptySubject_ThrowsForbidden() {
        // Arrange
        String transactionId = "TX-123";
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> controller.getDocuments(transactionId, null, jwt))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ========== createDocumentRequest Tests ==========

    @Test
    void createDocumentRequest_ReturnsCreatedResponse() {
        // Arrange
        String transactionId = "TX-123";
        DocumentRequestRequestDTO requestDTO = new DocumentRequestRequestDTO();
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId("REQ-NEW")
                .build();
        
        when(service.createDocumentRequest(transactionId, requestDTO)).thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.createDocumentRequest(transactionId, requestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getRequestId()).isEqualTo("REQ-NEW");
    }

    // ========== getDocumentRequest Tests ==========

    @Test
    void getDocumentRequest_WithValidAccess_ReturnsDocument() {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";
        String userId = "auth0|user123";
        Jwt jwt = createJwt(userId);
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(requestId)
                .build();
        when(service.getDocumentRequest(requestId, userId)).thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.getDocumentRequest(transactionId, requestId, null, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRequestId()).isEqualTo(requestId);
    }

    // ========== updateDocumentRequest Tests ==========

    @Test
    void updateDocumentRequest_ReturnsUpdatedDocument() {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";
        DocumentRequestRequestDTO requestDTO = new DocumentRequestRequestDTO();
        requestDTO.setCustomTitle("Updated Title");
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(requestId)
                .customTitle("Updated Title")
                .build();
        when(service.updateDocumentRequest(requestId, requestDTO)).thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.updateDocumentRequest(transactionId, requestId, requestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCustomTitle()).isEqualTo("Updated Title");
    }

    // ========== deleteDocumentRequest Tests ==========

    @Test
    void deleteDocumentRequest_ReturnsNoContent() {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";

        // Act
        ResponseEntity<Void> response = controller.deleteDocumentRequest(transactionId, requestId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).deleteDocumentRequest(requestId);
    }

    // ========== submitDocument Tests ==========

    @Test
    void submitDocument_AsClient_SetsClientUploaderType() throws IOException {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";
        String userId = "auth0|client123";
        Jwt jwt = createJwt(userId);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(requestId)
                .build();
        when(service.submitDocument(transactionId, requestId, file, userId, UploadedByRefEnum.CLIENT))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.submitDocument(
                transactionId, requestId, file, null, jwt
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).submitDocument(transactionId, requestId, file, userId, UploadedByRefEnum.CLIENT);
    }

    @Test
    void submitDocument_AsBroker_SetsBrokerUploaderType() throws IOException {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";
        String userId = "auth0|broker123";
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(userId)
                .claim("https://courtierpro.dev/roles", List.of("BROKER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(requestId)
                .build();
        when(service.submitDocument(transactionId, requestId, file, userId, UploadedByRefEnum.BROKER))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.submitDocument(
                transactionId, requestId, file, null, jwt
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).submitDocument(transactionId, requestId, file, userId, UploadedByRefEnum.BROKER);
    }

    @Test
    void submitDocument_WithHeaderId_UsesHeaderForUserId() throws IOException {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";
        String headerId = "header-broker-id";
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(requestId)
                .build();
        when(service.submitDocument(transactionId, requestId, file, headerId, UploadedByRefEnum.CLIENT))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.submitDocument(
                transactionId, requestId, file, headerId, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).submitDocument(transactionId, requestId, file, headerId, UploadedByRefEnum.CLIENT);
    }

    // ========== getDocumentDownloadUrl Tests ==========

    @Test
    void getDocumentDownloadUrl_ReturnsUrlInMap() {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";
        String documentId = "DOC-1";
        String userId = "auth0|user123";
        Jwt jwt = createJwt(userId);
        
        when(service.getDocumentDownloadUrl(requestId, documentId, userId))
                .thenReturn("https://presigned.url/doc.pdf");

        // Act
        ResponseEntity<Map<String, String>> response = controller.getDocumentDownloadUrl(
                transactionId, requestId, documentId, null, jwt
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("url")).isEqualTo("https://presigned.url/doc.pdf");
    }

    @Test
    void getDocumentDownloadUrl_WithHeader_UsesHeaderForUserId() {
        // Arrange
        String transactionId = "TX-123";
        String requestId = "REQ-1";
        String documentId = "DOC-1";
        String headerId = "header-user-id";
        
        when(service.getDocumentDownloadUrl(requestId, documentId, headerId))
                .thenReturn("https://presigned.url/doc.pdf");

        // Act
        ResponseEntity<Map<String, String>> response = controller.getDocumentDownloadUrl(
                transactionId, requestId, documentId, headerId, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).getDocumentDownloadUrl(requestId, documentId, headerId);
    }

    // ========== Helper Methods ==========

    private Jwt createJwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
