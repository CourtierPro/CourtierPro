package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
@Import(GlobalExceptionHandler.class)
class TransactionControllerUnitTest {

        @org.springframework.boot.test.mock.mockito.MockBean
        private com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.businesslayer.TimelineService timelineService;

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

    // Helper to simulate a Broker JWT
    private static final SimpleGrantedAuthority ROLE_BROKER = new SimpleGrantedAuthority("ROLE_BROKER");

    @Test
    void createTransaction_validRequest_returns201AndBody() throws Exception {

        UUID clientId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId(clientId);
        req.setSide(TransactionSide.BUY_SIDE);
        req.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenReturn(EntityDtoUtil.toResponseStub(transactionId, clientId, brokerId));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.brokerId").value(brokerId.toString()))
                .andExpect(jsonPath("$.side").value("BUY_SIDE"))
                .andExpect(jsonPath("$.currentStage").value("BUYER_PREQUALIFY_FINANCIALLY"))
                .andExpect(jsonPath("$.openedDate").isNotEmpty());
    }

    @Test
    void createTransaction_missingBrokerHeader_returns403() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId(UUID.randomUUID());
        req.setSide(TransactionSide.BUY_SIDE);
        req.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        // With addFilters=false, UserContextFilter doesn't set the internal user ID.
        // When the broker header is missing and no internal ID exists,
        // the controller's resolveUserId throws 403 Forbidden.
        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_InvalidBody_400() throws Exception {

        // Missing required fields
        TransactionRequestDTO req = new TransactionRequestDTO();
        UUID brokerId = UUID.randomUUID();

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_ServiceInvalidInput_400() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId(UUID.randomUUID());
        req.setSide(TransactionSide.BUY_SIDE);
        req.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new BadRequestException("Invalid transaction"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", UUID.randomUUID().toString())))
                        .header("x-broker-id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_ServiceNotFound_404() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId(UUID.randomUUID());
        req.setSide(TransactionSide.BUY_SIDE);
        req.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new NotFoundException("Transaction not found"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", UUID.randomUUID().toString())))
                        .header("x-broker-id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_Duplicate_409() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId(UUID.randomUUID());
        req.setSide(TransactionSide.BUY_SIDE);
        req.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new BadRequestException("Duplicate transaction"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", UUID.randomUUID().toString())))
                        .header("x-broker-id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest());
    }
}