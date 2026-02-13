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

        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(List.of(broker));

        Appointment appt = new Appointment();
        appt.setTitle("Meeting");
        appt.setFromDateTime(LocalDateTime.now().plusDays(1));
        when(appointmentRepository.findByBrokerIdAndDateRange(any(), any(), any()))
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

        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(List.of(broker));

        when(appointmentRepository.findByBrokerIdAndDateRange(any(), any(), any()))
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
    void sendWeeklyDigests_OptOutPath_DisabledBroker() {
        // SCENARIO: The Opt-Out Path (Disabled Broker)
        // Arrange
        // The service layer relies on the repository to filter enabled brokers.
        // If the repository returns an empty list (meaning no brokers found with toggle = true),
        // the service should never attempt to aggregate or send.
        
        when(userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndActiveTrue(UserRole.BROKER))
                .thenReturn(new ArrayList<>());

        // Act
        weeklyDigestService.sendWeeklyDigests();

        // Assert
        verify(emailService, never()).sendWeeklyDigestEmail(any(), anyList(), anyList(), anyList());
        verify(appointmentRepository, never()).findByBrokerIdAndDateRange(any(), any(), any());
    }
}
