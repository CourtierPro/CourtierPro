package com.example.courtierprobackend.appointments.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentCancellationDTO;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;
import com.example.courtierprobackend.audit.appointment_audit.businesslayer.AppointmentAuditService;
import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.transactions.datalayer.Property;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.repositories.PropertyRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceCoverageTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AppointmentAuditService appointmentAuditService;
    @Mock private EmailService emailService;
    @Mock private TimelineService timelineService;
    @Mock private NotificationService notificationService;
    @Mock private TransactionParticipantRepository transactionParticipantRepository;
    @Mock private PropertyRepository propertyRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private UUID appointmentId = UUID.randomUUID();
    private UUID transactionId = UUID.randomUUID();
    private UUID brokerId = UUID.randomUUID();
    private UUID clientId = UUID.randomUUID();
    private UUID otherId = UUID.randomUUID();
    private UUID propertyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
    }

    // --- Access Control / Filter Logic ---

    @Test
    void getAppointmentsForClient_filtersUnauthorized() {
        Appointment authorized = new Appointment();
        authorized.setAppointmentId(UUID.randomUUID());
        authorized.setClientId(clientId);
        authorized.setBrokerId(brokerId);
        // Requester is broker -> authorized

        Appointment unauthorized = new Appointment();
        unauthorized.setAppointmentId(UUID.randomUUID());
        unauthorized.setClientId(clientId);
        unauthorized.setBrokerId(otherId);
        // Requester is broker -> NOT authorized for other broker's apt (unless shared via transaction)

        when(appointmentRepository.findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId))
                .thenReturn(List.of(authorized, unauthorized));
        
        when(transactionParticipantRepository.findByTransactionId(any())).thenReturn(new ArrayList<>());

        var result = appointmentService.getAppointmentsForClient(clientId, brokerId, "broker@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).appointmentId()).isEqualTo(authorized.getAppointmentId());
    }

    // --- House Visit Validation ---

    @Test
    void requestAppointment_houseVisit_propertyNotFound_throws() {
        AppointmentRequestDTO request = new AppointmentRequestDTO(
                transactionId,
                "house_visit",
                "Visit",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Note",
                propertyId
        );

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setClientId(clientId);
        tx.setSide(TransactionSide.BUY_SIDE);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.requestAppointment(request, brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Property not found");
    }

    @Test
    void requestAppointment_houseVisit_wrongTransaction_throws() {
        AppointmentRequestDTO request = new AppointmentRequestDTO(
                transactionId,
                "house_visit",
                "Visit",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Note",
                propertyId
        );

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setClientId(clientId);
        tx.setSide(TransactionSide.BUY_SIDE);

        Property property = new Property();
        property.setPropertyId(propertyId);
        property.setTransactionId(UUID.randomUUID()); // Different transaction

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));

        assertThatThrownBy(() -> appointmentService.requestAppointment(request, brokerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property does not belong to this transaction");
    }

    // --- Timeline Failure Resilience ---

    @Test
    void requestAppointment_timelineFailure_doesNotThrow() {
        AppointmentRequestDTO request = new AppointmentRequestDTO(
                transactionId,
                "meeting",
                "Meeting",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Note",
                null
        );

        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setClientId(clientId);

        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            a.setAppointmentId(appointmentId);
            a.setFromDateTime(LocalDateTime.of(request.date(), request.startTime()));
            return a;
        });

        // Mock timeline failure
        doThrow(new RuntimeException("Timeline DB down"))
                .when(timelineService).addEntry(any(), any(), any(), any(), any(), any());

        appointmentService.requestAppointment(request, brokerId);

        // Verification: should complete successfully
        verify(appointmentRepository).save(any());
        verify(appointmentAuditService).logAction(any(), eq("CREATED"), any(), any());
    }

    // --- Review Appointment (Cancellation/Reschedule) ---

    @Test
    void reviewAppointment_cancelledNonReschedule_throws() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(appointmentId);
        apt.setStatus(AppointmentStatus.CANCELLED);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        apt.setInitiatedBy(InitiatorType.CLIENT);

        when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId)).thenReturn(Optional.of(apt));

        AppointmentReviewDTO review = new AppointmentReviewDTO(
                AppointmentReviewDTO.ReviewAction.CONFIRM, // Not RESCHEDULE
                null, null, null, null
        );

        assertThatThrownBy(() -> appointmentService.reviewAppointment(appointmentId, review, brokerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cancelled or Declined appointments can only be rescheduled");
    }

    @Test
    void reviewAppointment_cancelledByOther_throws() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(appointmentId);
        apt.setStatus(AppointmentStatus.CANCELLED);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        apt.setInitiatedBy(InitiatorType.BROKER);
        apt.setCancelledBy(clientId); // Cancelled by client

        when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId)).thenReturn(Optional.of(apt));

        AppointmentReviewDTO review = new AppointmentReviewDTO(
                AppointmentReviewDTO.ReviewAction.RESCHEDULE,
                null, LocalDate.now().plusDays(2), LocalTime.of(10, 0), LocalTime.of(11, 0)
        );

        // Reviewer is broker (not canceller)
        assertThatThrownBy(() -> appointmentService.reviewAppointment(appointmentId, review, brokerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only the party who cancelled");
    }
    
    @Test
    void reviewAppointment_timelineFailure_doesNotThrow() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(appointmentId);
        apt.setStatus(AppointmentStatus.PROPOSED);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        apt.setTransactionId(transactionId);
        apt.setInitiatedBy(InitiatorType.CLIENT);

        when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any())).thenReturn(apt);

        AppointmentReviewDTO review = new AppointmentReviewDTO(
                AppointmentReviewDTO.ReviewAction.CONFIRM,
                null, null, null, null
        );
        
        doThrow(new RuntimeException("Timeline DB down"))
            .when(timelineService).addEntry(any(), any(), any(), any(), any(), any());

        appointmentService.reviewAppointment(appointmentId, review, brokerId);
        
        verify(appointmentRepository).save(any());
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    // --- Cancel Appointment ---

    @Test
    void cancelAppointment_sendsNotification() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(appointmentId);
        apt.setStatus(AppointmentStatus.CONFIRMED);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        apt.setTransactionId(transactionId);

        when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any())).thenReturn(apt);
        
        // Mock recipient
        UserAccount client = new UserAccount();
        client.setId(clientId);
        client.setEmail("client@example.com");
        when(userAccountRepository.findById(clientId)).thenReturn(Optional.of(client));

        AppointmentCancellationDTO cancel = new AppointmentCancellationDTO("Changed mind");

        appointmentService.cancelAppointment(appointmentId, cancel, brokerId);

        verify(emailService).sendAppointmentStatusUpdateNotification(eq(apt), eq("client@example.com"), any(), any(), eq("CANCELLED"), eq("Changed mind"));
        verify(notificationService).createNotification(eq(clientId.toString()), any(), any(), any(), any(), any());
    }

    @Test
    void cancelAppointment_timelineFailure_doesNotThrow() {
        Appointment apt = new Appointment();
        apt.setAppointmentId(appointmentId);
        apt.setStatus(AppointmentStatus.CONFIRMED);
        apt.setBrokerId(brokerId);
        apt.setClientId(clientId);
        apt.setTransactionId(transactionId);

        when(appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any())).thenReturn(apt);
        
        doThrow(new RuntimeException("Timeline DB down"))
            .when(timelineService).addEntry(any(), any(), any(), any(), any(), any());

        AppointmentCancellationDTO cancel = new AppointmentCancellationDTO("Changed mind");

        appointmentService.cancelAppointment(appointmentId, cancel, brokerId);
        
        verify(appointmentRepository).save(any());
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }
}
