package com.example.courtierprobackend.appointments.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentCancellationDTO;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;
import com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceNotificationTest {

        @Mock
        private AppointmentRepository appointmentRepository;

        @Mock
        private UserAccountRepository userAccountRepository;

        @Mock
        private TransactionRepository transactionRepository;

        @Mock
        private com.example.courtierprobackend.audit.appointment_audit.businesslayer.AppointmentAuditService appointmentAuditService;

        @Mock
        private com.example.courtierprobackend.email.EmailService emailService;

        @Mock
        private com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;

        private AppointmentServiceImpl appointmentService;

        private UUID brokerId;
        private UUID clientId;
        private UUID transactionId;
        private UserAccount broker;
        private UserAccount client;

        @Mock
        private com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository transactionParticipantRepository; // Added
                                                                                                                                                      // mock

        @Mock
        private com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService timelineService;

        @BeforeEach
        void setUp() {
                appointmentService = new AppointmentServiceImpl(appointmentRepository, userAccountRepository,
                                transactionRepository, appointmentAuditService, emailService, timelineService,
                                notificationService,
                                transactionParticipantRepository);

                brokerId = UUID.randomUUID();
                clientId = UUID.randomUUID();
                transactionId = UUID.randomUUID();

                broker = new UserAccount();
                broker.setId(brokerId);
                broker.setFirstName("John");
                broker.setLastName("Broker");
                broker.setEmail("broker@example.com");
                broker.setAuth0UserId("auth0|broker");

                client = new UserAccount();
                client.setId(clientId);
                client.setFirstName("Jane");
                client.setLastName("Client");
                client.setEmail("client@example.com");
                client.setAuth0UserId("auth0|client");

                // Common Mocks
                when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));
                when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));
                // Mock finding multiple IDs too if needed
                when(userAccountRepository.findAllById(any())).thenReturn(List.of(broker, client));
        }

        @Test
        void createAppointment_shouldTriggerNotification() {
                // Arrange
                AppointmentRequestDTO request = new AppointmentRequestDTO(
                                transactionId, "Viewing", "Notes", LocalDate.now().plusDays(1),
                                LocalTime.of(10, 0), LocalTime.of(11, 0), "Message");

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transactionId);
                transaction.setBrokerId(brokerId);
                transaction.setClientId(clientId);

                when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> {
                        Appointment a = i.getArgument(0);
                        a.setAppointmentId(UUID.randomUUID());
                        return a;
                });

                // Act - Broker requests appointment
                appointmentService.requestAppointment(request, brokerId);

                // Assert
                // Since Broker requested, Client should receive notification
                verify(notificationService).createNotification(
                                eq(clientId.toString()), // Using UUID string as per recent fix
                                eq("APPOINTMENT_REQUESTED"),
                                eq("NEW_APPOINTMENT_REQUEST"),
                                any(Map.class),
                                anyString(),
                                eq(NotificationCategory.APPOINTMENT));

                verify(emailService).sendAppointmentRequestedNotification(
                                any(Appointment.class),
                                eq(client.getEmail()),
                                eq("Jane Client"),
                                eq("en"));
        }

        @Test
        void reviewAppointment_confirm_shouldTriggerNotification() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = new Appointment();
                appointment.setAppointmentId(appointmentId);
                appointment.setTransactionId(transactionId);
                appointment.setBrokerId(brokerId);
                appointment.setClientId(clientId);
                appointment.setStatus(AppointmentStatus.PROPOSED);
                appointment.setInitiatedBy(InitiatorType.BROKER); // Initiated by Broker

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(appointment));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

                AppointmentReviewDTO reviewDTO = new AppointmentReviewDTO(
                                AppointmentReviewDTO.ReviewAction.CONFIRM, null, null, null, null);

                // Act - Client confirms
                appointmentService.reviewAppointment(appointmentId, reviewDTO, clientId);

                // Assert
                // Notifications go to Broker (Initiator)
                verify(notificationService).createNotification(
                                eq(brokerId.toString()),
                                eq("APPOINTMENT_CONFIRMED"),
                                eq("APPOINTMENT_HAS_BEEN_CONFIRMED"),
                                any(Map.class),
                                anyString(),
                                eq(NotificationCategory.APPOINTMENT));

                verify(emailService).sendAppointmentConfirmedNotification(
                                any(Appointment.class),
                                eq(broker.getEmail()),
                                eq("John Broker"),
                                eq("en"));
        }

        @Test
        void reviewAppointment_decline_shouldTriggerNotification() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = new Appointment();
                appointment.setAppointmentId(appointmentId);
                appointment.setTransactionId(transactionId);
                appointment.setBrokerId(brokerId);
                appointment.setClientId(clientId);
                appointment.setStatus(AppointmentStatus.PROPOSED);
                appointment.setInitiatedBy(InitiatorType.BROKER);

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(appointment));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

                AppointmentReviewDTO reviewDTO = new AppointmentReviewDTO(
                                AppointmentReviewDTO.ReviewAction.DECLINE, "Busy", null, null, null);

                // Act - Client declines
                appointmentService.reviewAppointment(appointmentId, reviewDTO, clientId);

                // Assert
                verify(notificationService).createNotification(
                                eq(brokerId.toString()),
                                eq("APPOINTMENT_DECLINED"),
                                eq("APPOINTMENT_HAS_BEEN_DECLINED"),
                                any(Map.class),
                                anyString(),
                                eq(NotificationCategory.APPOINTMENT));

                verify(emailService).sendAppointmentStatusUpdateNotification(
                                any(Appointment.class),
                                eq(broker.getEmail()),
                                eq("John Broker"),
                                eq("en"),
                                eq("DECLINED"),
                                eq("Busy"));
        }

        @Test
        void cancelAppointment_shouldTriggerNotification() {
                // Arrange
                UUID appointmentId = UUID.randomUUID();
                Appointment appointment = new Appointment();
                appointment.setAppointmentId(appointmentId);
                appointment.setTransactionId(transactionId);
                appointment.setBrokerId(brokerId);
                appointment.setClientId(clientId);
                appointment.setStatus(AppointmentStatus.CONFIRMED); // Confirmed appointment
                appointment.setInitiatedBy(InitiatorType.BROKER);

                when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId))
                                .thenReturn(Optional.of(appointment));
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

                AppointmentCancellationDTO cancelDTO = new AppointmentCancellationDTO("Emergencies");

                // Act - Broker cancels
                appointmentService.cancelAppointment(appointmentId, cancelDTO, brokerId);

                // Assert
                // Client gets notified
                verify(notificationService).createNotification(
                                eq(clientId.toString()),
                                eq("APPOINTMENT_CANCELLED"),
                                eq("APPOINTMENT_HAS_BEEN_CANCELLED"),
                                any(Map.class),
                                anyString(),
                                eq(NotificationCategory.APPOINTMENT));

                verify(emailService).sendAppointmentStatusUpdateNotification(
                                any(Appointment.class),
                                eq(client.getEmail()),
                                eq("Jane Client"),
                                eq("en"),
                                eq("CANCELLED"),
                                eq("Emergencies"));
        }

        @Test
        void sendAppointmentReminders_shouldSendEmails_ForUpcomingAppointments() {
                // Arrange
                Appointment upcoming = new Appointment();
                upcoming.setAppointmentId(UUID.randomUUID());
                upcoming.setBrokerId(brokerId);
                upcoming.setClientId(clientId);
                upcoming.setFromDateTime(LocalDateTime.now().plusHours(24).plusMinutes(30));
                upcoming.setReminderSent(false);
                upcoming.setStatus(AppointmentStatus.CONFIRMED);

                // Mock returns list with one appointment
                when(appointmentRepository
                                .findByFromDateTimeBetweenAndReminderSentFalseAndStatusNotInAndDeletedAtIsNull(
                                                any(LocalDateTime.class), any(LocalDateTime.class), any()))
                                .thenReturn(List.of(upcoming));

                // Capture saved appointment
                when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

                // Act
                appointmentService.sendAppointmentReminders();

                // Assert
                // Verify emails sent to both
                verify(emailService).sendAppointmentReminderNotification(upcoming, broker.getEmail(), "John Broker",
                                "en");
                verify(emailService).sendAppointmentReminderNotification(upcoming, client.getEmail(), "Jane Client",
                                "en");

                // Verify notifications sent to both
                verify(notificationService).createNotification(
                                eq(brokerId.toString()), eq("APPOINTMENT_REMINDER"), anyString(), any(), anyString(),
                                eq(NotificationCategory.APPOINTMENT));
                verify(notificationService).createNotification(
                                eq(clientId.toString()), eq("APPOINTMENT_REMINDER"), anyString(), any(), anyString(),
                                eq(NotificationCategory.APPOINTMENT));

                // Verify reminderSent flag update
                verify(appointmentRepository).saveAll(anyList());
        }

        @Test
        void sendAppointmentReminders_shouldNotSend_IfAlreadySent() {
                // Arrange
                // Mock returns empty list (simulating query filtering out already sent
                // reminders)
                when(appointmentRepository
                                .findByFromDateTimeBetweenAndReminderSentFalseAndStatusNotInAndDeletedAtIsNull(
                                                any(LocalDateTime.class), any(LocalDateTime.class), any()))
                                .thenReturn(Collections.emptyList());

                // Act
                appointmentService.sendAppointmentReminders();

                // Assert
                verifyNoInteractions(emailService);
                verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), any(),
                                anyString(), any());
        }
}
