package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyAddressDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClientTransactionController.
 * Tests client-facing transaction endpoints and authorization logic.
 * Updated to use HttpServletRequest with internal UUID from UserContextFilter.
 */
@ExtendWith(MockitoExtension.class)
class ClientTransactionControllerTest {
    // ========== getTransactionProperties & getPropertyById Tests ========== 

    @Test
    void getTransactionProperties_WithValidRequest_ReturnsProperties() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);
        PropertyResponseDTO property = PropertyResponseDTO.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(transactionId)
                .address(PropertyAddressDTO.builder().street("123 Main St").city("Testville").province("QC").postalCode("A1A1A1").build())
                .build();
        when(transactionService.getProperties(eq(transactionId), eq(clientId), eq(false))).thenReturn(List.of(property));

        ResponseEntity<List<PropertyResponseDTO>> response = controller.getTransactionProperties(clientId.toString(), transactionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPropertyId()).isEqualTo(property.getPropertyId());
        verify(transactionService).getProperties(transactionId, clientId, false);
    }

    @Test
    void getPropertyById_WithValidRequest_ReturnsProperty() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);
        PropertyResponseDTO property = PropertyResponseDTO.builder()
                .propertyId(propertyId)
                .transactionId(transactionId)
                .address(PropertyAddressDTO.builder().street("123 Main St").city("Testville").province("QC").postalCode("A1A1A1").build())
                .build();
        when(transactionService.getPropertyById(eq(propertyId), eq(clientId), eq(false))).thenReturn(property);

        ResponseEntity<PropertyResponseDTO> response = controller.getPropertyById(clientId.toString(), transactionId, propertyId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPropertyId()).isEqualTo(propertyId);
        verify(transactionService).getPropertyById(propertyId, clientId, false);
    }

    @Mock
    private TransactionService transactionService;

    @Mock
    private com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;

    private ClientTransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new ClientTransactionController(transactionService, userAccountRepository);
    }

    // ========== getClientTransactions Tests ==========

    @Test
    void getClientTransactions_WithValidClient_ReturnsTransactions() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        String clientId = internalId.toString();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        UUID txId = UUID.randomUUID();
        TransactionResponseDTO transaction = TransactionResponseDTO.builder()
                .transactionId(txId)
                .clientId(internalId)
                .build();
        
        when(transactionService.getClientTransactions(internalId)).thenReturn(List.of(transaction));

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTransactionId()).isEqualTo(txId);
        verify(transactionService).getClientTransactions(internalId);
    }

    @Test
    void getClientTransactions_WithMismatchedClientId_ThrowsForbidden() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        String otherClientId = UUID.randomUUID().toString();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(otherClientId, request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("only access your own transactions");
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNullRequest_ThrowsForbidden() {
        // Arrange
        String clientId = UUID.randomUUID().toString();

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(clientId, null))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Unable to resolve user id");
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNoInternalIdInRequest_ThrowsForbidden() {
        // Arrange
        String clientId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No internal ID set in request attributes

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(clientId, request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Unable to resolve user id");
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNoTransactions_ReturnsEmptyList() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        String clientId = internalId.toString();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        when(transactionService.getClientTransactions(internalId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getClientTransactions_WithMultipleTransactions_ReturnsAll() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        String clientId = internalId.toString();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        TransactionResponseDTO tx1 = TransactionResponseDTO.builder().transactionId(UUID.randomUUID()).clientId(internalId).build();
        TransactionResponseDTO tx2 = TransactionResponseDTO.builder().transactionId(UUID.randomUUID()).clientId(internalId).build();
        TransactionResponseDTO tx3 = TransactionResponseDTO.builder().transactionId(UUID.randomUUID()).clientId(internalId).build();
        
        when(transactionService.getClientTransactions(internalId)).thenReturn(List.of(tx1, tx2, tx3));

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    // ========== Helper Methods ==========

    private MockHttpServletRequest createRequestWithInternalId(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        return request;
    }
}
