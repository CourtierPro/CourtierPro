package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.example.courtierprobackend.transactions.presentationlayer.ClientTransactionController;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for client-facing offer endpoints in ClientTransactionController.
 * Tests HTTP layer behavior for clients accessing offers on sell-side transactions.
 */
@WebMvcTest(ClientTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ClientOfferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @MockitoBean
    private UserContextFilter userContextFilter;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private static final SimpleGrantedAuthority ROLE_CLIENT = new SimpleGrantedAuthority("ROLE_CLIENT");

    // ==================== GET /clients/{clientId}/transactions/{transactionId}/offers Tests ====================

    @Nested
    @DisplayName("GET /clients/{clientId}/transactions/{transactionId}/offers")
    class GetClientOffersTests {

        @Test
        @DisplayName("should return offers for authenticated client - 200")
        void getOffers_asAuthenticatedClient_returns200() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            OfferResponseDTO offer = createSampleOfferResponse(transactionId);
            // Notes are now visible to clients

            when(service.getOffers(eq(transactionId), eq(clientId), eq(false)))
                    .thenReturn(List.of(offer));

            mockMvc.perform(
                    get("/clients/{clientId}/transactions/{transactionId}/offers", clientId, transactionId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", clientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].buyerName").value("John Doe"))
                    .andExpect(jsonPath("$[0].notes").value("Test notes"));
        }

        @Test
        @DisplayName("should return 403 when client accesses another client's offers")
        void getOffers_wrongClient_returns403() throws Exception {
            UUID authenticatedClientId = UUID.randomUUID();
            UUID otherClientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            mockMvc.perform(
                    get("/clients/{clientId}/transactions/{transactionId}/offers", otherClientId, transactionId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", authenticatedClientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, authenticatedClientId)
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when transaction not found")
        void getOffers_transactionNotFound_returns404() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            when(service.getOffers(eq(transactionId), eq(clientId), eq(false)))
                    .thenThrow(new NotFoundException("Transaction not found"));

            mockMvc.perform(
                    get("/clients/{clientId}/transactions/{transactionId}/offers", clientId, transactionId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", clientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return empty list for buy-side transaction")
        void getOffers_buySideTransaction_returnsEmptyList() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            when(service.getOffers(eq(transactionId), eq(clientId), eq(false)))
                    .thenReturn(List.of());

            mockMvc.perform(
                    get("/clients/{clientId}/transactions/{transactionId}/offers", clientId, transactionId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", clientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ==================== GET /clients/{clientId}/transactions/{transactionId}/offers/{offerId} Tests ====================

    @Nested
    @DisplayName("GET /clients/{clientId}/transactions/{transactionId}/offers/{offerId}")
    class GetClientOfferByIdTests {

        @Test
        @DisplayName("should return single offer for authenticated client - 200")
        void getOfferById_asAuthenticatedClient_returns200() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            OfferResponseDTO offer = createSampleOfferResponse(transactionId);
            offer.setOfferId(offerId);
            // Notes are now visible to clients

            when(service.getOfferById(eq(offerId), eq(clientId), eq(false)))
                    .thenReturn(offer);

            mockMvc.perform(
                    get("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}", clientId, transactionId, offerId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", clientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.offerId").value(offerId.toString()))
                    .andExpect(jsonPath("$.buyerName").value("John Doe"))
                    .andExpect(jsonPath("$.notes").value("Test notes"));
        }

        @Test
        @DisplayName("should return 404 when offer not found")
        void getOfferById_offerNotFound_returns404() throws Exception {
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            when(service.getOfferById(eq(offerId), eq(clientId), eq(false)))
                    .thenThrow(new NotFoundException("Offer not found"));

            mockMvc.perform(
                    get("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}", clientId, transactionId, offerId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", clientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when client accesses another client's offer")
        void getOfferById_wrongClient_returns403() throws Exception {
            UUID authenticatedClientId = UUID.randomUUID();
            UUID otherClientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();

            mockMvc.perform(
                    get("/clients/{clientId}/transactions/{transactionId}/offers/{offerId}", otherClientId, transactionId, offerId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", authenticatedClientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, authenticatedClientId)
            )
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== Helper Methods ====================

    private OfferResponseDTO createSampleOfferResponse(UUID transactionId) {
        return OfferResponseDTO.builder()
                .offerId(UUID.randomUUID())
                .transactionId(transactionId)
                .buyerName("John Doe")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .notes("Test notes")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
