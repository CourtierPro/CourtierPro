package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentService;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.presentationlayer.models.ChecklistToggleRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.StageChecklistResponseDTO;
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

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentController.
 * Updated to use HttpServletRequest with internal UUID from UserContextFilter.
 */
@ExtendWith(MockitoExtension.class)
class DocumentControllerUnitTest {

        @Mock
        private DocumentService service;

        private DocumentController controller;

        @BeforeEach
        void setUp() {
                controller = new DocumentController(service);
        }

        // ========== resolveUserId Tests ==========

        @Test
        void getDocuments_WithInternalId_ResolvesUserIdFromRequest() {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                DocumentResponseDTO doc = DocumentResponseDTO.builder().documentId(UUID.randomUUID())
                                .build();
                when(service.getDocumentsForTransaction(txId, internalId)).thenReturn(List.of(doc));

                // Act
                ResponseEntity<List<DocumentResponseDTO>> response = controller.getDocuments(txId, null, null,
                                request);

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

                DocumentResponseDTO doc = DocumentResponseDTO.builder().documentId(UUID.randomUUID())
                                .build();
                when(service.getDocumentsForTransaction(txId, headerUuid)).thenReturn(List.of(doc));

                // Act
                ResponseEntity<List<DocumentResponseDTO>> response = controller.getDocuments(txId, headerId,
                                null, request);

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

        // ========== createDocument Tests ==========

        @Test
        void createDocument_ReturnsCreatedResponse() {
                // Arrange
                UUID txId = UUID.randomUUID();
                DocumentRequestDTO requestDTO = new DocumentRequestDTO();

                UUID newDocId = UUID.randomUUID();
                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(newDocId)
                                .build();

                when(service.createDocument(eq(txId), eq(requestDTO), any(UUID.class))).thenReturn(responseDTO);

                // Act
                // Use a request with internal ID to ensure resolveUserId works
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);
                ResponseEntity<DocumentResponseDTO> response = controller.createDocument(txId, requestDTO,
                                null, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody().getDocumentId()).isEqualTo(newDocId);
        }

        // ========== getDocument Tests ==========

        @Test
        void getDocument_WithValidAccess_ReturnsDocument() {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.getDocument(docId, internalId)).thenReturn(responseDTO);

