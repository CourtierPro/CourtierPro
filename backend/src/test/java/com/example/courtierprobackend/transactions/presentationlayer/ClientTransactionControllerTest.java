package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClientTransactionController.
 * Tests client-facing transaction endpoints and authorization logic.
 */
@ExtendWith(MockitoExtension.class)
class ClientTransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private ClientTransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new ClientTransactionController(transactionService);
    }

    // ========== getClientTransactions Tests ==========

    @Test
    void getClientTransactions_WithValidClient_ReturnsTransactions() {
        // Arrange
        String clientId = "auth0|client123";
        Jwt jwt = createJwt(clientId);
        
        TransactionResponseDTO transaction = TransactionResponseDTO.builder()
                .transactionId("TX-1")
                .clientId(clientId)
                .build();
        
        when(transactionService.getClientTransactions(clientId)).thenReturn(List.of(transaction));

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTransactionId()).isEqualTo("TX-1");
        verify(transactionService).getClientTransactions(clientId);
    }

    @Test
    void getClientTransactions_WithMismatchedClientId_ThrowsForbidden() {
        // Arrange
        String tokenClientId = "auth0|client123";
        String pathClientId = "auth0|otherClient";
        Jwt jwt = createJwt(tokenClientId);

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(pathClientId, jwt))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("only access your own transactions");
                });
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNullJwt_ThrowsForbidden() {
        // Arrange
        String clientId = "auth0|client123";

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(clientId, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("Authentication required");
                });
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithEmptySubject_ThrowsForbidden() {
        // Arrange
        String clientId = "auth0|client123";
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("")  // Empty subject
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(clientId, jwt))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("Unable to resolve client id");
                });
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNoTransactions_ReturnsEmptyList() {
        // Arrange
        String clientId = "auth0|client123";
        Jwt jwt = createJwt(clientId);
        
        when(transactionService.getClientTransactions(clientId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getClientTransactions_WithMultipleTransactions_ReturnsAll() {
        // Arrange
        String clientId = "auth0|client123";
        Jwt jwt = createJwt(clientId);
        
        TransactionResponseDTO tx1 = TransactionResponseDTO.builder().transactionId("TX-1").clientId(clientId).build();
        TransactionResponseDTO tx2 = TransactionResponseDTO.builder().transactionId("TX-2").clientId(clientId).build();
        TransactionResponseDTO tx3 = TransactionResponseDTO.builder().transactionId("TX-3").clientId(clientId).build();
        
        when(transactionService.getClientTransactions(clientId)).thenReturn(List.of(tx1, tx2, tx3));

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void getClientTransactionTimeline_shouldReturn200() {
        // Arrange
        String clientId = "auth0|client123";
        String txId = "TX-100";
        Jwt jwt = createJwt(clientId);

        TimelineEntryDTO t1 = TimelineEntryDTO.builder()
                .title("Public Note")
                .type(com.example.courtierprobackend.transactions.datalayer.enums.TimelineEntryType.NOTE)
                .occurredAt(LocalDateTime.now())
                .visibleToClient(true)
                .build();

        when(transactionService.getClientTransactionTimeline(txId, clientId)).thenReturn(List.of(t1));

        // Act
        var response = controller.getClientTransactionTimeline(clientId, txId, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Public Note");
        verify(transactionService).getClientTransactionTimeline(txId, clientId);
    }

    // ========== Helper Methods ==========

    private Jwt createJwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}

