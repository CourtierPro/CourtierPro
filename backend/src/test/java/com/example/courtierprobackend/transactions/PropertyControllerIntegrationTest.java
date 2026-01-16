package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyAddressDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus;
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
 * Integration tests for property-related endpoints in TransactionController.
 * Tests HTTP layer behavior including request/response handling, validation,
 * and proper status codes.
 */
@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PropertyControllerIntegrationTest {

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

    // ==================== GET /transactions/{id}/properties Tests ====================

    @Nested
    @DisplayName("GET /transactions/{transactionId}/properties")
    class GetPropertiesTests {

        @Test
        @DisplayName("should return properties for broker - 200")
        void getProperties_asBroker_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyResponseDTO property = createSamplePropertyResponse();

            when(service.getProperties(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenReturn(List.of(property));

            mockMvc.perform(
                    get("/transactions/{transactionId}/properties", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].propertyId").exists())
                    .andExpect(jsonPath("$[0].address.street").value("123 Test St"));
        }

        @Test
        @DisplayName("should return 403 when missing broker header")
        void getProperties_missingBrokerHeader_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();

            mockMvc.perform(
                    get("/transactions/{transactionId}/properties", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "broker")))
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when transaction not found")
        void getProperties_transactionNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getProperties(eq(transactionId), eq(brokerId), anyBoolean()))
                    .thenThrow(new NotFoundException("Transaction not found"));

            mockMvc.perform(
                    get("/transactions/{transactionId}/properties", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== POST /transactions/{id}/properties Tests ====================

    @Nested
    @DisplayName("POST /transactions/{transactionId}/properties")
    class AddPropertyTests {

        @Test
        @DisplayName("should create property - 201")
        void addProperty_validRequest_returns201() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyRequestDTO request = createSamplePropertyRequest();
            PropertyResponseDTO response = createSamplePropertyResponse();

            when(service.addProperty(eq(transactionId), any(PropertyRequestDTO.class), eq(brokerId)))
                    .thenReturn(response);

            mockMvc.perform(
                    post("/transactions/{transactionId}/properties", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.propertyId").exists())
                    .andExpect(jsonPath("$.address.street").value("123 Test St"))
                    .andExpect(jsonPath("$.offerStatus").value("OFFER_TO_BE_MADE"));
        }

        @Test
        @DisplayName("should return 400 for invalid request body")
        void addProperty_invalidBody_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            // Missing required fields
            String invalidJson = "{}";

            mockMvc.perform(
                    post("/transactions/{transactionId}/properties", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson)
            )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when adding to sell-side transaction")
        void addProperty_sellSideTransaction_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyRequestDTO request = createSamplePropertyRequest();

            when(service.addProperty(eq(transactionId), any(PropertyRequestDTO.class), eq(brokerId)))
                    .thenThrow(new BadRequestException("Properties can only be added to buyer-side transactions"));

            mockMvc.perform(
                    post("/transactions/{transactionId}/properties", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 for non-broker")
        void addProperty_nonBroker_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyRequestDTO request = createSamplePropertyRequest();

            when(service.addProperty(eq(transactionId), any(PropertyRequestDTO.class), eq(brokerId)))
                    .thenThrow(new ForbiddenException("Not authorized"));

            mockMvc.perform(
                    post("/transactions/{transactionId}/properties", transactionId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PUT /transactions/{id}/properties/{propertyId} Tests ====================

    @Nested
    @DisplayName("PUT /transactions/{transactionId}/properties/{propertyId}")
    class UpdatePropertyTests {

        @Test
        @DisplayName("should update property - 200")
        void updateProperty_validRequest_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyRequestDTO request = createSamplePropertyRequest();
            request.setOfferStatus(PropertyOfferStatus.OFFER_MADE);
            request.setOfferAmount(BigDecimal.valueOf(475000));

            PropertyResponseDTO response = createSamplePropertyResponse();
            response.setOfferStatus(PropertyOfferStatus.OFFER_MADE);
            response.setOfferAmount(BigDecimal.valueOf(475000));

            when(service.updateProperty(eq(transactionId), eq(propertyId), any(PropertyRequestDTO.class), eq(brokerId)))
                    .thenReturn(response);

            mockMvc.perform(
                    put("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.offerStatus").value("OFFER_MADE"))
                    .andExpect(jsonPath("$.offerAmount").value(475000));
        }

        @Test
        @DisplayName("should return 404 when property not found")
        void updateProperty_propertyNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyRequestDTO request = createSamplePropertyRequest();

            when(service.updateProperty(eq(transactionId), eq(propertyId), any(PropertyRequestDTO.class), eq(brokerId)))
                    .thenThrow(new NotFoundException("Property not found"));

            mockMvc.perform(
                    put("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when property does not belong to transaction")
        void updateProperty_wrongTransaction_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyRequestDTO request = createSamplePropertyRequest();

            when(service.updateProperty(eq(transactionId), eq(propertyId), any(PropertyRequestDTO.class), eq(brokerId)))
                    .thenThrow(new BadRequestException("Property does not belong to this transaction"));

            mockMvc.perform(
                    put("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request))
            )
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== DELETE /transactions/{id}/properties/{propertyId} Tests ====================

    @Nested
    @DisplayName("DELETE /transactions/{transactionId}/properties/{propertyId}")
    class RemovePropertyTests {

        @Test
        @DisplayName("should remove property - 204")
        void removeProperty_validRequest_returns204() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doNothing().when(service).removeProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    delete("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNoContent());

            verify(service).removeProperty(transactionId, propertyId, brokerId);
        }

        @Test
        @DisplayName("should return 404 when property not found")
        void removeProperty_propertyNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new NotFoundException("Property not found"))
                    .when(service).removeProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    delete("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 for non-broker")
        void removeProperty_nonBroker_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new ForbiddenException("Not authorized"))
                    .when(service).removeProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    delete("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== GET /transactions/{id}/properties/{propertyId} Tests ====================

    @Nested
    @DisplayName("GET /transactions/{transactionId}/properties/{propertyId}")
    class GetPropertyByIdTests {

        @Test
        @DisplayName("should return property - 200")
        void getPropertyById_validRequest_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            PropertyResponseDTO response = createSamplePropertyResponse();
            response.setPropertyId(propertyId);

            when(service.getPropertyById(eq(propertyId), eq(brokerId), anyBoolean()))
                    .thenReturn(response);

            mockMvc.perform(
                    get("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propertyId").value(propertyId.toString()))
                    .andExpect(jsonPath("$.address.street").value("123 Test St"));
        }

        @Test
        @DisplayName("should return 404 when property not found")
        void getPropertyById_propertyNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getPropertyById(eq(propertyId), eq(brokerId), anyBoolean()))
                    .thenThrow(new NotFoundException("Property not found"));

            mockMvc.perform(
                    get("/transactions/{transactionId}/properties/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== PUT /transactions/{id}/active-property/{propertyId} Tests ====================

    @Nested
    @DisplayName("PUT /transactions/{transactionId}/active-property/{propertyId}")
    class SetActivePropertyTests {

        @Test
        @DisplayName("should set active property - 204")
        void setActiveProperty_validRequest_returns204() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doNothing().when(service).setActiveProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    put("/transactions/{transactionId}/active-property/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNoContent());

            verify(service).setActiveProperty(transactionId, propertyId, brokerId);
        }

        @Test
        @DisplayName("should return 404 when transaction not found")
        void setActiveProperty_transactionNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new NotFoundException("Transaction not found"))
                    .when(service).setActiveProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    put("/transactions/{transactionId}/active-property/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when property not found")
        void setActiveProperty_propertyNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new NotFoundException("Property not found"))
                    .when(service).setActiveProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    put("/transactions/{transactionId}/active-property/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when property does not belong to transaction")
        void setActiveProperty_wrongTransaction_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new BadRequestException("Property does not belong to this transaction"))
                    .when(service).setActiveProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    put("/transactions/{transactionId}/active-property/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when property has no address")
        void setActiveProperty_noAddress_returns400() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new BadRequestException("Property must have an address to be set as active"))
                    .when(service).setActiveProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    put("/transactions/{transactionId}/active-property/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 for non-broker")
        void setActiveProperty_nonBroker_returns403() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID propertyId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            doThrow(new ForbiddenException("Not authorized"))
                    .when(service).setActiveProperty(transactionId, propertyId, brokerId);

            mockMvc.perform(
                    put("/transactions/{transactionId}/active-property/{propertyId}", transactionId, propertyId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== Helper Methods ====================

    private PropertyRequestDTO createSamplePropertyRequest() {
        return PropertyRequestDTO.builder()
                .address(new PropertyAddress("123 Test St", "Montreal", "QC", "H1A 1A1"))
                .askingPrice(BigDecimal.valueOf(500000))
                .offerStatus(PropertyOfferStatus.OFFER_TO_BE_MADE)
                .centrisNumber("12345678")
                .notes("Test notes")
                .build();
    }

    private PropertyResponseDTO createSamplePropertyResponse() {
        return PropertyResponseDTO.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .address(new PropertyAddressDTO("123 Test St", "Montreal", "QC", "H1A 1A1"))
                .askingPrice(BigDecimal.valueOf(500000))
                .offerStatus(PropertyOfferStatus.OFFER_TO_BE_MADE)
                .centrisNumber("12345678")
                .notes("Test notes")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
