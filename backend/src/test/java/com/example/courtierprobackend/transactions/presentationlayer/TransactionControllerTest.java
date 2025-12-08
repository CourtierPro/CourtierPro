package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TimelineEntryType;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TransactionController REST endpoints.
 * Uses MockMvc with mocked TransactionService to test controller behavior.
 */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    // ========== createTransaction Tests ==========

    // ========== getNotes Tests ==========

    @Test
    @WithMockUser(roles = "BROKER")
    void getNotes_withValidTransactionId_returnsNotesList() throws Exception {
        // Arrange
        TimelineEntryDTO note1 = new TimelineEntryDTO();
        note1.setType(TimelineEntryType.NOTE);
        note1.setTitle("Note 1");

        TimelineEntryDTO note2 = new TimelineEntryDTO();
        note2.setType(TimelineEntryType.NOTE);
        note2.setTitle("Note 2");

        when(transactionService.getNotes("TX-789", "auth0|broker-1"))
                .thenReturn(List.of(note1, note2));

        // Act & Assert
        mockMvc.perform(get("/transactions/TX-789/notes")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|broker-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Note 1"))
                .andExpect(jsonPath("$[1].title").value("Note 2"));

        verify(transactionService).getNotes("TX-789", "auth0|broker-1");
    }

    @Test
    @WithMockUser(roles = "BROKER")
    void getNotes_withHeaderBrokerId_usesHeader() throws Exception {
        // Arrange
        when(transactionService.getNotes("TX-999", "broker-header"))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions/TX-999/notes")
                        .with(jwt())
                        .header("x-broker-id", "broker-header"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).getNotes("TX-999", "broker-header");
    }

    // ========== getBrokerTransactions Tests ==========

    @Test
    @WithMockUser(roles = "BROKER")
    void getBrokerTransactions_returnsTransactionsList() throws Exception {
        // Arrange
        TransactionResponseDTO tx1 = TransactionResponseDTO.builder()
                .transactionId("TX-1")
                .brokerId("auth0|broker-1")
                .build();

        TransactionResponseDTO tx2 = TransactionResponseDTO.builder()
                .transactionId("TX-2")
                .brokerId("auth0|broker-1")
                .build();

        when(transactionService.getBrokerTransactions(eq("auth0|broker-1"), any(), any(), any()))
                .thenReturn(List.of(tx1, tx2));

        // Act & Assert
        mockMvc.perform(get("/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|broker-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value("TX-1"))
                .andExpect(jsonPath("$[1].transactionId").value("TX-2"));

        verify(transactionService).getBrokerTransactions(eq("auth0|broker-1"), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "BROKER")
    void getBrokerTransactions_withHeaderBrokerId_usesHeader() throws Exception {
        // Arrange
        when(transactionService.getBrokerTransactions(eq("broker-header"), any(), any(), any()))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions")
                        .with(jwt())
                        .header("x-broker-id", "broker-header"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).getBrokerTransactions(eq("broker-header"), any(), any(), any());
    }

    // ========== getTransactionById Tests ==========

    @Test
    @WithMockUser(roles = "BROKER")
    void getTransactionById_withValidId_returnsTransaction() throws Exception {
        // Arrange
        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .transactionId("TX-ABC")
                .clientId("client-1")
                .brokerId("auth0|broker-1")
                .side(TransactionSide.BUY_SIDE)
                .build();

        when(transactionService.getByTransactionId("TX-ABC", "auth0|broker-1"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/transactions/TX-ABC")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|broker-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-ABC"))
                .andExpect(jsonPath("$.clientId").value("client-1"))
                .andExpect(jsonPath("$.brokerId").value("auth0|broker-1"));

        verify(transactionService).getByTransactionId("TX-ABC", "auth0|broker-1");
    }

    @Test
    @WithMockUser(roles = "BROKER")
    void getTransactionById_withHeaderBrokerId_usesHeader() throws Exception {
        // Arrange
        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .transactionId("TX-XYZ")
                .brokerId("broker-header")
                .build();

        when(transactionService.getByTransactionId("TX-XYZ", "broker-header"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/transactions/TX-XYZ")
                        .with(jwt())
                        .header("x-broker-id", "broker-header"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-XYZ"))
                .andExpect(jsonPath("$.brokerId").value("broker-header"));

        verify(transactionService).getByTransactionId("TX-XYZ", "broker-header");
    }
}
