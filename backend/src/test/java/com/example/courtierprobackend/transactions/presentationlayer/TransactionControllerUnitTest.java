package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerUnitTest {

    @Mock
    private TransactionService transactionService;

    private TransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(transactionService);
    }

    @Test
    void patchTransactionStage_Success() {
        // Arrange
        String txId = "TX-1";
        String brokerHeader = "broker-1";

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");
        dto.setNote("Note");

        TransactionResponseDTO resp = TransactionResponseDTO.builder()
                .transactionId(txId)
                .currentStage("BUYER_OFFER_ACCEPTED")
                .build();

        when(transactionService.updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(brokerHeader))).thenReturn(resp);

        // Act
        ResponseEntity<TransactionResponseDTO> response = controller.updateTransactionStage(txId, dto, brokerHeader, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCurrentStage()).isEqualTo("BUYER_OFFER_ACCEPTED");
        verify(transactionService).updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(brokerHeader));
    }

    @Test
    void patchTransactionStage_Unauthorized_whenNoBrokerHeaderOrJwt_throwsForbidden() {
        // Arrange
        String txId = "TX-1";

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");

        // Act & Assert
        assertThatThrownBy(() -> controller.updateTransactionStage(txId, dto, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verifyNoInteractions(transactionService);
    }

    // helper to create Jwt if needed in future tests
    private Jwt createJwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
