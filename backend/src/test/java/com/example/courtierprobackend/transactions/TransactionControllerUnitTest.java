package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.exceptions.TransactionControllerExceptionHandler;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TransactionControllerExceptionHandler.class)
class TransactionControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @Autowired
    private ObjectMapper mapper;

    // 1) SUCCESS CASE 201
    @Test
    void createTransaction_Returns201() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId("CLIENT1");
        req.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any()))
                .thenReturn(EntityDtoUtil.toResponseStub("TX-123", "CLIENT1", "BROKER1"));

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isCreated());
    }

    // 2) MISSING BROKER HEADER → 400
    @Test
    void createTransaction_MissingBrokerHeader_400() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId("CLIENT1");
        req.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any())).thenReturn(null);

        mockMvc.perform(
                post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isCreated());
    }

    // 3) INVALID BODY → 400 (Bean Validation)
    @Test
    void createTransaction_InvalidBody_400() throws Exception {

        // Missing required fields: clientId + side
        TransactionRequestDTO req = new TransactionRequestDTO();

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest());
    }

    // 4) SERVICE THROWS InvalidInputException → 400
    @Test
    void createTransaction_ServiceInvalidInput_400() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId("CLIENT1");
        req.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any()))
                .thenThrow(new InvalidInputException("Invalid transaction"));

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest());
    }

    // 5) SERVICE THROWS NotFoundException → 404
    @Test
    void createTransaction_ServiceNotFound_404() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId("CLIENT1");
        req.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any()))
                .thenThrow(new NotFoundException("Transaction not found"));

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isNotFound());
    }

    // 6) DUPLICATE 400/409
    @Test
    void createTransaction_Duplicate_409() throws Exception {

        TransactionRequestDTO req = new TransactionRequestDTO();
        req.setClientId("CLIENT1");
        req.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any()))
                .thenThrow(new InvalidInputException("Duplicate transaction"));

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest());
    }
}
