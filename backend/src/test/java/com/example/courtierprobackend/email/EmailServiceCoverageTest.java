package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceCoverageTest {

    @Mock private OrganizationSettingsService organizationSettingsService;
    @Mock private UserAccountRepository userAccountRepository;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(
                "user", "pass", "host", "587",
                organizationSettingsService,
                userAccountRepository,
                null
        );
        // Inject fromAddress used in SES path, though SMTP uses gmailUsername.
        // Doing this to be safe and consistent.
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@courtierpro.com");

        // Default settings
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder().build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);
    }

    // --- translateDocumentType ---

    @Test
    void translateDocumentType_coversAllCases() {
        assertThat(emailService.translateDocumentType("MORTGAGE_PRE_APPROVAL", false)).isEqualTo("Mortgage Pre-Approval");
        assertThat(emailService.translateDocumentType("MORTGAGE_PRE_APPROVAL", true)).isEqualTo("Pré-approbation hypothécaire");
        
        assertThat(emailService.translateDocumentType(null, false)).isEqualTo("Other");
        assertThat(emailService.translateDocumentType(null, true)).isEqualTo("Autre");

        assertThat(emailService.translateDocumentType("UNKNOWN_TYPE", false)).isEqualTo("UNKNOWN_TYPE");
    }

    // --- translateAppointmentTitle ---

    @Test
    void translateAppointmentTitle_coversAllCases() {
        assertThat(emailService.translateAppointmentTitle("house_visit", false)).isEqualTo("House Visit");
        assertThat(emailService.translateAppointmentTitle("house_visit", true)).isEqualTo("Visite de maison");

        assertThat(emailService.translateAppointmentTitle(null, false)).isEqualTo("Appointment");
        assertThat(emailService.translateAppointmentTitle(null, true)).isEqualTo("Rendez-vous");

        assertThat(emailService.translateAppointmentTitle("Unknown", false)).isEqualTo("Unknown");
    }

    // --- sendDocumentSubmittedNotification ---

    @Test
    void sendDocumentSubmittedNotification_nullDocTypeAndName_fallsBack() {
        String brokerEmail = "broker@test.com";
        UserAccount broker = new UserAccount();
        broker.setEmailNotificationsEnabled(true);
        when(userAccountRepository.findByEmail(brokerEmail)).thenReturn(Optional.of(broker));

        Document doc = mock(Document.class);
        when(doc.getTransactionRef()).thenReturn(new TransactionRef(UUID.randomUUID(), null, TransactionSide.BUY_SIDE));

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            emailService.sendDocumentSubmittedNotification(doc, brokerEmail, "Uploader", null, null, "en");
            
            transport.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendDocumentSubmittedNotification_messagingException_logsError() {
        String brokerEmail = "broker@test.com";
        UserAccount broker = new UserAccount();
        broker.setEmailNotificationsEnabled(true);
        when(userAccountRepository.findByEmail(brokerEmail)).thenReturn(Optional.of(broker));

        Document doc = mock(Document.class);
        when(doc.getTransactionRef()).thenReturn(new TransactionRef(UUID.randomUUID(), null, TransactionSide.BUY_SIDE));

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            transport.when(() -> Transport.send(any(Message.class))).thenThrow(new MessagingException("Fail"));
            
            // Should not throw
            emailService.sendDocumentSubmittedNotification(doc, brokerEmail, "Uploader", "Doc", "Type", "en");
        }
    }

    // --- sendDocumentRequestedNotification ---

    @Test
    void sendDocumentRequestedNotification_signatureRequired_nullBody_usesTemplate() {
        String clientEmail = "client@test.com";
        UserAccount client = new UserAccount();
        client.setEmailNotificationsEnabled(true);
        when(userAccountRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));

        // Settings with null body
        when(organizationSettingsService.getSettings()).thenReturn(OrganizationSettingsResponseModel.builder().build());

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            emailService.sendDocumentRequestedNotification(clientEmail, "Name", "Broker", "Doc", "Type", "Note", "en", true);
            
            transport.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendDocumentRequestedNotification_noSignature_nullBody_usesTemplate() {
        String clientEmail = "client@test.com";
        UserAccount client = new UserAccount();
        client.setEmailNotificationsEnabled(true);
        when(userAccountRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));

        // Settings with null body
        when(organizationSettingsService.getSettings()).thenReturn(OrganizationSettingsResponseModel.builder().build());

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            emailService.sendDocumentRequestedNotification(clientEmail, "Name", "Broker", "Doc", "Type", "Note", "en", false);
            
            transport.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    // --- sendAppointmentConfirmedNotification ---

    @Test
    void sendAppointmentConfirmedNotification_sendsEmail() {
        String clientEmail = "client@test.com";
        UserAccount client = new UserAccount();
        client.setEmailNotificationsEnabled(true);
        when(userAccountRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));

        Appointment appt = new Appointment();
        appt.setTitle("house_visit");
        appt.setFromDateTime(LocalDateTime.now());
        
        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            emailService.sendAppointmentConfirmedNotification(appt, clientEmail, "Client", "en");
            
            transport.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    // --- sendAppointmentStatusUpdateNotification ---

    @Test
    void sendAppointmentStatusUpdateNotification_sendsEmail() {
        String clientEmail = "client@test.com";
        UserAccount client = new UserAccount();
        client.setEmailNotificationsEnabled(true);
        when(userAccountRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));

        Appointment appt = new Appointment();
        appt.setTitle("house_visit");
        appt.setFromDateTime(LocalDateTime.now());

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            emailService.sendAppointmentStatusUpdateNotification(appt, clientEmail, "Client", "fr", "CANCELLED", "Reason");

            transport.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    // --- sendAppointmentReminderNotification ---

    @Test
    void sendAppointmentReminderNotification_sendsEmail() {
        String clientEmail = "client@test.com";
        UserAccount client = new UserAccount();
        client.setEmailNotificationsEnabled(true);
        when(userAccountRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));

        Appointment appt = new Appointment();
        appt.setTitle("house_visit");
        appt.setFromDateTime(LocalDateTime.now());

        try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
            emailService.sendAppointmentReminderNotification(appt, clientEmail, "Client", "en");

            transport.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }
}
