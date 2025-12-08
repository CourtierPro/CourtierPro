package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DocumentRequestController.
 */
@ExtendWith(MockitoExtension.class)
class DocumentRequestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentRequestService service;

    @InjectMocks
    private DocumentRequestController controller;

    private ObjectMapper objectMapper;
    private DocumentRequestResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        TransactionRef transactionRef = TransactionRef.builder()
                .transactionId("TX-123")
                .clientId("CLIENT-1")
                .side(TransactionSide.BUY_SIDE)
                .build();

        sampleResponse = DocumentRequestResponseDTO.builder()
                .requestId("REQ-001")
                .transactionRef(transactionRef)
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .customTitle("Proof of Funds")
                .status(DocumentStatusEnum.REQUESTED)
                .expectedFrom(DocumentPartyEnum.CLIENT)
                .submittedDocuments(List.of())
                .brokerNotes("Please upload")
                .lastUpdatedAt(LocalDateTime.now())
                .visibleToClient(true)
                .build();
    }

    // ==================== GET /transactions/{transactionId}/documents ====================

    @Test
    void getDocuments_returnsList() throws Exception {
        when(service.getDocumentsForTransaction(eq("TX-123"), anyString()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/transactions/TX-123/documents")
                .header("x-broker-id", "BROKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("REQ-001"))
                .andExpect(jsonPath("$[0].docType").value("PROOF_OF_FUNDS"));

        verify(service).getDocumentsForTransaction(eq("TX-123"), anyString());
    }

    @Test
    void getDocuments_emptyList() throws Exception {
        when(service.getDocumentsForTransaction(eq("TX-999"), anyString()))
                .thenReturn(List.of());

        mockMvc.perform(get("/transactions/TX-999/documents")
                .header("x-broker-id", "BROKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== POST /transactions/{transactionId}/documents ====================

    @Test
    void createDocumentRequest_returnsCreated() throws Exception {
        DocumentRequestRequestDTO requestDTO = DocumentRequestRequestDTO.builder()
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .customTitle("Proof of Funds")
                .expectedFrom(DocumentPartyEnum.CLIENT)
                .visibleToClient(true)
                .build();

        when(service.createDocumentRequest(eq("TX-123"), any(DocumentRequestRequestDTO.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/transactions/TX-123/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value("REQ-001"));

        verify(service).createDocumentRequest(eq("TX-123"), any(DocumentRequestRequestDTO.class));
    }

    // ==================== GET /transactions/{transactionId}/documents/{requestId} ====================

    @Test
    void getDocumentRequest_returnsDocument() throws Exception {
        when(service.getDocumentRequest(eq("REQ-001"), anyString()))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/transactions/TX-123/documents/REQ-001")
                .header("x-broker-id", "BROKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REQ-001"))
                .andExpect(jsonPath("$.docType").value("PROOF_OF_FUNDS"));

        verify(service).getDocumentRequest(eq("REQ-001"), anyString());
    }

    // ==================== PUT /transactions/{transactionId}/documents/{requestId} ====================

    @Test
    void updateDocumentRequest_returnsUpdated() throws Exception {
        DocumentRequestRequestDTO updateDTO = DocumentRequestRequestDTO.builder()
                .customTitle("Updated Title")
                .build();

        DocumentRequestResponseDTO updatedResponse = DocumentRequestResponseDTO.builder()
                .requestId("REQ-001")
                .customTitle("Updated Title")
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .status(DocumentStatusEnum.REQUESTED)
                .build();

        when(service.updateDocumentRequest(eq("REQ-001"), any(DocumentRequestRequestDTO.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/transactions/TX-123/documents/REQ-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customTitle").value("Updated Title"));

        verify(service).updateDocumentRequest(eq("REQ-001"), any(DocumentRequestRequestDTO.class));
    }

    // ==================== DELETE /transactions/{transactionId}/documents/{requestId} ====================

    @Test
    void deleteDocumentRequest_returnsNoContent() throws Exception {
        doNothing().when(service).deleteDocumentRequest("REQ-001");

        mockMvc.perform(delete("/transactions/TX-123/documents/REQ-001"))
                .andExpect(status().isNoContent());

        verify(service).deleteDocumentRequest("REQ-001");
    }

    // ==================== POST /transactions/{transactionId}/documents/{requestId}/submit ====================

    @Test
    void submitDocument_returnsUpdatedDocument() throws Exception {
        DocumentRequestResponseDTO submittedResponse = DocumentRequestResponseDTO.builder()
                .requestId("REQ-001")
                .status(DocumentStatusEnum.SUBMITTED)
                .build();

        when(service.submitDocument(
                eq("TX-123"),
                eq("REQ-001"),
                any(),
                eq("anonymous"),
                eq(UploadedByRefEnum.CLIENT)
        )).thenReturn(submittedResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        mockMvc.perform(multipart("/transactions/TX-123/documents/REQ-001/submit")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(service).submitDocument(
                eq("TX-123"),
                eq("REQ-001"),
                any(),
                eq("anonymous"),
                eq(UploadedByRefEnum.CLIENT)
        );
    }
}
