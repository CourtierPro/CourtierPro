package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;

import com.example.courtierprobackend.transactions.exceptions.TransactionControllerExceptionHandler;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc // Security enabled
@Import(TransactionControllerExceptionHandler.class)
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @Autowired
    private ObjectMapper mapper;

    private static final SimpleGrantedAuthority ROLE_BROKER = new SimpleGrantedAuthority("ROLE_BROKER");

    @Test
    void createTransaction_validRequest_returns201AndBody() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any())).thenReturn(EntityDtoUtil.toResponseStub("TX-1", "CLIENT1", "BROKER1"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value("CLIENT1"))
                .andExpect(jsonPath("$.brokerId").value("BROKER1"))
                .andExpect(jsonPath("$.side").value("BUY_SIDE"))
                .andExpect(jsonPath("$.currentStage").value("BUYER_PREQUALIFY_FINANCIALLY"))
                .andExpect(jsonPath("$.openedDate").isNotEmpty());
    }

    @Test
    void createTransaction_missingBrokerHeader_returns400() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any())).thenThrow(new InvalidInputException("brokerId is required"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
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
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new InvalidInputException("Bad input"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_ServiceNotFound_404() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_Duplicate_409() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);
        dto.setInitialStage("BUYER_PREQUALIFY_FINANCIALLY");

        when(service.createTransaction(any()))
                .thenThrow(new InvalidInputException("Duplicate transaction"));

        mockMvc.perform(
                post("/transactions")
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", "BROKER1")))
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void patchTransactionStage_Success_Returns200() throws Exception {
        String txId = "TX-PATCH-1";
        String broker = "BROKER1";

        String payload = "{\"stage\": \"BUYER_OFFER_ACCEPTED\", \"note\": \"Moving fast!\"}";

        when(service.updateTransactionStage(eq(txId), any(), eq(broker)))
                .thenReturn(com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO.builder()
                        .transactionId(txId)
                        .currentStage("BUYER_OFFER_ACCEPTED")
                        .brokerId(broker)
                        .build());

        mockMvc.perform(
                patch("/transactions/{id}/stage", txId)
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", broker)))
                        .header("x-broker-id", broker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andExpect(status().isOk())
         .andExpect(jsonPath("$.currentStage").value("BUYER_OFFER_ACCEPTED"));
    }

    @Test
    void patchTransactionStage_InvalidEnum_Returns400() throws Exception {
        String txId = "TX-PATCH-2";
        String broker = "BROKER1";

        String payload = "{\"stage\": \"INVALID_STAGE_NAME\"}";

        when(service.updateTransactionStage(eq(txId), any(), eq(broker)))
                .thenThrow(new InvalidInputException("invalid stage"));

        mockMvc.perform(
                patch("/transactions/{id}/stage", txId)
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", broker)))
                        .header("x-broker-id", broker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void patchTransactionStage_EmptyStage_Returns400() throws Exception {
        String txId = "TX-PATCH-3";
        String broker = "BROKER1";

        String payload = "{\"stage\": \"\"}";

        mockMvc.perform(
                patch("/transactions/{id}/stage", txId)
                        .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", broker)))
                        .header("x-broker-id", broker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        ).andExpect(status().isBadRequest());
    }
}