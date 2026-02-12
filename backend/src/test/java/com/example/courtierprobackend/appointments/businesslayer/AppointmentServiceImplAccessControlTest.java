package com.example.courtierprobackend.appointments.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.transactions.datalayer.TransactionParticipant;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AppointmentServiceImplAccessControlTest {
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private com.example.courtierprobackend.audit.appointment_audit.businesslayer.AppointmentAuditService appointmentAuditService;
    @Mock
    private TransactionParticipantRepository transactionParticipantRepository;

    private AppointmentServiceImpl appointmentService;

    private UUID transactionId;
    private UUID brokerId;
    private UUID clientId;
    private UUID coBrokerId;

    @Mock
    private com.example.courtierprobackend.email.EmailService emailService;
    @Mock
    private com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;
    @Mock
    private com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService timelineService;
    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.PropertyRepository propertyRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                userAccountRepository,
                transactionRepository,
                appointmentAuditService,
                emailService,
                timelineService,
                notificationService,
                transactionParticipantRepository,
                propertyRepository);
        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        coBrokerId = UUID.randomUUID();
    }

    @Test
    void getAppointmentsForClientByDateRange_filtersByCoBrokerAccess() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(UUID.randomUUID());
        apt.setTransactionId(transactionId);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(1);
        java.time.LocalDateTime to = java.time.LocalDateTime.now().plusDays(1);
        when(appointmentRepository.findByClientIdAndDateRange(clientId, from, to))
                .thenReturn(List.of(apt));
        TransactionParticipant coBroker = new TransactionParticipant();
        coBroker.setUserId(coBrokerId);
        coBroker.setTransactionId(transactionId);
        coBroker.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
        when(transactionParticipantRepository.findByTransactionId(transactionId)).thenReturn(List.of(coBroker));
        var result = appointmentService.getAppointmentsForClientByDateRange(clientId, from, to, coBrokerId, null);
        assertThat(result).hasSize(1);
    }

    @Test
    void getAppointmentsForClientByStatus_filtersByCoBrokerAccess() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(UUID.randomUUID());
        apt.setTransactionId(transactionId);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED;
        when(appointmentRepository.findByClientIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId, status))
                .thenReturn(List.of(apt));
        TransactionParticipant coBroker = new TransactionParticipant();
        coBroker.setUserId(coBrokerId);
        coBroker.setTransactionId(transactionId);
        coBroker.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
        when(transactionParticipantRepository.findByTransactionId(transactionId)).thenReturn(List.of(coBroker));
        var result = appointmentService.getAppointmentsForClientByStatus(clientId, status, coBrokerId, null);
        assertThat(result).hasSize(1);
    }

    @Test
    void getAppointmentsForClientByDateRangeAndStatus_filtersByCoBrokerAccess() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(UUID.randomUUID());
        apt.setTransactionId(transactionId);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(1);
        java.time.LocalDateTime to = java.time.LocalDateTime.now().plusDays(1);
        com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED;
        when(appointmentRepository.findByClientIdAndDateRangeAndStatus(clientId, from, to, status))
                .thenReturn(List.of(apt));
        TransactionParticipant coBroker = new TransactionParticipant();
        coBroker.setUserId(coBrokerId);
        coBroker.setTransactionId(transactionId);
        coBroker.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
        when(transactionParticipantRepository.findByTransactionId(transactionId)).thenReturn(List.of(coBroker));
        var result = appointmentService.getAppointmentsForClientByDateRangeAndStatus(clientId, from, to, status,
                coBrokerId, null);
        assertThat(result).hasSize(1);
    }
}
