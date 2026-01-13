package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.BuyerOfferStatus;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PropertyOfferControllerIntegrationTest {

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

    @Nested
    @DisplayName("GET /transactions/{transactionId}/properties/{propertyId}/offers")
    class GetPropertyOffersTests {

        @Test
        @DisplayName("should return property offers - 200")
        void getPropertyOffers_asBroker_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyOfferResponseDTO offer = PropertyOfferResponseDTO.builder()
                    .propertyOfferId(UUID.randomUUID())
                    .offerAmount(new BigDecimal("500000"))
                    .status(BuyerOfferStatus.OFFER_MADE)
                    .build();

            when(service.getPropertyOffers(eq(propertyId), eq(brokerId), eq(true)))
                    .thenReturn(List.of(offer));

            mockMvc.perform(
                    get("/transactions/properties/{propertyId}/offers", propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, brokerId)
                            .requestAttr(UserContextFilter.USER_ROLE_ATTR, "BROKER")
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].offerAmount").value(500000));
        }
    }

    @Nested
    @DisplayName("POST /transactions/{transactionId}/properties/{propertyId}/offers")
    class AddPropertyOfferTests {

        @Test
        @DisplayName("should add property offer - 201")
        void addPropertyOffer_validRequest_returns201() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("500000"))
                    .status(BuyerOfferStatus.OFFER_MADE)
                    .build();

            PropertyOfferResponseDTO response = PropertyOfferResponseDTO.builder()
                    .propertyOfferId(UUID.randomUUID())
                    .offerAmount(new BigDecimal("500000"))
                    .status(BuyerOfferStatus.OFFER_MADE)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(service.addPropertyOffer(eq(propertyId), any(PropertyOfferRequestDTO.class), eq(brokerId)))
                    .thenReturn(response);

            mockMvc.perform(
                    post("/transactions/properties/{propertyId}/offers", propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, brokerId)
                            .requestAttr(UserContextFilter.USER_ROLE_ATTR, "BROKER")
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.propertyOfferId").exists());
        }

        @Test
        @DisplayName("should return 400 when property not found")
        void addPropertyOffer_propertyNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("500000"))
                    .build();

            when(service.addPropertyOffer(eq(propertyId), any(PropertyOfferRequestDTO.class), eq(brokerId)))
                    .thenThrow(new NotFoundException("Property not found"));

            mockMvc.perform(
                    post("/transactions/properties/{propertyId}/offers", propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, brokerId)
                            .requestAttr(UserContextFilter.USER_ROLE_ATTR, "BROKER")
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /transactions/{transactionId}/properties/{propertyId}/offers/{propertyOfferId}")
    class UpdatePropertyOfferTests {

        @Test
        @DisplayName("should update property offer - 200")
        void updatePropertyOffer_validRequest_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID propertyOfferId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("510000"))
                    .status(BuyerOfferStatus.ACCEPTED)
                    .build();

            PropertyOfferResponseDTO response = PropertyOfferResponseDTO.builder()
                    .propertyOfferId(propertyOfferId)
                    .offerAmount(new BigDecimal("510000"))
                    .status(BuyerOfferStatus.ACCEPTED)
                    .build();

            when(service.updatePropertyOffer(eq(propertyId), eq(propertyOfferId), any(PropertyOfferRequestDTO.class), eq(brokerId)))
                    .thenReturn(response);

            mockMvc.perform(
                    put("/transactions/properties/{propertyId}/offers/{propertyOfferId}", 
                            propertyId, propertyOfferId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, brokerId)
                            .requestAttr(UserContextFilter.USER_ROLE_ATTR, "BROKER")
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }
    }
}
