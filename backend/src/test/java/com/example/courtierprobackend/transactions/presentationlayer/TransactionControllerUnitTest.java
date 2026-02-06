package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.datalayer.dto.MissingAutoDraftsResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ParticipantResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.SearchCriteriaRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.SearchCriteriaResponseDTO;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TerminateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.UpdateParticipantRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
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
        dto.setStage("BUYER_OFFER_AND_NEGOTIATION");
        dto.setNote("Note");

        TransactionResponseDTO resp = TransactionResponseDTO.builder()
                .transactionId(txId)
                .currentStage("BUYER_OFFER_AND_NEGOTIATION")
                .build();

        when(transactionService.updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(brokerUuid))).thenReturn(resp);

        // Act
        ResponseEntity<TransactionResponseDTO> response = controller.updateTransactionStage(txId, dto, brokerHeader, null, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCurrentStage()).isEqualTo("BUYER_OFFER_AND_NEGOTIATION");
        verify(transactionService).updateTransactionStage(eq(txId), any(StageUpdateRequestDTO.class), eq(brokerUuid));
    }

    @Test
    void patchTransactionStage_WithInternalId_Success() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_OFFER_AND_NEGOTIATION");
        dto.setNote("Note");

        TransactionResponseDTO resp = TransactionResponseDTO.builder()
                .transactionId(txId)
                .currentStage("BUYER_OFFER_AND_NEGOTIATION")
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
        dto.setStage("BUYER_OFFER_AND_NEGOTIATION");

        // Act & Assert
        assertThatThrownBy(() -> controller.updateTransactionStage(txId, dto, null, null, request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Unable to resolve user id");

        verifyNoInteractions(transactionService);
    }

    @Test
    void getTransactions_AsClient_UsesClientServicePath() {
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);
        request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "CLIENT");

        TransactionResponseDTO dto = TransactionResponseDTO.builder()
                .transactionId(UUID.randomUUID())
                .clientId(clientId)
                .build();
        when(transactionService.getClientTransactions(clientId)).thenReturn(List.of(dto));

        ResponseEntity<List<TransactionResponseDTO>> response = controller.getTransactions(
                null, null, null, null, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(transactionService).getClientTransactions(clientId);
        verify(transactionService, never()).getBrokerTransactions(any(), any(), any(), any());
    }

    @Test
    void terminateTransaction_UsesResolvedBrokerId() {
        UUID txId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        TerminateRequestDTO terminateRequestDTO = new TerminateRequestDTO();
        terminateRequestDTO.setReason("Client changed plans");

        TransactionResponseDTO responseDTO = TransactionResponseDTO.builder()
                .transactionId(txId)
                .status(com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus.TERMINATED_EARLY)
                .build();
        when(transactionService.terminateTransaction(txId, "Client changed plans", brokerId)).thenReturn(responseDTO);

        ResponseEntity<TransactionResponseDTO> response = controller.terminateTransaction(
                txId, terminateRequestDTO, null, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(transactionService).terminateTransaction(txId, "Client changed plans", brokerId);
    }

    @Test
    void getMissingAutoDrafts_DelegatesToService() {
        UUID txId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        MissingAutoDraftsResponseDTO responseDTO = MissingAutoDraftsResponseDTO.builder()
                .stage("BUYER_FINANCIAL_PREPARATION")
                .missingItems(List.of())
                .build();
        when(transactionService.getMissingAutoDrafts(txId, "BUYER_FINANCIAL_PREPARATION", brokerId))
                .thenReturn(responseDTO);

        ResponseEntity<MissingAutoDraftsResponseDTO> response = controller.getMissingAutoDrafts(
                txId, "BUYER_FINANCIAL_PREPARATION", null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStage()).isEqualTo("BUYER_FINANCIAL_PREPARATION");
        verify(transactionService).getMissingAutoDrafts(txId, "BUYER_FINANCIAL_PREPARATION", brokerId);
    }

    @Test
    void getSearchCriteria_WhenServiceReturnsNull_ReturnsNoContent() {
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(userId);
        request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "BROKER");

        when(transactionService.getSearchCriteria(txId, userId, true)).thenReturn(null);

        ResponseEntity<SearchCriteriaResponseDTO> response = controller.getSearchCriteria(txId, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(transactionService).getSearchCriteria(txId, userId, true);
    }

    @Test
    void createOrUpdateSearchCriteria_ForwardsRequestAndRoleContext() {
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(userId);
        request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "CLIENT");

        SearchCriteriaRequestDTO requestDTO = SearchCriteriaRequestDTO.builder().build();
        SearchCriteriaResponseDTO responseDTO = SearchCriteriaResponseDTO.builder()
                .transactionId(txId)
                .build();

        when(transactionService.createOrUpdateSearchCriteria(txId, requestDTO, userId, false))
                .thenReturn(responseDTO);

        ResponseEntity<SearchCriteriaResponseDTO> response = controller.createOrUpdateSearchCriteria(
                txId, requestDTO, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTransactionId()).isEqualTo(txId);
        verify(transactionService).createOrUpdateSearchCriteria(txId, requestDTO, userId, false);
    }

    @Test
    void deleteSearchCriteria_UsesResolvedUserAndReturnsNoContent() {
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(userId);
        request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "BROKER");

        ResponseEntity<Void> response = controller.deleteSearchCriteria(txId, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(transactionService).deleteSearchCriteria(txId, userId, true);
    }

    @Test
    void getSearchCriteria_WhenPresent_ReturnsOk() {
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(userId);
        request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "BROKER");

        SearchCriteriaResponseDTO dto = SearchCriteriaResponseDTO.builder()
                .transactionId(txId)
                .build();
        when(transactionService.getSearchCriteria(txId, userId, true)).thenReturn(dto);

        ResponseEntity<SearchCriteriaResponseDTO> response = controller.getSearchCriteria(txId, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTransactionId()).isEqualTo(txId);
        verify(transactionService).getSearchCriteria(txId, userId, true);
    }

    @Test
    void updateParticipant_DelegatesToService() {
        UUID txId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        UpdateParticipantRequestDTO requestDTO = new UpdateParticipantRequestDTO();
        requestDTO.setName("Updated Participant");

        ParticipantResponseDTO responseDTO = ParticipantResponseDTO.builder()
                .id(participantId)
                .transactionId(txId)
                .name("Updated Participant")
                .build();
        when(transactionService.updateParticipant(txId, participantId, requestDTO, brokerId)).thenReturn(responseDTO);

        ResponseEntity<ParticipantResponseDTO> response = controller.updateParticipant(
                txId, participantId, requestDTO, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Updated Participant");
        verify(transactionService).updateParticipant(txId, participantId, requestDTO, brokerId);
    }

    // ========== Helper Methods ==========

    private MockHttpServletRequest createRequestWithInternalId(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        return request;
    }
}
