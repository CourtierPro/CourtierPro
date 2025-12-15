package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

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
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {
        @MockBean
        private TimelineService timelineService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserContextFilter userContextFilter;

    @MockBean
    private UserAccountRepository userAccountRepository;

    // ========== createTransaction Tests ==========

    // ========== getNotes Tests ==========

    @Test
    @WithMockUser(roles = "BROKER")
    void getNotes_withValidTransactionId_returnsNotesList() throws Exception {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        String brokerId = brokerUuid.toString();

        var note1 = new com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO();
        note1.setTitle("Note 1");
        var note2 = new com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO();
        note2.setTitle("Note 2");
        when(transactionService.getNotes(txId, brokerUuid)).thenReturn(List.of(note1, note2));

        // Act & Assert
        mockMvc.perform(get("/transactions/" + txId + "/notes")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|broker-1")))
                        .header("x-broker-id", brokerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Note 1"))
                .andExpect(jsonPath("$[1].title").value("Note 2"));

        verify(transactionService).getNotes(txId, brokerUuid);
    }
    
    @Test
    @WithMockUser(roles = "BROKER")
    void getNotes_withHeaderBrokerId_usesHeader() throws Exception {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        String brokerId = brokerUuid.toString();
        
        when(transactionService.getNotes(txId, brokerUuid))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions/" + txId + "/notes")
                        .with(jwt())
                        .header("x-broker-id", brokerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).getNotes(txId, brokerUuid);
    }

    // ========== getBrokerTransactions Tests ==========

    @Test
    @WithMockUser(roles = "BROKER")
    void getBrokerTransactions_returnsTransactionsList() throws Exception {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        String brokerId = brokerUuid.toString();
        UUID tx1Id = UUID.randomUUID();
        UUID tx2Id = UUID.randomUUID();
        
        TransactionResponseDTO tx1 = TransactionResponseDTO.builder()
                .transactionId(tx1Id)
                .brokerId(brokerUuid)
                .build();

        TransactionResponseDTO tx2 = TransactionResponseDTO.builder()
                .transactionId(tx2Id)
                .brokerId(brokerUuid)
                .build();

        when(transactionService.getBrokerTransactions(eq(brokerUuid), any(), any(), any()))
                .thenReturn(List.of(tx1, tx2));

        // Act & Assert
        mockMvc.perform(get("/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|broker-1"))) // JWT sub ignored if header present
                        .header("x-broker-id", brokerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value(tx1Id.toString()))
                .andExpect(jsonPath("$[1].transactionId").value(tx2Id.toString()));

        verify(transactionService).getBrokerTransactions(eq(brokerUuid), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "BROKER")
    void getBrokerTransactions_withHeaderBrokerId_usesHeader() throws Exception {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        String brokerId = brokerUuid.toString();
        
        when(transactionService.getBrokerTransactions(eq(brokerUuid), any(), any(), any()))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions")
                        .with(jwt())
                        .header("x-broker-id", brokerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).getBrokerTransactions(eq(brokerUuid), any(), any(), any());
    }

    // ========== getTransactionById Tests ==========

    @Test
    @WithMockUser(roles = "BROKER")
    void getTransactionById_withValidId_returnsTransaction() throws Exception {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        String brokerId = brokerUuid.toString();
        
        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .transactionId(txId)
                .clientId(UUID.randomUUID())
                .brokerId(brokerUuid)
                .side(TransactionSide.BUY_SIDE)
                .build();

        when(transactionService.getByTransactionId(eq(txId), eq(brokerUuid)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/transactions/" + txId)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|" + brokerId))) // Auth0 ID can be anything, but we use internal ID
                        .header("x-broker-id", brokerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.brokerId").value(brokerId));

        verify(transactionService).getByTransactionId(eq(txId), eq(brokerUuid));
    }

    @Test
    @WithMockUser(roles = "BROKER")
    void getTransactionById_withHeaderBrokerId_usesHeader() throws Exception {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        String brokerId = brokerUuid.toString();
        
        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .transactionId(txId)
                .brokerId(brokerUuid)
                .build();

        when(transactionService.getByTransactionId(eq(txId), eq(brokerUuid)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/transactions/" + txId)
                        .with(jwt())
                        .header("x-broker-id", brokerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.brokerId").value(brokerId));

        verify(transactionService).getByTransactionId(eq(txId), eq(brokerUuid));
    }
}
