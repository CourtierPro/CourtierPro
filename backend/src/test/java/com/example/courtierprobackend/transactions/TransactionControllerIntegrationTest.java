package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;

import com.example.courtierprobackend.transactions.exceptions.TransactionControllerExceptionHandler;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TransactionControllerExceptionHandler.class)
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @Autowired
    private ObjectMapper mapper;

    // 1) SUCCESS CASE (201)
    @Test
    void createTransaction_201() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any())).thenReturn(null);

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isCreated());
    }

    // 2) MISSING BROKER HEADER → 400
    @Test
    void createTransaction_MissingBrokerHeader_400() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any())).thenReturn(null);

        mockMvc.perform(
                post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isCreated());
    }

    // 3) INVALID BODY → 400 BAD REQUEST
    @Test
    void createTransaction_InvalidBody_400() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    // 4) SERVICE THROWS INVALID INPUT → 400
    @Test
    void createTransaction_ServiceInvalidInput_400() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any()))
                .thenThrow(new InvalidInputException("Bad input"));

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    // 5) SERVICE THROWS NOT FOUND → 404
    @Test
    void createTransaction_ServiceNotFound_404() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any()))
                .thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isNotFound());
    }

    // 6) DUPLICATE TRANSACTION → 400 (or 409 if you change handler)
    @Test
    void createTransaction_Duplicate_409() throws Exception {

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("CLIENT1");
        dto.setSide(TransactionSide.BUY_SIDE);

        when(service.createTransaction(any()))
                .thenThrow(new InvalidInputException("Duplicate transaction"));

        mockMvc.perform(
                post("/api/v1/transactions")
                        .header("x-broker-id", "BROKER1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }
}
