package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for offer-related endpoints in TransactionController.
 * Tests HTTP layer behavior including request/response handling, validation,
 * and proper status codes for seller-side transaction offers.
 */
@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OfferControllerIntegrationTest {

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

    @Autowired
    private ObjectMapper mapper;

    private static final SimpleGrantedAuthority ROLE_BROKER = new SimpleGrantedAuthority("ROLE_BROKER");
    private static final SimpleGrantedAuthority ROLE_CLIENT = new SimpleGrantedAuthority("ROLE_CLIENT");

    // ==================== GET /transactions/{id}/offers Tests ====================

    @Nested
    @DisplayName("GET /transactions/{transactionId}/offers")
    class GetOffersTests {

        @Test
        @DisplayName("should return offers for broker - 200")
        void getOffers_asBroker_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferResponseDTO offer = createSampleOfferResponse();

            when(service.getOffers(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenReturn(List.of(offer));

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].offerId").exists())
                    .andExpect(jsonPath("$[0].buyerName").value("John Doe"));
        }

        @Test
        @DisplayName("should return offers for client - 200")
        void getOffers_asClient_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            OfferResponseDTO offer = createSampleOfferResponse();
            // Notes are now visible to clients

            when(service.getOffers(eq(transactionId), eq(clientId), eq(false)))
                    .thenReturn(List.of(offer));

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_CLIENT).jwt(jwt -> jwt.claim("sub", clientId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, clientId)
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].notes").value("Test notes"));
        }

        @Test
        @DisplayName("should return 403 when missing broker header")
        void getOffers_missingBrokerHeader_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "broker")))
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when transaction not found")
        void getOffers_transactionNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getOffers(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenThrow(new NotFoundException("Transaction not found"));

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return empty list for buy-side transaction")
        void getOffers_buySideTransaction_returnsEmptyList() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getOffers(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenReturn(List.of());

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ==================== POST /transactions/{id}/offers Tests ====================

    @Nested
    @DisplayName("POST /transactions/{transactionId}/offers")
    class AddOfferTests {

        @Test
        @DisplayName("should create offer - 201")
        void addOffer_validRequest_returns201() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferRequestDTO request = createSampleOfferRequest();
            OfferResponseDTO response = createSampleOfferResponse();

            when(service.addOffer(eq(transactionId), any(OfferRequestDTO.class), eq(brokerId)))
                    .thenReturn(response);

            mockMvc.perform(
                    post("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.offerId").exists())
                    .andExpect(jsonPath("$.buyerName").value("John Doe"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 400 for missing buyer name")
        void addOffer_missingBuyerName_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            String invalidJson = "{\"offerAmount\": 500000}";

            mockMvc.perform(
                    post("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson)
            )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when adding to buy-side transaction")
        void addOffer_buySideTransaction_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferRequestDTO request = createSampleOfferRequest();

            when(service.addOffer(eq(transactionId), any(OfferRequestDTO.class), eq(brokerId)))
                    .thenThrow(new BadRequestException("Offers can only be added to seller-side transactions"));

            mockMvc.perform(
                    post("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 for non-broker")
        void addOffer_nonBroker_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferRequestDTO request = createSampleOfferRequest();

            when(service.addOffer(eq(transactionId), any(OfferRequestDTO.class), eq(brokerId)))
                    .thenThrow(new ForbiddenException("Not authorized"));

            mockMvc.perform(
                    post("/transactions/{transactionId}/offers", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PUT /transactions/{id}/offers/{offerId} Tests ====================

    @Nested
    @DisplayName("PUT /transactions/{transactionId}/offers/{offerId}")
    class UpdateOfferTests {

        @Test
        @DisplayName("should update offer - 200")
        void updateOffer_validRequest_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferRequestDTO request = createSampleOfferRequest();
            request.setStatus(ReceivedOfferStatus.ACCEPTED);

            OfferResponseDTO response = createSampleOfferResponse();
            response.setStatus(ReceivedOfferStatus.ACCEPTED);

            when(service.updateOffer(eq(transactionId), eq(offerId), any(OfferRequestDTO.class), eq(brokerId)))
                    .thenReturn(response);

            mockMvc.perform(
                    put("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("should return 404 when offer not found")
        void updateOffer_offerNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferRequestDTO request = createSampleOfferRequest();

            when(service.updateOffer(eq(transactionId), eq(offerId), any(OfferRequestDTO.class), eq(brokerId)))
                    .thenThrow(new NotFoundException("Offer not found"));

            mockMvc.perform(
                    put("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when offer does not belong to transaction")
        void updateOffer_wrongTransaction_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferRequestDTO request = createSampleOfferRequest();

            when(service.updateOffer(eq(transactionId), eq(offerId), any(OfferRequestDTO.class), eq(brokerId)))
                    .thenThrow(new BadRequestException("Offer does not belong to this transaction"));

            mockMvc.perform(
                    put("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== DELETE /transactions/{id}/offers/{offerId} Tests ====================

    @Nested
    @DisplayName("DELETE /transactions/{transactionId}/offers/{offerId}")
    class RemoveOfferTests {

        @Test
        @DisplayName("should remove offer - 204")
        void removeOffer_validRequest_returns204() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doNothing().when(service).removeOffer(transactionId, offerId, brokerId);

            mockMvc.perform(
                    delete("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNoContent());

            verify(service).removeOffer(transactionId, offerId, brokerId);
        }

        @Test
        @DisplayName("should return 404 when offer not found")
        void removeOffer_offerNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new NotFoundException("Offer not found"))
                    .when(service).removeOffer(transactionId, offerId, brokerId);

            mockMvc.perform(
                    delete("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 for non-broker")
        void removeOffer_nonBroker_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new ForbiddenException("Not authorized"))
                    .when(service).removeOffer(transactionId, offerId, brokerId);

            mockMvc.perform(
                    delete("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== GET /transactions/{id}/offers/{offerId} Tests ====================

    @Nested
    @DisplayName("GET /transactions/{transactionId}/offers/{offerId}")
    class GetOfferByIdTests {

        @Test
        @DisplayName("should return offer - 200")
        void getOfferById_validRequest_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferResponseDTO response = createSampleOfferResponse();
            response.setOfferId(offerId);

            when(service.getOfferById(eq(offerId), eq(brokerId), anyBoolean()))
                    .thenReturn(response);

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.offerId").value(offerId.toString()))
                    .andExpect(jsonPath("$.buyerName").value("John Doe"));
        }

        @Test
        @DisplayName("should return 404 when offer not found")
        void getOfferById_offerNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getOfferById(eq(offerId), eq(brokerId), anyBoolean()))
                    .thenThrow(new NotFoundException("Offer not found"));

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Helper Methods ====================

    private OfferRequestDTO createSampleOfferRequest() {
        return OfferRequestDTO.builder()
                .buyerName("John Doe")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .notes("Test notes")
                .build();
    }

    private OfferResponseDTO createSampleOfferResponse() {
        return OfferResponseDTO.builder()
                .offerId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .buyerName("John Doe")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .notes("Test notes")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
