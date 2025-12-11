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
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
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
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.getParameterType().equals(Jwt.class);
                    }
                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter, org.springframework.web.method.support.ModelAndViewContainer mavContainer, org.springframework.web.context.request.NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return null; 
                    }
                })
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        TransactionRef transactionRef = TransactionRef.builder()
                .transactionId(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .side(TransactionSide.BUY_SIDE)
                .build();

        sampleResponse = DocumentRequestResponseDTO.builder()
                .requestId(UUID.randomUUID())
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
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        sampleResponse.setRequestId(reqId);
        
        when(service.getDocumentsForTransaction(eq(txId), any(UUID.class)))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/transactions/" + txId + "/documents")
                .header("x-broker-id", brokerUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(reqId.toString()))
                .andExpect(jsonPath("$[0].docType").value("PROOF_OF_FUNDS"));

        verify(service).getDocumentsForTransaction(eq(txId), any(UUID.class));
    }

    @Test
    void getDocuments_emptyList() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        when(service.getDocumentsForTransaction(eq(txId), any(UUID.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/transactions/" + txId + "/documents")
                .header("x-broker-id", brokerUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== POST /transactions/{transactionId}/documents ====================

    @Test
    void createDocumentRequest_returnsCreated() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        
        DocumentRequestRequestDTO requestDTO = DocumentRequestRequestDTO.builder()
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .customTitle("Proof of Funds")
                .expectedFrom(DocumentPartyEnum.CLIENT)
                .visibleToClient(true)
                .build();

        sampleResponse.setRequestId(reqId);
        when(service.createDocumentRequest(eq(txId), any(DocumentRequestRequestDTO.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/transactions/" + txId + "/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(reqId.toString()));

        verify(service).createDocumentRequest(eq(txId), any(DocumentRequestRequestDTO.class));
    }

    // ==================== GET /transactions/{transactionId}/documents/{requestId} ====================

    @Test
    void getDocumentRequest_returnsDocument() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        
        sampleResponse.setRequestId(reqId);
        
        when(service.getDocumentRequest(eq(reqId), any(UUID.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/transactions/" + txId + "/documents/" + reqId)
                .header("x-broker-id", brokerUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(reqId.toString()))
                .andExpect(jsonPath("$.docType").value("PROOF_OF_FUNDS"));

        verify(service).getDocumentRequest(eq(reqId), any(UUID.class));
    }

    // ==================== PUT /transactions/{transactionId}/documents/{requestId} ====================

    @Test
    void updateDocumentRequest_returnsUpdated() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        
        DocumentRequestRequestDTO updateDTO = DocumentRequestRequestDTO.builder()
                .customTitle("Updated Title")
                .build();

        DocumentRequestResponseDTO updatedResponse = DocumentRequestResponseDTO.builder()
                .requestId(reqId)
                .customTitle("Updated Title")
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .status(DocumentStatusEnum.REQUESTED)
                .build();

        when(service.updateDocumentRequest(eq(reqId), any(DocumentRequestRequestDTO.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/transactions/" + txId + "/documents/" + reqId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customTitle").value("Updated Title"));

        verify(service).updateDocumentRequest(eq(reqId), any(DocumentRequestRequestDTO.class));
    }

    // ==================== DELETE /transactions/{transactionId}/documents/{requestId} ====================

    @Test
    void deleteDocumentRequest_returnsNoContent() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        
        doNothing().when(service).deleteDocumentRequest(reqId);

        mockMvc.perform(delete("/transactions/" + txId + "/documents/" + reqId))
                .andExpect(status().isNoContent());

        verify(service).deleteDocumentRequest(reqId);
    }

    // ==================== POST /transactions/{transactionId}/documents/{requestId}/submit ====================

    @Test
    void submitDocument_returnsUpdatedDocument() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        
        DocumentRequestResponseDTO submittedResponse = DocumentRequestResponseDTO.builder()
                .requestId(reqId)
                .status(DocumentStatusEnum.SUBMITTED)
                .build();

        when(service.submitDocument(
                eq(txId),
                eq(reqId),
                any(),
                eq(brokerUuid),
                eq(UploadedByRefEnum.CLIENT)
        )).thenReturn(submittedResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        mockMvc.perform(multipart("/transactions/" + txId + "/documents/" + reqId + "/submit")
                        .file(file)
                        .header("x-broker-id", brokerUuid.toString())) // Add auth header
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(service).submitDocument(
                eq(txId),
                eq(reqId),
                any(),
                eq(brokerUuid), // Expect BROKER-1 from header as UUID
                eq(UploadedByRefEnum.CLIENT)
        );
    }
}
