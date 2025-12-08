package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.user.businesslayer.UserProvisioningService;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrokerController.
 */
@ExtendWith(MockitoExtension.class)
class BrokerControllerTest {

    @Mock
    private UserProvisioningService service;

    private BrokerController controller;

    @BeforeEach
    void setUp() {
        controller = new BrokerController(service);
    }

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
