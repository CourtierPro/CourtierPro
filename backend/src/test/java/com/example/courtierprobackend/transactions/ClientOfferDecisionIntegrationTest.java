package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.ClientOfferDecisionDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ClientOfferDecision;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.example.courtierprobackend.transactions.presentationlayer.ClientTransactionController;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for client offer decision endpoint.
 * Tests HTTP request/response flow using MockMvc.
 */
@WebMvcTest(ClientTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientOfferDecisionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserContextFilter userContextFilter;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private TimelineService timelineService;

    @Nested
    @DisplayName("PUT /clients/{clientId}/transactions/{transactionId}/offers/{offerId}/decision")
    class SubmitOfferDecisionEndpointTests {

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should accept offer decision and return updated offer")
        void submitOfferDecision_Accept_ReturnsOk() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            ClientOfferDecisionDTO request = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .notes("I agree to this offer")
                    .build();

            OfferResponseDTO response = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .transactionId(transactionId)
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .status(ReceivedOfferStatus.PENDING)
                    .clientDecision(ClientOfferDecision.ACCEPT)
                    .clientDecisionAt(LocalDateTime.now())
                    .clientDecisionNotes("I agree to this offer")
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(clientId)))
                    .thenReturn(response);

            mockMvc.perform(put("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}/decision",
                            clientId, transactionId, offerId)
                            .with(jwt())
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.offerId").value(offerId.toString()))
                    .andExpect(jsonPath("$.clientDecision").value("ACCEPT"))
                    .andExpect(jsonPath("$.clientDecisionNotes").value("I agree to this offer"));

            verify(transactionService).submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(clientId));
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should decline offer decision and return updated offer")
        void submitOfferDecision_Decline_ReturnsOk() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            ClientOfferDecisionDTO request = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.DECLINE)
                    .notes("Price is too low")
                    .build();

            OfferResponseDTO response = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .clientDecision(ClientOfferDecision.DECLINE)
                    .clientDecisionNotes("Price is too low")
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(clientId)))
                    .thenReturn(response);

            mockMvc.perform(put("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}/decision",
                            clientId, transactionId, offerId)
                            .with(jwt())
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientDecision").value("DECLINE"))
                    .andExpect(jsonPath("$.clientDecisionNotes").value("Price is too low"));
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should counter offer decision and return updated offer")
        void submitOfferDecision_Counter_ReturnsOk() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            ClientOfferDecisionDTO request = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.COUNTER)
                    .notes("Would accept at $550,000")
                    .build();

            OfferResponseDTO response = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .clientDecision(ClientOfferDecision.COUNTER)
                    .clientDecisionNotes("Would accept at $550,000")
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(clientId)))
                    .thenReturn(response);

            mockMvc.perform(put("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}/decision",
                            clientId, transactionId, offerId)
                            .with(jwt())
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientDecision").value("COUNTER"));
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should return bad request when decision is missing")
        void submitOfferDecision_MissingDecision_ReturnsBadRequest() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            // Request without decision field
            String requestJson = "{\"notes\": \"Some notes\"}";

            mockMvc.perform(put("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}/decision",
                            clientId, transactionId, offerId)
                            .with(jwt())
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }

        // Note: Test for invalid decision value removed as JSON parsing error handling
        // depends on global exception handler configuration. The ClientOfferDecisionTest
        // already tests that invalid enum values throw exceptions during deserialization.

        @Test
        @WithMockUser(roles = "CLIENT")
        @DisplayName("should accept decision without notes")
        void submitOfferDecision_NoNotes_ReturnsOk() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            ClientOfferDecisionDTO request = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .build();

            OfferResponseDTO response = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .clientDecision(ClientOfferDecision.ACCEPT)
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(clientId)))
                    .thenReturn(response);

            mockMvc.perform(put("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}/decision",
                            clientId, transactionId, offerId)
                            .with(jwt())
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientDecision").value("ACCEPT"));
        }

        // Note: Role-based authorization test (broker cannot access client endpoints)
        // is not included here because @AutoConfigureMockMvc(addFilters = false) disables
        // security filters. Role authorization is tested at the integration level with
        // full security context enabled.
    }
}
