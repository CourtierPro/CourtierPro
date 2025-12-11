package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.security.UserContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentRequestController.
 * Updated to use HttpServletRequest with internal UUID from UserContextFilter.
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
    void getDocuments_WithInternalId_ResolvesUserIdFromRequest() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        DocumentRequestResponseDTO doc = DocumentRequestResponseDTO.builder().requestId(UUID.randomUUID()).build();
        when(service.getDocumentsForTransaction(txId, internalId)).thenReturn(List.of(doc));

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getDocuments(txId, null, null, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(service).getDocumentsForTransaction(txId, internalId);
    }

    @Test
    void getDocuments_WithHeader_ResolvesUserIdFromHeader() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID headerUuid = UUID.randomUUID();
        String headerId = headerUuid.toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        DocumentRequestResponseDTO doc = DocumentRequestResponseDTO.builder().requestId(UUID.randomUUID()).build();
        when(service.getDocumentsForTransaction(txId, headerUuid)).thenReturn(List.of(doc));

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getDocuments(txId, headerId, null, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).getDocumentsForTransaction(txId, headerUuid);
    }

    @Test
    void getDocuments_WithNoIdSource_ThrowsForbidden() {
        // Arrange
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No internal ID set in request

        // Act & Assert
        assertThatThrownBy(() -> controller.getDocuments(txId, null, null, request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Unable to resolve user id");
    }

    // ========== createDocumentRequest Tests ==========

    @Test
    void createDocumentRequest_ReturnsCreatedResponse() {
        // Arrange
        UUID txId = UUID.randomUUID();
        DocumentRequestRequestDTO requestDTO = new DocumentRequestRequestDTO();
        
        UUID newReqId = UUID.randomUUID();
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(newReqId)
                .build();
        
        when(service.createDocumentRequest(txId, requestDTO)).thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.createDocumentRequest(txId, requestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getRequestId()).isEqualTo(newReqId);
    }

    // ========== getDocumentRequest Tests ==========

    @Test
    void getDocumentRequest_WithValidAccess_ReturnsDocument() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(reqId)
                .build();
        when(service.getDocumentRequest(reqId, internalId)).thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.getDocumentRequest(txId, reqId, null, null, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRequestId()).isEqualTo(reqId);
    }

    // ========== updateDocumentRequest Tests ==========

    @Test
    void updateDocumentRequest_ReturnsUpdatedDocument() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        DocumentRequestRequestDTO requestDTO = new DocumentRequestRequestDTO();
        requestDTO.setCustomTitle("Updated Title");
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(reqId)
                .customTitle("Updated Title")
                .build();
        when(service.updateDocumentRequest(reqId, requestDTO)).thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.updateDocumentRequest(txId, reqId, requestDTO);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCustomTitle()).isEqualTo("Updated Title");
    }

    // ========== deleteDocumentRequest Tests ==========

    @Test
    void deleteDocumentRequest_ReturnsNoContent() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();

        // Act
        ResponseEntity<Void> response = controller.deleteDocumentRequest(txId, reqId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).deleteDocumentRequest(reqId);
    }

    // ========== submitDocument Tests ==========

    @Test
    void submitDocument_AsClient_SetsClientUploaderType() throws IOException {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        String userId = internalId.toString();
        Jwt jwt = createJwt(userId); // No BROKER role
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(reqId)
                .build();
        when(service.submitDocument(txId, reqId, file, internalId, UploadedByRefEnum.CLIENT))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.submitDocument(
                txId, reqId, file, null, jwt, request
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).submitDocument(txId, reqId, file, internalId, UploadedByRefEnum.CLIENT);
    }

    @Test
    void submitDocument_AsBroker_SetsBrokerUploaderType() throws IOException {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        String userId = internalId.toString();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("auth0|broker123")
                .claim("https://courtierpro.dev/roles", List.of("BROKER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(reqId)
                .build();
        when(service.submitDocument(txId, reqId, file, internalId, UploadedByRefEnum.BROKER))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.submitDocument(
                txId, reqId, file, null, jwt, request
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).submitDocument(txId, reqId, file, internalId, UploadedByRefEnum.BROKER);
    }

    @Test
    void submitDocument_WithHeaderId_UsesHeaderForUserId() throws IOException {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID headerUuid = UUID.randomUUID();
        String headerId = headerUuid.toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        
        DocumentRequestResponseDTO responseDTO = DocumentRequestResponseDTO.builder()
                .requestId(reqId)
                .build();
        when(service.submitDocument(txId, reqId, file, headerUuid, UploadedByRefEnum.CLIENT))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<DocumentRequestResponseDTO> response = controller.submitDocument(
                txId, reqId, file, headerId, null, request
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).submitDocument(txId, reqId, file, headerUuid, UploadedByRefEnum.CLIENT);
    }

    // ========== getDocumentDownloadUrl Tests ==========

    @Test
    void getDocumentDownloadUrl_ReturnsUrlInMap() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        when(service.getDocumentDownloadUrl(reqId, docId, internalId))
                .thenReturn("https://presigned.url/doc.pdf");

        // Act
        ResponseEntity<Map<String, String>> response = controller.getDocumentDownloadUrl(
                txId, reqId, docId, null, null, request
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("url")).isEqualTo("https://presigned.url/doc.pdf");
    }

    @Test
    void getDocumentDownloadUrl_WithHeader_UsesHeaderForUserId() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID headerUuid = UUID.randomUUID();
        String headerId = headerUuid.toString(); // Valid UUID string
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        when(service.getDocumentDownloadUrl(reqId, docId, UUID.fromString(headerId)))
                .thenReturn("https://presigned.url/doc.pdf");

        // Act
        ResponseEntity<Map<String, String>> response = controller.getDocumentDownloadUrl(
                txId, reqId, docId, headerId, null, request
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).getDocumentDownloadUrl(reqId, docId, UUID.fromString(headerId));
    }

    // ========== Helper Methods ==========

    private MockHttpServletRequest createRequestWithInternalId(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        return request;
    }

    private Jwt createJwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
