package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.security.UserContextFilter;
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
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerUnitTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private TimelineService timelineService;

    private TransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(transactionService, timelineService);
    }

    @Test
    void patchTransactionStage_Success() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID brokerUuid = UUID.randomUUID();
        String brokerHeader = brokerUuid.toString();
        MockHttpServletRequest request = new MockHttpServletRequest();

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");
        dto.setNote("Note");

        TransactionResponseDTO resp = TransactionResponseDTO.builder()
                .transactionId(txId)
                .currentStage("BUYER_OFFER_ACCEPTED")
                .build();

        when(transactionService.updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(brokerUuid))).thenReturn(resp);

        // Act
        ResponseEntity<TransactionResponseDTO> response = controller.updateTransactionStage(txId, dto, brokerHeader, null, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCurrentStage()).isEqualTo("BUYER_OFFER_ACCEPTED");
        verify(transactionService).updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(brokerUuid));
    }

    @Test
    void patchTransactionStage_WithInternalId_Success() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");
        dto.setNote("Note");

        TransactionResponseDTO resp = TransactionResponseDTO.builder()
                .transactionId(txId)
                .currentStage("BUYER_OFFER_ACCEPTED")
                .build();

        when(transactionService.updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(internalId))).thenReturn(resp);

        // Act
        ResponseEntity<TransactionResponseDTO> response = controller.updateTransactionStage(txId, dto, null, null, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(transactionService).updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(internalId));
    }

    @Test
    void patchTransactionStage_Unauthorized_whenNoBrokerHeaderOrJwt_throwsForbidden() {
        // Arrange
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No internal ID set

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_ACCEPTED");

        // Act & Assert
        assertThatThrownBy(() -> controller.updateTransactionStage(txId, dto, null, null, request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Unable to resolve user id");

        verifyNoInteractions(transactionService);
    }

    // ========== Helper Methods ==========

    private MockHttpServletRequest createRequestWithInternalId(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        return request;
    }
}
