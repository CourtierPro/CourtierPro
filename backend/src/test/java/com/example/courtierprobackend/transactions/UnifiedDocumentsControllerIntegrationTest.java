package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.UnifiedDocumentDTO;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for unified documents endpoint in TransactionController.
 * Tests the /transactions/{transactionId}/all-documents endpoint which aggregates
 * documents from all sources (client uploads, offer attachments, property offer attachments).
 */
@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UnifiedDocumentsControllerIntegrationTest {

    @MockitoBean
    private TimelineService timelineService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @MockitoBean
    private UserContextFilter userContextFilter;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private static final SimpleGrantedAuthority ROLE_BROKER = new SimpleGrantedAuthority("ROLE_BROKER");
    private static final SimpleGrantedAuthority ROLE_CLIENT = new SimpleGrantedAuthority("ROLE_CLIENT");

    @Nested
    @DisplayName("GET /transactions/{transactionId}/all-documents")
    class GetAllDocumentsTests {

        @Test
        @DisplayName("should return all documents for broker - 200")
        void getAllDocuments_asBroker_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            List<UnifiedDocumentDTO> documents = List.of(
                    createClientUploadDocument(),
                    createOfferAttachmentDocument(),
                    createPropertyOfferAttachmentDocument()
            );

            when(service.getAllTransactionDocuments(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenReturn(documents);

            mockMvc.perform(
                    get("/transactions/{transactionId}/all-documents", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].source").value("CLIENT_UPLOAD"))
                    .andExpect(jsonPath("$[1].source").value("OFFER_ATTACHMENT"))
                    .andExpect(jsonPath("$[2].source").value("PROPERTY_OFFER_ATTACHMENT"));
        }

        @Test
        @DisplayName("should return all documents for client - 200")
        void getAllDocuments_asClient_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            List<UnifiedDocumentDTO> documents = List.of(
                    createClientUploadDocument(),
                    createOfferAttachmentDocument()
            );

            when(service.getAllTransactionDocuments(eq(transactionId), eq(clientId), eq(false)))
                    .thenReturn(documents);

            mockMvc.perform(
                    get("/transactions/{transactionId}/all-documents", transactionId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", clientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("should return empty list when no documents - 200")
        void getAllDocuments_noDocuments_returnsEmptyList() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getAllTransactionDocuments(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenReturn(List.of());

            mockMvc.perform(
                    get("/transactions/{transactionId}/all-documents", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("should return 404 when transaction not found")
        void getAllDocuments_transactionNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getAllTransactionDocuments(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenThrow(new NotFoundException("Transaction not found"));

            mockMvc.perform(
                    get("/transactions/{transactionId}/all-documents", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when user has no access")
        void getAllDocuments_noAccess_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getAllTransactionDocuments(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenThrow(new ForbiddenException("You do not have access to this transaction"));

            mockMvc.perform(
                    get("/transactions/{transactionId}/all-documents", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when missing broker header")
        void getAllDocuments_missingBrokerHeader_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();

            mockMvc.perform(
                    get("/transactions/{transactionId}/all-documents", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "broker")))
            )
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== Helper Methods ====================

    private UnifiedDocumentDTO createClientUploadDocument() {
        return UnifiedDocumentDTO.builder()
                .documentId(UUID.randomUUID())
                .fileName("pre_approval.pdf")
                .mimeType("application/pdf")
                .sizeBytes(1024L)
                .uploadedAt(LocalDateTime.now())
                .source("CLIENT_UPLOAD")
                .sourceId(UUID.randomUUID())
                .sourceName("Pre-Approval Letter")
                .status("APPROVED")
                .build();
    }

    private UnifiedDocumentDTO createOfferAttachmentDocument() {
        return UnifiedDocumentDTO.builder()
                .documentId(UUID.randomUUID())
                .fileName("promise_to_purchase.pdf")
                .mimeType("application/pdf")
                .sizeBytes(2048L)
                .uploadedAt(LocalDateTime.now())
                .source("OFFER_ATTACHMENT")
                .sourceId(UUID.randomUUID())
                .sourceName("Offer - John Doe")
                .status(null)
                .build();
    }

    private UnifiedDocumentDTO createPropertyOfferAttachmentDocument() {
        return UnifiedDocumentDTO.builder()
                .documentId(UUID.randomUUID())
                .fileName("counter_offer.pdf")
                .mimeType("application/pdf")
                .sizeBytes(1536L)
                .uploadedAt(LocalDateTime.now())
                .source("PROPERTY_OFFER_ATTACHMENT")
                .sourceId(UUID.randomUUID())
                .sourceName("Property Offer - 123 Main St")
                .status(null)
                .build();
    }
}
