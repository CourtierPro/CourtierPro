package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;

import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
@Import(GlobalExceptionHandler.class)
class TransactionControllerIntegrationTest {

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

    @Test
    void createTransaction_validRequest_returns201AndBody() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(clientId);
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any())).thenReturn(EntityDtoUtil.toResponseStub(txId, clientId, brokerId));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
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

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        // With addFilters=false, UserContextFilter doesn't set the internal user ID.
        // When the broker header is missing and no internal ID exists, 
        // the controller's resolveUserId throws 403 Forbidden.
        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_InvalidBody_400() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_ServiceInvalidInput_400() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new BadRequestException("Bad input"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_ServiceNotFound_404() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_Duplicate_409() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new BadRequestException("Duplicate transaction"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void patchTransactionStage_Success_Returns200() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID broker = UUID.randomUUID();

        String payload = "{\"stage\": \"BUYER_OFFER_ACCEPTED\", \"note\": \"Moving fast!\"}";

        when(service.updateTransactionStage(eq(txId), any(), eq(broker)))
                .thenReturn(com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO.builder()
                        .transactionId(txId)
                        .currentStage("BUYER_OFFER_ACCEPTED")
                        .brokerId(broker)
                        .build());

        mockMvc.perform(
                patch("/transactions/{id}/stage", txId)
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", broker.toString())))
                        .header("x-broker-id", broker.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andExpect(status().isOk())
         .andExpect(jsonPath("$.currentStage").value("BUYER_OFFER_ACCEPTED"));
    }

    @Test
    void patchTransactionStage_InvalidEnum_Returns400() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID broker = UUID.randomUUID();

        String payload = "{\"stage\": \"INVALID_STAGE_NAME\"}";

        when(service.updateTransactionStage(eq(txId), any(), eq(broker)))
                .thenThrow(new BadRequestException("invalid stage"));

        mockMvc.perform(
                patch("/transactions/{id}/stage", txId)
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", broker.toString())))
                        .header("x-broker-id", broker.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void patchTransactionStage_EmptyStage_Returns400() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID broker = UUID.randomUUID();

        String payload = "{\"stage\": \"\"}";

        mockMvc.perform(
                patch("/transactions/{id}/stage", txId)
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", broker.toString())))
                        .header("x-broker-id", broker.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andExpect(status().isBadRequest());
    }
}