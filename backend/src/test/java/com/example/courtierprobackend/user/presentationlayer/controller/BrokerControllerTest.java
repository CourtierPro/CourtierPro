package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.user.businesslayer.UserProvisioningService;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrokerController.
 */
@ExtendWith(MockitoExtension.class)
class BrokerControllerTest {

    @Mock
    private UserProvisioningService service;

    @Mock
    private TransactionService transactionService;

    @Mock
    private HttpServletRequest request;

    private BrokerController controller;

    private static final UUID BROKER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        controller = new BrokerController(service, transactionService);
    }

    @Nested
    @DisplayName("GET /api/broker/clients")
    class GetClientsTests {

        @Test
        void getClients_ReturnsListOfClients() {
            // Arrange
            List<UserResponse> clients = List.of(
                    UserResponse.builder().id("c1-id").email("c1@test.com").role("CLIENT").active(true).build(),
                    UserResponse.builder().id("c2-id").email("c2@test.com").role("CLIENT").active(true).build()
            );
            when(service.getClients()).thenReturn(clients);

            // Act
            List<UserResponse> result = controller.getClients();

            // Assert
            assertThat(result).hasSize(2);
            verify(service).getClients();
        }

        @Test
        void getClients_WithNoClients_ReturnsEmptyList() {
            // Arrange
            when(service.getClients()).thenReturn(List.of());

            // Act
            List<UserResponse> result = controller.getClients();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void getClients_DelegatesCorrectlyToService() {
            // Arrange
            when(service.getClients()).thenReturn(List.of());

            // Act
            controller.getClients();

            // Assert
            verify(service, times(1)).getClients();
            verifyNoMoreInteractions(service);
        }
    }

    @Nested
    @DisplayName("GET /api/broker/clients/{clientId}/transactions")
    class GetClientTransactionsTests {

        @Test
        void getClientTransactions_ReturnsListOfTransactions() {
            // Arrange
            when(request.getAttribute("internalUserId")).thenReturn(BROKER_ID);
            List<TransactionResponseDTO> transactions = List.of(
                    TransactionResponseDTO.builder()
                            .transactionId(UUID.randomUUID())
                            .clientId(CLIENT_ID)
                            .clientName("Test Client")
                            .side(TransactionSide.BUY_SIDE)
                            .status(TransactionStatus.ACTIVE)
                            .build(),
                    TransactionResponseDTO.builder()
                            .transactionId(UUID.randomUUID())
                            .clientId(CLIENT_ID)
                            .clientName("Test Client")
                            .side(TransactionSide.SELL_SIDE)
                            .status(TransactionStatus.CLOSED_SUCCESSFULLY)
                            .build()
            );
            when(transactionService.getBrokerClientTransactions(BROKER_ID, CLIENT_ID)).thenReturn(transactions);

            // Act
            List<TransactionResponseDTO> result = controller.getClientTransactions(CLIENT_ID, request);

            // Assert
            assertThat(result).hasSize(2);
            verify(transactionService).getBrokerClientTransactions(BROKER_ID, CLIENT_ID);
        }

        @Test
        void getClientTransactions_WithNoTransactions_ReturnsEmptyList() {
            // Arrange
            when(request.getAttribute("internalUserId")).thenReturn(BROKER_ID);
            when(transactionService.getBrokerClientTransactions(BROKER_ID, CLIENT_ID)).thenReturn(List.of());

            // Act
            List<TransactionResponseDTO> result = controller.getClientTransactions(CLIENT_ID, request);

            // Assert
            assertThat(result).isEmpty();
            verify(transactionService).getBrokerClientTransactions(BROKER_ID, CLIENT_ID);
        }

        @Test
        void getClientTransactions_DelegatesCorrectlyToService() {
            // Arrange
            when(request.getAttribute("internalUserId")).thenReturn(BROKER_ID);
            when(transactionService.getBrokerClientTransactions(BROKER_ID, CLIENT_ID)).thenReturn(List.of());

            // Act
            controller.getClientTransactions(CLIENT_ID, request);

            // Assert
            verify(transactionService, times(1)).getBrokerClientTransactions(BROKER_ID, CLIENT_ID);
            verifyNoMoreInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("GET /api/broker/clients/{clientId}/all-transactions")
    class GetAllClientTransactionsTests {

        @Test
        void getAllClientTransactions_ReturnsAllTransactionsWithBrokerNames() {
            // Arrange
            UUID otherBrokerId = UUID.randomUUID();
            List<TransactionResponseDTO> transactions = List.of(
                    TransactionResponseDTO.builder()
                            .transactionId(UUID.randomUUID())
                            .clientId(CLIENT_ID)
                            .clientName("Test Client")
                            .brokerId(BROKER_ID)
                            .brokerName("Current Broker")
                            .side(TransactionSide.BUY_SIDE)
                            .status(TransactionStatus.ACTIVE)
                            .build(),
                    TransactionResponseDTO.builder()
                            .transactionId(UUID.randomUUID())
                            .clientId(CLIENT_ID)
                            .clientName("Test Client")
                            .brokerId(otherBrokerId)
                            .brokerName("Other Broker")
                            .side(TransactionSide.SELL_SIDE)
                            .status(TransactionStatus.ACTIVE)
                            .build()
            );
            when(transactionService.getAllClientTransactions(CLIENT_ID)).thenReturn(transactions);

            // Act
            List<TransactionResponseDTO> result = controller.getAllClientTransactions(CLIENT_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getBrokerName()).isEqualTo("Current Broker");
            assertThat(result.get(1).getBrokerName()).isEqualTo("Other Broker");
            verify(transactionService).getAllClientTransactions(CLIENT_ID);
        }

        @Test
        void getAllClientTransactions_WithNoTransactions_ReturnsEmptyList() {
            // Arrange
            when(transactionService.getAllClientTransactions(CLIENT_ID)).thenReturn(List.of());

            // Act
            List<TransactionResponseDTO> result = controller.getAllClientTransactions(CLIENT_ID);

            // Assert
            assertThat(result).isEmpty();
            verify(transactionService).getAllClientTransactions(CLIENT_ID);
        }

        @Test
        void getAllClientTransactions_DelegatesCorrectlyToService() {
            // Arrange
            when(transactionService.getAllClientTransactions(CLIENT_ID)).thenReturn(List.of());

            // Act
            controller.getAllClientTransactions(CLIENT_ID);

            // Assert
            verify(transactionService, times(1)).getAllClientTransactions(CLIENT_ID);
            verifyNoMoreInteractions(transactionService);
        }
    }
}
