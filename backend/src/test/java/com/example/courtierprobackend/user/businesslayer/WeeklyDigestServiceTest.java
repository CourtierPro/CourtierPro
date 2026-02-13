package com.example.courtierprobackend.user.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WeeklyDigestServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private WeeklyDigestService weeklyDigestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendWeeklyDigests_HappyPath_EnabledBrokerWithActivity() {
        // SCENARIO: The Happy Path (Enabled Broker)
        // Arrange
        UserAccount broker = new UserAccount();
        broker.setId(UUID.randomUUID());
        broker.setEmail("broker@test.com");
        broker.setRole(UserRole.BROKER);
        broker.setWeeklyDigestEnabled(true);
        broker.setPreferredLanguage("en");

        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndEmailNotificationsEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(List.of(broker));

        Appointment appt = new Appointment();
        appt.setTitle("Meeting");
        appt.setFromDateTime(LocalDateTime.now().plusDays(1));
        when(appointmentRepository.findByBrokerIdAndDateRangeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(List.of(appt));

        Document doc = new Document();
        doc.setCustomTitle("Pending Doc");
        when(documentRepository.findPendingDocumentsForWeeklyDigest(any()))
                .thenReturn(List.of(doc));

        Transaction tx = new Transaction();
        tx.setLastUpdated(LocalDateTime.now().minusDays(15));
        when(transactionRepository.findStalledTransactions(any(), any()))
                .thenReturn(List.of(tx));

        // Act
        weeklyDigestService.sendWeeklyDigests();

        // Assert
        verify(emailService, times(1)).sendWeeklyDigestEmail(eq(broker), anyList(), anyList(), anyList());
    }

    @Test
    void sendWeeklyDigests_EmptyStatePath_EnabledBrokerNoActivity() {
        // SCENARIO: The Empty State Path
        // Arrange
        UserAccount broker = new UserAccount();
        broker.setId(UUID.randomUUID());
        broker.setEmail("broker@test.com");
        broker.setRole(UserRole.BROKER);
        broker.setWeeklyDigestEnabled(true);

        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndEmailNotificationsEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(List.of(broker));

        when(appointmentRepository.findByBrokerIdAndDateRangeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        when(documentRepository.findPendingDocumentsForWeeklyDigest(any()))
                .thenReturn(new ArrayList<>());

        when(transactionRepository.findStalledTransactions(any(), any()))
                .thenReturn(new ArrayList<>());

        // Act
        weeklyDigestService.sendWeeklyDigests();

        // Assert
        // We skip the email entirely if no activity to avoid spam
        verify(emailService, never()).sendWeeklyDigestEmail(any(), anyList(), anyList(), anyList());
    }

    @Test
    void sendWeeklyDigests_ShouldCatchAndLogExceptionForIndividualBroker() {
        // Arrange
        UserAccount broker1 = new UserAccount();
        broker1.setId(UUID.randomUUID());
        broker1.setEmail("fail@test.com");
        broker1.setWeeklyDigestEnabled(true);

        UserAccount broker2 = new UserAccount();
        broker2.setId(UUID.randomUUID());
        broker2.setEmail("success@test.com");
        broker2.setWeeklyDigestEnabled(true);

        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndEmailNotificationsEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(List.of(broker1, broker2));

        // Make broker1 fail during data aggregation
        when(appointmentRepository.findByBrokerIdAndDateRangeAndStatusIn(eq(broker1.getId()), any(), any(), any()))
                .thenThrow(new RuntimeException("Aggregation failed"));

        // Make broker2 succeed with an appointment
        Appointment appt = new Appointment();
        appt.setTitle("Meeting");
        appt.setFromDateTime(LocalDateTime.now().plusDays(1));
        when(appointmentRepository.findByBrokerIdAndDateRangeAndStatusIn(eq(broker2.getId()), any(), any(), any()))
                .thenReturn(List.of(appt));
        when(documentRepository.findPendingDocumentsForWeeklyDigest(eq(broker2.getId())))
                .thenReturn(new ArrayList<>());
        when(transactionRepository.findStalledTransactions(eq(broker2.getId()), any()))
                .thenReturn(new ArrayList<>());

        // Act
        weeklyDigestService.sendWeeklyDigests();

        // Assert
        // Verify emailService was only called for broker2
        verify(emailService, times(1)).sendWeeklyDigestEmail(eq(broker2), anyList(), anyList(), anyList());
        verify(emailService, never()).sendWeeklyDigestEmail(eq(broker1), anyList(), anyList(), anyList());
    }

    @Test
    void sendWeeklyDigests_ShouldFilterCancelledAndDeclinedAppointments() {
        // Arrange
        UserAccount broker = new UserAccount();
        broker.setId(UUID.randomUUID());
        broker.setEmail("broker@test.com");
        broker.setWeeklyDigestEnabled(true);
        broker.setRole(UserRole.BROKER);

        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndEmailNotificationsEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(List.of(broker));

        // Mock returns empty list for the status-filtered query
        when(appointmentRepository.findByBrokerIdAndDateRangeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(documentRepository.findPendingDocumentsForWeeklyDigest(any()))
                .thenReturn(new ArrayList<>());
        when(transactionRepository.findStalledTransactions(any(), any()))
                .thenReturn(new ArrayList<>());

        // Act
        weeklyDigestService.sendWeeklyDigests();

        // Assert
        // Should not send email because all sections (after filtering) are empty
        verify(emailService, never()).sendWeeklyDigestEmail(any(), anyList(), anyList(), anyList());

        // Verify we actually passed the correct statuses to the repository
        verify(appointmentRepository).findByBrokerIdAndDateRangeAndStatusIn(
                eq(broker.getId()), any(), any(),
                argThat(statuses -> statuses.contains(com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED)
                        && statuses.contains(com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.PROPOSED)));
    }

    @Test
    void sendWeeklyDigests_ShouldNeverSendIfEmailNotificationsDisabled() {
        // SCENARIO: Master Email Notification flag is OFF
        // Although this is primarily handled by the repository query, 
        // confirming that the service correctly uses the filtered query.
        
        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndEmailNotificationsEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(new ArrayList<>()); // Repository returns none because flag is false

        // Act
        weeklyDigestService.sendWeeklyDigests();

        // Assert
        verify(emailService, never()).sendWeeklyDigestEmail(any(), anyList(), anyList(), anyList());
    }
}
