package com.example.courtierprobackend.appointments.presentationlayer;

import com.example.courtierprobackend.appointments.businesslayer.AppointmentService;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentResponseDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.security.UserContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentController.
 * Tests all endpoints with mocked service.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

        @Mock
        private AppointmentService appointmentService;

        private AppointmentController controller;

        private UUID brokerId;
        private UUID clientId;
        private UUID transactionId;

        @BeforeEach
        void setUp() {
                controller = new AppointmentController(appointmentService);
                brokerId = UUID.randomUUID();
                clientId = UUID.randomUUID();
                transactionId = UUID.randomUUID();
        }

        // ========== Helper Methods ==========

        private AppointmentResponseDTO createTestAppointmentDTO() {
                return new AppointmentResponseDTO(
                                UUID.randomUUID(),
                                "Test Appointment",
                                transactionId,
                                brokerId,
                                "John Broker",
                                clientId,
                                "Jane Client",
                                LocalDateTime.now(),
                                LocalDateTime.now().plusHours(1),
                                AppointmentStatus.PROPOSED,
                                InitiatorType.BROKER,
                                "123 Main St",
                                45.5,
                                -73.5,
                                "Test notes",
                                LocalDateTime.now(),
                                LocalDateTime.now());
        }

        private MockHttpServletRequest createBrokerRequest(UUID internalId) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
                request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "BROKER");
                return request;
        }

        private MockHttpServletRequest createClientRequest(UUID internalId) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
                request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "CLIENT");
                return request;
        }

        private Jwt createJwt(String subject) {
                return Jwt.withTokenValue("token")
                                .header("alg", "RS256")
                                .subject(subject)
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();
        }

        // ========== GET /appointments Tests - Broker ==========

        @Test
        void getAppointments_brokerWithNoFilters_returnsAllBrokerAppointments() {
                // Arrange
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForBroker(brokerId)).thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                null, null, null, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForBroker(brokerId);
        }

        @Test
        void getAppointments_brokerWithPartialDateRange_throwsBadRequest() {
                // Arrange
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                LocalDateTime from = LocalDateTime.now();

                // Act & Assert
                org.junit.jupiter.api.Assertions.assertThrows(BadRequestException.class, () -> {
                        controller.getAppointments(from, null, null, null, jwt, request);
                });

                org.junit.jupiter.api.Assertions.assertThrows(BadRequestException.class, () -> {
                        controller.getAppointments(null, from, null, null, jwt, request); // using from as to
                });
        }

        @Test
        void getAppointments_brokerWithDateRange_returnsFilteredAppointments() {
                // Arrange
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForBrokerByDateRange(brokerId, from, to))
                                .thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                from, to, null, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForBrokerByDateRange(brokerId, from, to);
        }

        @Test
        void getAppointments_brokerWithStatus_returnsFilteredAppointments() {
                // Arrange
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForBrokerByStatus(brokerId, AppointmentStatus.CONFIRMED))
                                .thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                null, null, AppointmentStatus.CONFIRMED, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForBrokerByStatus(brokerId, AppointmentStatus.CONFIRMED);
        }

        @Test
        void getAppointments_brokerWithDateRangeAndStatus_returnsFilteredAppointments() {
                // Arrange
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                AppointmentStatus status = AppointmentStatus.CONFIRMED;
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForBrokerByDateRangeAndStatus(brokerId, from, to, status))
                                .thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                from, to, status, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForBrokerByDateRangeAndStatus(brokerId, from, to, status);
        }

        // ========== GET /appointments Tests - Client ==========

        @Test
        void getAppointments_clientWithNoFilters_returnsAllClientAppointments() {
                // Arrange
                MockHttpServletRequest request = createClientRequest(clientId);
                Jwt jwt = createJwt("auth0|client");
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForClient(clientId)).thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                null, null, null, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForClient(clientId);
        }

        @Test
        void getAppointments_clientWithDateRange_returnsFilteredAppointments() {
                // Arrange
                MockHttpServletRequest request = createClientRequest(clientId);
                Jwt jwt = createJwt("auth0|client");
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForClientByDateRange(clientId, from, to))
                                .thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                from, to, null, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForClientByDateRange(clientId, from, to);
        }

        @Test
        void getAppointments_clientWithStatus_returnsFilteredAppointments() {
                // Arrange
                MockHttpServletRequest request = createClientRequest(clientId);
                Jwt jwt = createJwt("auth0|client");
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForClientByStatus(clientId, AppointmentStatus.DECLINED))
                                .thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                null, null, AppointmentStatus.DECLINED, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForClientByStatus(clientId, AppointmentStatus.DECLINED);
        }

        @Test
        void getAppointments_clientWithDateRangeAndStatus_returnsFilteredAppointments() {
                // Arrange
                MockHttpServletRequest request = createClientRequest(clientId);
                Jwt jwt = createJwt("auth0|client");
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                AppointmentStatus status = AppointmentStatus.CONFIRMED;
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForClientByDateRangeAndStatus(clientId, from, to, status))
                                .thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                from, to, status, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForClientByDateRangeAndStatus(clientId, from, to, status);
        }

        // ========== GET /appointments Tests - Header Override ==========

        @Test
        void getAppointments_withBrokerHeader_usesHeaderForUserId() {
                // Arrange
                UUID headerBrokerId = UUID.randomUUID();
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "BROKER");
                // No internal ID set, but header is provided
                Jwt jwt = createJwt("auth0|broker");
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForBroker(headerBrokerId)).thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointments(
                                null, null, null, headerBrokerId.toString(), jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(appointmentService).getAppointmentsForBroker(headerBrokerId);
        }

        // ========== GET /appointments/{appointmentId} Tests ==========

        @Test
        void getAppointmentById_returnsAppointment() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentById(eq(appointmentId), any(UUID.class))).thenReturn(dto);

                // Act
                ResponseEntity<AppointmentResponseDTO> response = controller.getAppointmentById(
                                appointmentId, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                verify(appointmentService).getAppointmentById(eq(appointmentId), any(UUID.class));
        }

        // ========== GET /appointments/transaction/{transactionId} Tests ==========

        @Test
        void getAppointmentsForTransaction_returnsAppointments() {
                // Arrange
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                AppointmentResponseDTO dto = createTestAppointmentDTO();

                when(appointmentService.getAppointmentsForTransaction(eq(transactionId), any(UUID.class)))
                                .thenReturn(List.of(dto));

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointmentsForTransaction(
                                transactionId, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                verify(appointmentService).getAppointmentsForTransaction(eq(transactionId), any(UUID.class));
        }

        @Test
        void getAppointmentsForTransaction_withNoAppointments_returnsEmptyList() {
                // Arrange
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");

                when(appointmentService.getAppointmentsForTransaction(eq(transactionId), any(UUID.class)))
                                .thenReturn(List.of());

                // Act
                ResponseEntity<List<AppointmentResponseDTO>> response = controller.getAppointmentsForTransaction(
                                transactionId, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEmpty();
        }

        // ========== POST /appointments Tests ==========

        @Test
        void requestAppointment_createsAndReturnsAppointment() {
                // ... (existing content) ...
                MockHttpServletRequest request = createBrokerRequest(brokerId);
                Jwt jwt = createJwt("auth0|broker");
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO requestDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId,
                                "inspection",
                                "Inspection Title",
                                java.time.LocalDate.now().plusDays(1),
                                java.time.LocalTime.of(10, 0),
                                java.time.LocalTime.of(11, 0),
                                "Test message");

                AppointmentResponseDTO responseDTO = createTestAppointmentDTO();

                when(appointmentService.requestAppointment(eq(requestDTO), any(UUID.class))).thenReturn(responseDTO);

                // Act
                ResponseEntity<AppointmentResponseDTO> response = controller.requestAppointment(
                                requestDTO, null, jwt, request);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                verify(appointmentService).requestAppointment(eq(requestDTO), any(UUID.class));
        }

}