                // Act
                ResponseEntity<DocumentResponseDTO> response = controller.getDocument(txId, docId, null,
                                null, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getDocumentId()).isEqualTo(docId);
        }

        // ========== updateDocument Tests ==========

        @Test
        void updateDocument_ReturnsUpdatedDocument() {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                DocumentRequestDTO requestDTO = new DocumentRequestDTO();
                requestDTO.setCustomTitle("Updated Title");

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .customTitle("Updated Title")
                                .build();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                when(service.updateDocument(eq(docId), eq(requestDTO), any(UUID.class))).thenReturn(responseDTO);

                // Act
                ResponseEntity<DocumentResponseDTO> response = controller.updateDocument(txId, docId,
                                requestDTO, null, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getCustomTitle()).isEqualTo("Updated Title");
        }

        // ========== deleteDocument Tests ==========

        @Test
        void deleteDocument_ReturnsNoContent() {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();

                // Act
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                // Act
                ResponseEntity<Void> response = controller.deleteDocument(txId, docId, null, request);

                verify(service).deleteDocument(docId, internalId);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        // ========== submitDocument Tests ==========

        @Test
        void submitDocument_AsClient_SetsClientUploaderType() throws IOException {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                String userId = internalId.toString();
                Jwt jwt = createJwt(userId); // No BROKER role
                MockHttpServletRequest request = createRequestWithInternalId(internalId);
                MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                                "content".getBytes());

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.submitDocument(txId, docId, file, internalId, UploadedByRefEnum.CLIENT))
                                .thenReturn(responseDTO);

                // Act
                ResponseEntity<DocumentResponseDTO> response = controller.submitDocument(
                                txId, docId, file, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).submitDocument(txId, docId, file, internalId, UploadedByRefEnum.CLIENT);
        }

        @Test
        void submitDocument_AsBroker_SetsBrokerUploaderType() throws IOException {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                Jwt jwt = Jwt.withTokenValue("token")
                                .header("alg", "RS256")
                                .subject("auth0|broker123")
                                .claim("https://courtierpro.dev/roles", List.of("BROKER"))
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);
                MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                                "content".getBytes());

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.submitDocument(txId, docId, file, internalId, UploadedByRefEnum.BROKER))
                                .thenReturn(responseDTO);

                // Act
                ResponseEntity<DocumentResponseDTO> response = controller.submitDocument(
                                txId, docId, file, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).submitDocument(txId, docId, file, internalId, UploadedByRefEnum.BROKER);
        }

        @Test
        void submitDocument_WithHeaderId_UsesHeaderForUserId() throws IOException {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID headerUuid = UUID.randomUUID();
                String headerId = headerUuid.toString();
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                                "content".getBytes());

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.submitDocument(txId, docId, file, headerUuid, UploadedByRefEnum.CLIENT))
                                .thenReturn(responseDTO);

                // Act
                ResponseEntity<DocumentResponseDTO> response = controller.submitDocument(
                                txId, docId, file, headerId, null, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).submitDocument(txId, docId, file, headerUuid, UploadedByRefEnum.CLIENT);
        }

        // ========== getDocumentDownloadUrl Tests ==========

        @Test
        void getDocumentDownloadUrl_ReturnsUrlInMap() {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID versionId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                when(service.getDocumentDownloadUrl(docId, versionId, internalId))
                                .thenReturn("https://presigned.url/doc.pdf");

                // Act
                ResponseEntity<Map<String, String>> response = controller.getDocumentDownloadUrl(
                                txId, docId, versionId, null, null, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().get("url")).isEqualTo("https://presigned.url/doc.pdf");
        }

        @Test
        void getDocumentDownloadUrl_WithHeader_UsesHeaderForUserId() {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID versionId = UUID.randomUUID();
                UUID headerUuid = UUID.randomUUID();
                String headerId = headerUuid.toString();
                MockHttpServletRequest request = new MockHttpServletRequest();

                when(service.getDocumentDownloadUrl(docId, versionId, UUID.fromString(headerId)))
                                .thenReturn("https://presigned.url/doc.pdf");

                // Act
                ResponseEntity<Map<String, String>> response = controller.getDocumentDownloadUrl(
                                txId, docId, versionId, headerId, null, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).getDocumentDownloadUrl(docId, versionId, UUID.fromString(headerId));
        }

        @Test
        void getStageChecklist_ReturnsChecklistResponse() {
                UUID txId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);
                String stage = "BUYER_FINANCIAL_PREPARATION";

                StageChecklistResponseDTO dto = StageChecklistResponseDTO.builder()
                                .stage(stage)
                                .items(List.of())
                                .build();
                when(service.getStageChecklist(txId, stage, internalId)).thenReturn(dto);

                ResponseEntity<StageChecklistResponseDTO> response = controller.getStageChecklist(txId, stage, null,
                                null, request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getStage()).isEqualTo(stage);
                verify(service).getStageChecklist(txId, stage, internalId);
        }

        @Test
        void setChecklistManualState_ForwardsPayloadAndPathVariables() {
                UUID txId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);
                String itemKey = "mortgage_pre_approval_letter";

                ChecklistToggleRequestDTO toggleDTO = new ChecklistToggleRequestDTO();
                toggleDTO.setStage("BUYER_FINANCIAL_PREPARATION");
                toggleDTO.setChecked(Boolean.TRUE);

                StageChecklistResponseDTO responseDTO = StageChecklistResponseDTO.builder()
                                .stage(toggleDTO.getStage())
                                .items(List.of())
                                .build();
                when(service.setChecklistManualState(txId, toggleDTO.getStage(), itemKey, true, internalId))
                                .thenReturn(responseDTO);

                ResponseEntity<StageChecklistResponseDTO> response = controller.setChecklistManualState(txId, itemKey,
                                toggleDTO, null, request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).setChecklistManualState(txId, toggleDTO.getStage(), itemKey, true, internalId);
        }

        @Test
        void reviewDocument_UsesResolvedBrokerId() {
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                DocumentReviewRequestDTO reviewDTO = new DocumentReviewRequestDTO();
                reviewDTO.setDecision(com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum.APPROVED);

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.reviewDocument(txId, docId, reviewDTO, internalId)).thenReturn(responseDTO);

                ResponseEntity<DocumentResponseDTO> response = controller.reviewDocument(txId, docId, reviewDTO,
                                request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).reviewDocument(txId, docId, reviewDTO, internalId);
        }

        @Test
        void sendDocumentRequest_UsesResolvedBrokerId() {
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.sendDocumentRequest(docId, internalId)).thenReturn(responseDTO);

                ResponseEntity<DocumentResponseDTO> response = controller.sendDocumentRequest(txId, docId, null,
                                request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).sendDocumentRequest(docId, internalId);
        }

        @Test
        void uploadFileToDocument_UsesBrokerUploaderType() throws IOException {
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);
                MockMultipartFile file = new MockMultipartFile("file", "draft.pdf", "application/pdf",
                                "content".getBytes());

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.uploadFileToDocument(txId, docId, file, internalId, UploadedByRefEnum.BROKER))
                                .thenReturn(responseDTO);

                ResponseEntity<DocumentResponseDTO> response = controller.uploadFileToDocument(txId, docId, file, null,
                                request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).uploadFileToDocument(txId, docId, file, internalId, UploadedByRefEnum.BROKER);
        }

        @Test
        void shareDocumentWithClient_UsesResolvedBrokerId() {
                UUID txId = UUID.randomUUID();
                UUID docId = UUID.randomUUID();
                UUID internalId = UUID.randomUUID();
                MockHttpServletRequest request = createRequestWithInternalId(internalId);

                DocumentResponseDTO responseDTO = DocumentResponseDTO.builder()
                                .documentId(docId)
                                .build();
                when(service.shareDocumentWithClient(docId, internalId)).thenReturn(responseDTO);

                ResponseEntity<DocumentResponseDTO> response = controller.shareDocumentWithClient(txId, docId, null,
                                request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(service).shareDocumentWithClient(docId, internalId);
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
