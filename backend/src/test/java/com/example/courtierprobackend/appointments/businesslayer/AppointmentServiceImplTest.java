package com.example.courtierprobackend.appointments.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentResponseDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentServiceImpl.
 * Tests all service methods with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceImplTest {

        @Mock
        private AppointmentRepository appointmentRepository;

        @Mock
        private UserAccountRepository userAccountRepository;

        @Mock
        private TransactionRepository transactionRepository;

        @Mock
        private com.example.courtierprobackend.audit.appointment_audit.businesslayer.AppointmentAuditService appointmentAuditService;

        private AppointmentServiceImpl appointmentService;

        private UUID brokerId;
        private UUID clientId;
        private UUID transactionId;

        @Mock
        private com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository transactionParticipantRepository;

        @BeforeEach
        void setUp() {
                appointmentService = new AppointmentServiceImpl(appointmentRepository, userAccountRepository,
                                transactionRepository, appointmentAuditService, transactionParticipantRepository);
                brokerId = UUID.randomUUID();
                clientId = UUID.randomUUID();
                transactionId = UUID.randomUUID();
        }

        // ========== Helper Methods ==========

        private Appointment createTestAppointment() {
                Appointment apt = new Appointment();
                apt.setAppointmentId(UUID.randomUUID());
                apt.setTitle("Test Appointment");
                apt.setTransactionId(transactionId);
                apt.setBrokerId(brokerId);
                apt.setClientId(clientId);
                apt.setFromDateTime(LocalDateTime.now());
                apt.setToDateTime(LocalDateTime.now().plusHours(1));
                apt.setStatus(AppointmentStatus.PROPOSED);
                apt.setInitiatedBy(InitiatorType.BROKER);
                apt.setLocation("123 Main St");
                apt.setLatitude(45.5);
                apt.setLongitude(-73.5);
                apt.setNotes("Test notes");
                apt.setCreatedAt(LocalDateTime.now());
                apt.setUpdatedAt(LocalDateTime.now());
                return apt;
        }

        private UserAccount createUserAccount(UUID id, String firstName, String lastName) {
                UserAccount user = new UserAccount();
                user.setId(id);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                return user;
        }

        private void mockUserAccounts() {
                when(userAccountRepository.findAllById(any()))
                                .thenReturn(List.of(
                                                createUserAccount(brokerId, "John", "Broker"),
                                                createUserAccount(clientId, "Jane", "Client")));
        }

        // ========== getAppointmentsForBroker Tests ==========

        @Test
        void getAppointmentsForBroker_returnsAppointments() {
                // Arrange
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).title()).isEqualTo("Test Appointment");
                assertThat(result.get(0).brokerName()).isEqualTo("John Broker");
                assertThat(result.get(0).clientName()).isEqualTo("Jane Client");
                verify(appointmentRepository).findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId);
        }

        @Test
        void getAppointmentsForBroker_withNoAppointments_returnsEmptyList() {
                // Arrange
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of());

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result).isEmpty();
        }

        // ========== getAppointmentsForClient Tests ==========


        @Test
        void getAppointmentsForClient_asClient_returnsAppointments() {
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId))
                        .thenReturn(List.of(apt));
                mockUserAccounts();
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClient(clientId, clientId, null);
                assertThat(result).hasSize(1);
                assertThat(result.get(0).title()).isEqualTo("Test Appointment");
        }

        @Test
        void getAppointmentsForClient_asBroker_returnsAppointments() {
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId))
                        .thenReturn(List.of(apt));
                mockUserAccounts();
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClient(clientId, brokerId, null);
                assertThat(result).hasSize(1);
        }

        @Test
        void getAppointmentsForClient_asCoBroker_returnsAppointments() {
                Appointment apt = createTestAppointment();
                UUID coBrokerId = UUID.randomUUID();
                when(appointmentRepository.findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId))
                        .thenReturn(List.of(apt));
                mockUserAccounts();
                com.example.courtierprobackend.transactions.datalayer.TransactionParticipant coBroker = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
                coBroker.setUserId(coBrokerId);
                coBroker.setTransactionId(transactionId);
                coBroker.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
                when(transactionParticipantRepository.findByTransactionId(transactionId)).thenReturn(List.of(coBroker));
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClient(clientId, coBrokerId, null);
                assertThat(result).hasSize(1);
        }

        @Test
        void getAppointmentsForClient_forbiddenUser_returnsEmpty() {
                Appointment apt = createTestAppointment();
                UUID otherId = UUID.randomUUID();
                when(appointmentRepository.findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId))
                        .thenReturn(List.of(apt));
                mockUserAccounts();
                when(transactionParticipantRepository.findByTransactionId(transactionId)).thenReturn(List.of());
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClient(clientId, otherId, null);
                assertThat(result).isEmpty();
        }

        @Test
        void getAppointmentsForClient_withNoAppointments_returnsEmptyList() {
                when(appointmentRepository.findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId))
                        .thenReturn(List.of());
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClient(clientId, clientId, null);
                assertThat(result).isEmpty();
        }

        // ========== getAppointmentsForBrokerByDateRange Tests ==========

        @Test
        void getAppointmentsForBrokerByDateRange_returnsFilteredAppointments() {
                // Arrange
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDateRange(brokerId, from, to))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBrokerByDateRange(brokerId,
                                from,
                                to);

                // Assert
                assertThat(result).hasSize(1);
                verify(appointmentRepository).findByBrokerIdAndDateRange(brokerId, from, to);
        }

        // ========== getAppointmentsForClientByDateRange Tests ==========

        @Test
        void getAppointmentsForClientByDateRange_returnsFilteredAppointments() {
                // Arrange
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByClientIdAndDateRange(clientId, from, to))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClientByDateRange(clientId,
                                from,
                                to);

                // Assert
                assertThat(result).hasSize(1);
                verify(appointmentRepository).findByClientIdAndDateRange(clientId, from, to);
        }

        // ========== getAppointmentsForBrokerByStatus Tests ==========

        @Test
        void getAppointmentsForBrokerByStatus_returnsFilteredAppointments() {
                // Arrange
                Appointment apt = createTestAppointment();
                apt.setStatus(AppointmentStatus.CONFIRMED);
                when(appointmentRepository.findByBrokerIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId,
                                AppointmentStatus.CONFIRMED))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBrokerByStatus(brokerId,
                                AppointmentStatus.CONFIRMED);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).status()).isEqualTo(AppointmentStatus.CONFIRMED);
                verify(appointmentRepository).findByBrokerIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId,
                                AppointmentStatus.CONFIRMED);
        }

        // ========== getAppointmentsForClientByStatus Tests ==========

        @Test
        void getAppointmentsForClientByStatus_returnsFilteredAppointments() {
                // Arrange
                Appointment apt = createTestAppointment();
                apt.setStatus(AppointmentStatus.DECLINED);
                when(appointmentRepository.findByClientIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId,
                                AppointmentStatus.DECLINED))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClientByStatus(clientId,
                                AppointmentStatus.DECLINED);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).status()).isEqualTo(AppointmentStatus.DECLINED);
                verify(appointmentRepository).findByClientIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId,
                                AppointmentStatus.DECLINED);
        }

        // ========== getAppointmentsForBrokerByDateRangeAndStatus Tests ==========

        @Test
        void getAppointmentsForBrokerByDateRangeAndStatus_returnsFilteredAppointments() {
                // Arrange
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDateRangeAndStatus(brokerId, from, to,
                                AppointmentStatus.CONFIRMED))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBrokerByDateRangeAndStatus(
                                brokerId,
                                from, to, AppointmentStatus.CONFIRMED);

                // Assert
                assertThat(result).hasSize(1);
                verify(appointmentRepository).findByBrokerIdAndDateRangeAndStatus(brokerId, from, to,
                                AppointmentStatus.CONFIRMED);
        }

        // ========== getAppointmentsForClientByDateRangeAndStatus Tests ==========

        @Test
        void getAppointmentsForClientByDateRangeAndStatus_returnsFilteredAppointments() {
                // Arrange
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.now().plusDays(7);
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByClientIdAndDateRangeAndStatus(clientId, from, to,
                                AppointmentStatus.CONFIRMED))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForClientByDateRangeAndStatus(
                                clientId,
                                from, to, AppointmentStatus.CONFIRMED);

                // Assert
                assertThat(result).hasSize(1);
                verify(appointmentRepository).findByClientIdAndDateRangeAndStatus(clientId, from, to,
                                AppointmentStatus.CONFIRMED);
        }

        // ========== getAppointmentsForTransaction Tests ==========

        @Test
        void getAppointmentsForTransaction_returnsAppointments() {
                // Arrange
                Appointment apt = createTestAppointment();
                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
                when(appointmentRepository.findByTransactionIdAndDeletedAtIsNullOrderByFromDateTimeAsc(transactionId))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForTransaction(transactionId,
                                brokerId);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).transactionId()).isEqualTo(transactionId);
                verify(appointmentRepository)
                                .findByTransactionIdAndDeletedAtIsNullOrderByFromDateTimeAsc(transactionId);
        }

        @Test
        void getAppointmentsForTransaction_forbidden_throwsForbiddenException() {
                // Arrange
                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

                // Act & Assert
                UUID otherUserId = UUID.randomUUID();
                assertThatThrownBy(() -> appointmentService.getAppointmentsForTransaction(transactionId, otherUserId))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have permission");
        }

        @Test
        void getAppointmentsForTransaction_withNoAppointments_returnsEmptyList() {
                // Arrange
                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
                when(appointmentRepository.findByTransactionIdAndDeletedAtIsNullOrderByFromDateTimeAsc(transactionId))
                                .thenReturn(List.of());

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForTransaction(transactionId,
                                brokerId);

                // Assert
                assertThat(result).isEmpty();
        }

        @Test
        void getAppointmentsForTransaction_notFound_throwsNotFoundException() {
                // Arrange
                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.getAppointmentsForTransaction(transactionId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        // ========== getAppointmentById Tests ==========

        @Test
        void getAppointmentById_returnsAppointment() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);
                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));
                mockUserAccounts();

                // Act
                AppointmentResponseDTO result = appointmentService.getAppointmentById(appointmentId, brokerId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.appointmentId()).isEqualTo(appointmentId);
                verify(appointmentRepository).findByAppointmentIdAndDeletedAtIsNull(appointmentId);
        }

        @Test
        void getAppointmentById_forbidden_throwsForbiddenException() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);
                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));

                // Act & Assert
                UUID otherUserId = UUID.randomUUID();
                assertThatThrownBy(() -> appointmentService.getAppointmentById(appointmentId, otherUserId))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have permission");
        }

        @Test
        void getAppointmentById_notFound_throwsNotFoundException() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.getAppointmentById(appointmentId, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Appointment not found");
        }

        // ========== User Name Mapping Tests ==========

        @Test
        void mapToDTO_withUnknownUser_usesUnknownFallback() {
                // Arrange
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt));
                // Return empty list so user names are not found
                when(userAccountRepository.findAllById(any())).thenReturn(List.of());

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).brokerName()).isEqualTo("Unknown");
                assertThat(result.get(0).clientName()).isEqualTo("Unknown");
        }

        @Test
        void getFullName_withNullFirstName_returnsLastNameOnly() {
                // Arrange
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt));

                UserAccount brokerUser = createUserAccount(brokerId, null, "BrokerLastName");
                UserAccount clientUser = createUserAccount(clientId, "Jane", "Client");
                when(userAccountRepository.findAllById(any())).thenReturn(List.of(brokerUser, clientUser));

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result.get(0).brokerName()).isEqualTo("BrokerLastName");
        }

        @Test
        void getFullName_withNullLastName_returnsFirstNameOnly() {
                // Arrange
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt));

                UserAccount brokerUser = createUserAccount(brokerId, "John", null);
                UserAccount clientUser = createUserAccount(clientId, "Jane", "Client");
                when(userAccountRepository.findAllById(any())).thenReturn(List.of(brokerUser, clientUser));

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result.get(0).brokerName()).isEqualTo("John");
        }

        @Test
        void getFullName_withBothNamesNull_returnsEmptyString() {
                // Arrange
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt));

                UserAccount brokerUser = createUserAccount(brokerId, null, null);
                UserAccount clientUser = createUserAccount(clientId, "Jane", "Client");
                when(userAccountRepository.findAllById(any())).thenReturn(List.of(brokerUser, clientUser));

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result.get(0).brokerName()).isEmpty();
        }

        @Test
        void getUserNamesMap_withDuplicateUsers_handlesCollision() {
                // Arrange
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt));

                // Simulate repository returning duplicate users for the same ID (edge case)
                UserAccount user1 = createUserAccount(brokerId, "John", "Doe");
                UserAccount user2 = createUserAccount(brokerId, "John", "Doe"); // Duplicate
                when(userAccountRepository.findAllById(any())).thenReturn(List.of(user1, user2));

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).brokerName()).isEqualTo("John Doe");
        }

        // ========== DTO Mapping Tests ==========

        @Test
        void mapToDTO_mapsAllFields() {
                // Arrange
                Appointment apt = createTestAppointment();
                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                AppointmentResponseDTO dto = result.get(0);
                assertThat(dto.appointmentId()).isEqualTo(apt.getAppointmentId());
                assertThat(dto.title()).isEqualTo(apt.getTitle());
                assertThat(dto.transactionId()).isEqualTo(apt.getTransactionId());
                assertThat(dto.brokerId()).isEqualTo(apt.getBrokerId());
                assertThat(dto.clientId()).isEqualTo(apt.getClientId());
                assertThat(dto.fromDateTime()).isEqualTo(apt.getFromDateTime());
                assertThat(dto.toDateTime()).isEqualTo(apt.getToDateTime());
                assertThat(dto.status()).isEqualTo(apt.getStatus());
                assertThat(dto.initiatedBy()).isEqualTo(apt.getInitiatedBy());
                assertThat(dto.location()).isEqualTo(apt.getLocation());
                assertThat(dto.latitude()).isEqualTo(apt.getLatitude());
                assertThat(dto.longitude()).isEqualTo(apt.getLongitude());
                assertThat(dto.notes()).isEqualTo(apt.getNotes());
                assertThat(dto.refusalReason()).isEqualTo(apt.getRefusalReason());
                assertThat(dto.createdAt()).isEqualTo(apt.getCreatedAt());
                assertThat(dto.updatedAt()).isEqualTo(apt.getUpdatedAt());
        }

        @Test
        void getAppointmentsForBroker_withMultipleAppointments_returnsAll() {
                // Arrange
                Appointment apt1 = createTestAppointment();
                apt1.setTitle("Appointment 1");
                Appointment apt2 = createTestAppointment();
                apt2.setTitle("Appointment 2");
                apt2.setAppointmentId(UUID.randomUUID());

                when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                                .thenReturn(List.of(apt1, apt2));
                mockUserAccounts();

                // Act
                List<AppointmentResponseDTO> result = appointmentService.getAppointmentsForBroker(brokerId);

                // Assert
                assertThat(result).hasSize(2);
                assertThat(result.get(0).title()).isEqualTo("Appointment 1");
                assertThat(result.get(1).title()).isEqualTo("Appointment 2");
        }

        // ========== reviewAppointment Tests ==========

        @Test
        void reviewAppointment_confirm_updatesStatus() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);
                // apt is initiated by BROKER (default in createTestAppointment)

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArguments()[0]);
                mockUserAccounts();

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.CONFIRM,
                                null, null, null, null);

                // Act - Client reviews Broker's proposal
                AppointmentResponseDTO result = appointmentService.reviewAppointment(appointmentId, reviewDTO,
                                clientId);

                // Assert
                assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        }

        @Test
        void reviewAppointment_decline_updatesStatusAndReason() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArguments()[0]);
                mockUserAccounts();

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.DECLINE,
                                "Not available", null, null, null);

                // Act - Client reviews Broker's proposal
                AppointmentResponseDTO result = appointmentService.reviewAppointment(appointmentId, reviewDTO,
                                clientId);

                // Assert
                assertThat(result.status()).isEqualTo(AppointmentStatus.DECLINED);
                assertThat(result.refusalReason()).isEqualTo("Not available");
        }

        @Test
        void reviewAppointment_reschedule_updatesTimeAndResetsStatus() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);
                apt.setStatus(AppointmentStatus.PROPOSED);
                // apt initiated by BROKER

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArguments()[0]);
                mockUserAccounts();

                java.time.LocalDate newDate = java.time.LocalDate.now().plusDays(2);
                java.time.LocalTime newStart = java.time.LocalTime.of(14, 0);
                java.time.LocalTime newEnd = java.time.LocalTime.of(15, 0);

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE,
                                null, newDate, newStart, newEnd);

                // Act - Client attempts to reschedule (counter-offer)
                AppointmentResponseDTO result = appointmentService.reviewAppointment(appointmentId, reviewDTO,
                                clientId);

                // Assert
                assertThat(result.status()).isEqualTo(AppointmentStatus.PROPOSED);
                assertThat(result.fromDateTime()).isEqualTo(LocalDateTime.of(newDate, newStart));
                assertThat(result.toDateTime()).isEqualTo(LocalDateTime.of(newDate, newEnd));
                // Rescheduling means the CLIENT is now the initiator of this new proposal
                assertThat(result.initiatedBy()).isEqualTo(InitiatorType.CLIENT);
        }

        @Test
        void reviewAppointment_byInitiator_throwsForbidden() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);
                apt.setInitiatedBy(InitiatorType.BROKER); // Initiated by BROKER

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.CONFIRM,
                                null, null, null, null);

                // Act & Assert - Broker tries to review their own proposal
                assertThatThrownBy(() -> appointmentService.reviewAppointment(appointmentId, reviewDTO, brokerId))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You cannot review an appointment proposal you initiated");
        }

        @Test
        void reviewAppointment_whenNotProposed_throwsForbidden() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);
                apt.setStatus(AppointmentStatus.CONFIRMED); // Already confirmed

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.DECLINE,
                                "Changed mind", null, null, null);

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.reviewAppointment(appointmentId, reviewDTO, clientId))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("Appointment can only be reviewed while in PROPOSED status");
        }

        @Test
        void reviewAppointment_withInvalidRole_throwsForbidden() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.CONFIRM,
                                null, null, null, null);

                UUID strangerId = UUID.randomUUID();

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.reviewAppointment(appointmentId, reviewDTO, strangerId))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have permission to review this appointment");
        }

        @Test
        void reviewAppointment_decline_withoutReason_throwsException() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));
                mockUserAccounts();

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.DECLINE,
                                "", null, null, null); // Empty reason

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.reviewAppointment(appointmentId, reviewDTO, clientId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Refusal reason is required");
        }

        @Test
        void reviewAppointment_reschedule_withInvalidTime_throwsException() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment apt = createTestAppointment();
                apt.setAppointmentId(appointmentId);

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(apt));

                java.time.LocalDate newDate = java.time.LocalDate.now().plusDays(2);
                java.time.LocalTime newStart = java.time.LocalTime.of(15, 0);
                java.time.LocalTime newEnd = java.time.LocalTime.of(14, 0); // End before start

                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO(
                                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE,
                                null, newDate, newStart, newEnd);

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.reviewAppointment(appointmentId, reviewDTO, clientId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("New end time must be after start time");
        }
        // ========== requestAppointment Tests ==========

        @Test
        void requestAppointment_asBroker_createsAppointment() {
                // Arrange
                UUID requesterId = brokerId;
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId, "Property Viewing", null, java.time.LocalDate.now().plusDays(1),
                                java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "Let's view this");

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                        Appointment apt = invocation.getArgument(0);
                        apt.setAppointmentId(UUID.randomUUID());
                        return apt;
                });
                mockUserAccounts();

                // Act
                AppointmentResponseDTO result = appointmentService.requestAppointment(request, requesterId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.title()).isEqualTo("Property Viewing");
                assertThat(result.initiatedBy()).isEqualTo(InitiatorType.BROKER);
                assertThat(result.transactionId()).isEqualTo(transactionId);
                verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        void requestAppointment_asClient_createsAppointment() {
                // Arrange
                UUID requesterId = clientId;
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId, "Property Viewing", null, java.time.LocalDate.now().plusDays(1),
                                java.time.LocalTime.of(14, 0), java.time.LocalTime.of(15, 0), "Can we meet?");

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                        Appointment apt = invocation.getArgument(0);
                        apt.setAppointmentId(UUID.randomUUID());
                        return apt;
                });
                mockUserAccounts();

                // Act
                AppointmentResponseDTO result = appointmentService.requestAppointment(request, requesterId);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.initiatedBy()).isEqualTo(InitiatorType.CLIENT);
                verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        void requestAppointment_withOtherType_usesCustomTitle() {
                // Arrange
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId, "Other", "Custom Title", java.time.LocalDate.now().plusDays(1),
                                java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "Details");

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                        Appointment apt = invocation.getArgument(0);
                        apt.setAppointmentId(UUID.randomUUID());
                        return apt;
                });
                mockUserAccounts();

                // Act
                AppointmentResponseDTO result = appointmentService.requestAppointment(request, brokerId);

                // Assert
                assertThat(result.title()).isEqualTo("Custom Title");
        }

        @Test
        void requestAppointment_withOtherTypeButNoTitle_usesTypeAsTitle() {
                // Arrange
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId, "Other", "", java.time.LocalDate.now().plusDays(1), // empty title
                                java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "Details");

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                        Appointment apt = invocation.getArgument(0);
                        apt.setAppointmentId(UUID.randomUUID());
                        return apt;
                });
                mockUserAccounts();

                // Act
                AppointmentResponseDTO result = appointmentService.requestAppointment(request, brokerId);

                // Assert
                assertThat(result.title()).isEqualTo("Other");
        }

        @Test
        void requestAppointment_transactionNotFound_throwsNotFoundException() {
                // Arrange
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId, "Type", "Title", java.time.LocalDate.now(),
                                java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "Msg");

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.requestAppointment(request, brokerId))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessageContaining("Transaction not found");
        }

        @Test
        void requestAppointment_forbiddenUser_throwsForbiddenException() {
                // Arrange
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId, "Type", "Title", java.time.LocalDate.now().plusDays(1),
                                java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0), "Msg");

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

                // Act & Assert
                UUID otherUser = UUID.randomUUID();
                assertThatThrownBy(() -> appointmentService.requestAppointment(request, otherUser))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("You do not have permission");
        }

        @Test
        void requestAppointment_invalidTimeRange_throwsIllegalArgumentException() {
                // Arrange
                com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request = new com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO(
                                transactionId, "Type", "Title", java.time.LocalDate.now(),
                                java.time.LocalTime.of(12, 0), java.time.LocalTime.of(11, 0), "End before start");

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

                // Act & Assert
                assertThatThrownBy(() -> appointmentService.requestAppointment(request, brokerId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("End time must be after start time");
        }
}
