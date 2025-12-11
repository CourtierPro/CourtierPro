package com.example.courtierprobackend.dashboard.presentationlayer;

import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardController.
 * Updated to use HttpServletRequest with internal UUID from UserContextFilter.
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserAccountRepository userRepository;

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(transactionRepository, userRepository);
    }

    // ========== Client Dashboard Tests ==========

    @Test
    void getClientStats_WithActiveTransactions_ReturnsCorrectCount() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        Transaction tx1 = Transaction.builder().clientId(internalId).status(TransactionStatus.ACTIVE).build();
        Transaction tx2 = Transaction.builder().clientId(internalId).status(TransactionStatus.ACTIVE).build();
        Transaction tx3 = Transaction.builder().clientId(internalId).status(TransactionStatus.CLOSED_SUCCESSFULLY).build();
        when(transactionRepository.findAllByClientId(internalId)).thenReturn(List.of(tx1, tx2, tx3));

        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        // Act
        ResponseEntity<DashboardController.ClientDashboardStats> response = controller.getClientStats(null, null, request);

        // Assert
        assertThat(response.getBody().getActiveTransactions()).isEqualTo(2);
    }

    @Test
    void getClientStats_WithHeaderId_UsesHeaderId() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        UUID clientId = UUID.randomUUID();
        String clientHeader = clientId.toString();
        Transaction tx = Transaction.builder().clientId(clientId).status(TransactionStatus.ACTIVE).build();
        when(transactionRepository.findAllByClientId(clientId)).thenReturn(List.of(tx));

        // Act
        ResponseEntity<DashboardController.ClientDashboardStats> response = controller.getClientStats(clientHeader, null, request);

        // Assert
        assertThat(response.getBody().getActiveTransactions()).isEqualTo(1);
    }

    @Test
    void getClientStats_WithNoTransactions_ReturnsZero() {
        // Arrange
        // Arrange
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        when(transactionRepository.findAllByClientId(internalId)).thenReturn(List.of());

        // Act
        ResponseEntity<DashboardController.ClientDashboardStats> response = controller.getClientStats(null, null, request);

        // Assert
        assertThat(response.getBody().getActiveTransactions()).isZero();
    }

    // ========== Broker Dashboard Tests ==========

    @Test
    void getBrokerStats_WithActiveTransactions_ReturnsCorrectStats() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        Transaction tx1 = Transaction.builder().brokerId(internalId).clientId(UUID.randomUUID()).status(TransactionStatus.ACTIVE).build();
        Transaction tx2 = Transaction.builder().brokerId(internalId).clientId(UUID.randomUUID()).status(TransactionStatus.ACTIVE).build();
        when(transactionRepository.findAllByBrokerId(internalId)).thenReturn(List.of(tx1, tx2));

        // Act
        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(null, null, request);

        // Assert
        assertThat(response.getBody().getActiveTransactions()).isEqualTo(2);
        assertThat(response.getBody().getActiveClients()).isEqualTo(2);
        assertThat(response.getBody().getTotalCommission()).isEqualTo(10000.0);
    }

    @Test
    void getBrokerStats_WithSameClientMultipleTransactions_CountsUniqueClients() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        UUID clientUuid = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        Transaction tx1 = Transaction.builder().brokerId(internalId).clientId(clientUuid).status(TransactionStatus.ACTIVE).build();
        Transaction tx2 = Transaction.builder().brokerId(internalId).clientId(clientUuid).status(TransactionStatus.ACTIVE).build();
        when(transactionRepository.findAllByBrokerId(internalId)).thenReturn(List.of(tx1, tx2));

        // Act
        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(null, null, request);

        // Assert
        assertThat(response.getBody().getActiveClients()).isEqualTo(1);
    }

    @Test
    void getBrokerStats_WithHeaderId_UsesHeaderId() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        String brokerHeader = brokerUuid.toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(transactionRepository.findAllByBrokerId(brokerUuid)).thenReturn(List.of());

        // Act
        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(brokerHeader, null, request);

        // Assert
        assertThat(response.getBody().getActiveTransactions()).isZero();
    }

    // ========== Admin Dashboard Tests ==========

    @Test
    void getAdminStats_ReturnsCorrectStats() {
        // Arrange
        UserAccount broker1 = new UserAccount("b1", "b1@test.com", "First", "Last", UserRole.BROKER, "en");
        broker1.setActive(true);
        UserAccount broker2 = new UserAccount("b2", "b2@test.com", "First", "Last", UserRole.BROKER, "en");
        broker2.setActive(false);
        UserAccount client = new UserAccount("c1", "c1@test.com", "First", "Last", UserRole.CLIENT, "en");
        client.setActive(true);
        
        when(userRepository.count()).thenReturn(3L);
        when(userRepository.findAll()).thenReturn(List.of(broker1, broker2, client));

        // Act
        ResponseEntity<DashboardController.AdminDashboardStats> response = controller.getAdminStats();

        // Assert
        assertThat(response.getBody().getTotalUsers()).isEqualTo(3);
        assertThat(response.getBody().getActiveBrokers()).isEqualTo(1);
        assertThat(response.getBody().getSystemHealth()).isEqualTo("99.9%");
    }

    @Test
    void getAdminStats_WithNoBrokers_ReturnsZeroActiveBrokers() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        ResponseEntity<DashboardController.AdminDashboardStats> response = controller.getAdminStats();

        // Assert
        assertThat(response.getBody().getActiveBrokers()).isZero();
    }

    // ========== Helper Methods ==========

    private MockHttpServletRequest createRequestWithInternalId(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        return request;
    }
}
