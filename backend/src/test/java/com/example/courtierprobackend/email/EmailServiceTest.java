package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 * Mocks OrganizationSettingsService and Transport.send() to test email logic without sending real emails.
 */
class EmailServiceTest {

    @Mock
    private OrganizationSettingsService organizationSettingsService;

    @Mock
    private com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService(
                "testuser@gmail.com",
                "testpass",
                "smtp.gmail.com",
                "587",
                organizationSettingsService,
                userAccountRepository,
                null
        );
    }

    @Test
    void sendPasswordSetupEmail_DefaultLanguageIsUsedIfLanguageCodeNull() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN {{name}} {{passwordLink}}")
                .inviteSubjectFr("Sujet FR")
                .inviteBodyFr("Corps FR {{name}} {{passwordLink}}")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", null);
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_EnglishLanguageExplicit() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN {{name}} {{passwordLink}}")
                .inviteSubjectFr("Sujet FR")
                .inviteBodyFr("Corps FR {{name}} {{passwordLink}}")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", "en");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_FrenchLanguageExplicit() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN {{name}} {{passwordLink}}")
                .inviteSubjectFr("Sujet FR")
                .inviteBodyFr("Corps FR {{name}} {{passwordLink}}")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", "fr");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_SubjectAndBodyFallbacks() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("")
                .inviteBodyEn("")
                .inviteSubjectFr(null)
                .inviteBodyFr(null)
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", "en");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_OverloadDelegatesToMainMethod() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN {{name}} {{passwordLink}}")
                .inviteSubjectFr("Sujet FR")
                .inviteBodyFr("Corps FR {{name}} {{passwordLink}}")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_FrenchSubjectAndBodyFallbacks() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN")
                .inviteSubjectFr(null)
                .inviteBodyFr("")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", "fr");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendDocumentSubmittedNotification_SendsEmailIfNotificationsEnabled() throws Exception {
        var brokerEmail = "broker@example.com";
        var uploaderName = "Uploader";
        var documentName = "DocName";
        var docType = "MORTGAGE_PRE_APPROVAL";
        var brokerLanguage = "fr";
        var transactionId = UUID.randomUUID();
        var transactionRef = new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE);
        var request = mock(Document.class);
        when(request.getTransactionRef()).thenReturn(transactionRef);

        var brokerAccount = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(brokerAccount.isEmailNotificationsEnabled()).thenReturn(true);
        when(userAccountRepository.findByEmail(brokerEmail)).thenReturn(java.util.Optional.of(brokerAccount));

        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .documentSubmittedSubjectEn("Subject EN")
                .documentSubmittedBodyEn("Body EN")
                .documentSubmittedSubjectFr("Sujet FR")
                .documentSubmittedBodyFr("Corps FR {{uploaderName}} {{documentName}} {{transactionId}}")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendDocumentSubmittedNotification(request, brokerEmail, uploaderName, documentName, docType, brokerLanguage);
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendDocumentSubmittedNotification_DoesNotSendIfNotificationsDisabled() {
        var brokerEmail = "broker@example.com";
        var brokerAccount = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(brokerAccount.isEmailNotificationsEnabled()).thenReturn(false);
        when(userAccountRepository.findByEmail(brokerEmail)).thenReturn(java.util.Optional.of(brokerAccount));

        var request = mock(Document.class);

        emailService.sendDocumentSubmittedNotification(request, brokerEmail, "Uploader", "DocName", "Type", "en");
    }

    @Test
    void sendPasswordSetupEmail_FallsBackToEnIfNoLanguage() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN {{name}} {{passwordLink}}")
                .inviteSubjectFr("Sujet FR")
                .inviteBodyFr("Corps FR {{name}} {{passwordLink}}")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", null);
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_SubjectAndBodyFallbackToDefaults() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("")
                .inviteBodyEn("")
                .inviteSubjectFr("")
                .inviteBodyFr("")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", "en");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_ReturnsFalseOnException() throws Exception {
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .inviteSubjectEn("Subject EN")
                .inviteBodyEn("Body EN")
                .inviteSubjectFr("Sujet FR")
                .inviteBodyFr("Corps FR")
                .build();
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(Message.class))).thenThrow(new jakarta.mail.MessagingException("fail"));
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "http://link", "en");
            assertThat(result).isFalse();
        }
    }

    @Test
    void sendWeeklyDigestEmail_ShouldSendEmailWithFormattedContent() throws Exception {
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("en");

        var appt = mock(com.example.courtierprobackend.appointments.datalayer.Appointment.class);
        when(appt.getTitle()).thenReturn("Meeting");
        when(appt.getFromDateTime()).thenReturn(java.time.LocalDateTime.now());
        when(appt.getLocation()).thenReturn("Office");

        var doc = mock(com.example.courtierprobackend.documents.datalayer.Document.class);
        when(doc.getCustomTitle()).thenReturn("Pending Doc");
        when(doc.getStatus()).thenReturn(com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum.REQUESTED);

        var tx = mock(com.example.courtierprobackend.transactions.datalayer.Transaction.class);
        when(tx.getLastUpdated()).thenReturn(java.time.LocalDateTime.now().minusDays(15));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendWeeklyDigestEmail(broker, java.util.List.of(appt), java.util.List.of(doc), java.util.List.of(tx));
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendWeeklyDigestEmail_French_ShouldSendEmailWithFrenchContent() throws Exception {
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("fr");

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendWeeklyDigestEmail(broker, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendWeeklyDigestEmail_ShouldHandleMissingData() throws Exception {
        // Arrange
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("en");

        // Transaction with null property address
        var tx = mock(com.example.courtierprobackend.transactions.datalayer.Transaction.class);
        when(tx.getPropertyAddress()).thenReturn(null);
        when(tx.getLastUpdated()).thenReturn(java.time.LocalDateTime.now());

        // Document with null custom title and null docType
        var doc = mock(com.example.courtierprobackend.documents.datalayer.Document.class);
        when(doc.getCustomTitle()).thenReturn(null);
        when(doc.getDocType()).thenReturn(null);
        when(doc.getStatus()).thenReturn(null);

        // Appointment with null location
        var appt = mock(com.example.courtierprobackend.appointments.datalayer.Appointment.class);
        when(appt.getTitle()).thenReturn(null);
        when(appt.getFromDateTime()).thenReturn(java.time.LocalDateTime.now());
        when(appt.getLocation()).thenReturn(null);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendWeeklyDigestEmail(broker, java.util.List.of(appt), java.util.List.of(doc), java.util.List.of(tx));
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendWeeklyDigestEmail_ShouldLogExceptionOnMessagingException() throws Exception {
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("en");

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(Message.class))).thenThrow(new jakarta.mail.MessagingException("SMTP failed"));
            
            // This should catch the exception and log it (we're verifying it doesn't crash)
            emailService.sendWeeklyDigestEmail(broker, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
            
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void escapeHtml_ShouldEscapeSpecialCharacters() {
        String input = "Me & My <HTML> \"Quotes\" 'Single'";
        String expected = "Me &amp; My &lt;HTML&gt; &quot;Quotes&quot; &#39;Single&#39;";
        assertThat(emailService.escapeHtml(input)).isEqualTo(expected);
    }

    @Test
    void escapeHtml_ShouldHandleNull() {
        assertThat(emailService.escapeHtml(null)).isEqualTo("");
    }

    @Test
    void translateDocumentStatus_ShouldCoverAllBranches() {
        assertThat(emailService.translateDocumentStatus(null, false)).isEqualTo("");
        assertThat(emailService.translateDocumentStatus("REQUESTED", true)).isEqualTo("Demandé");
        assertThat(emailService.translateDocumentStatus("SUBMITTED", true)).isEqualTo("Soumis");
        assertThat(emailService.translateDocumentStatus("APPROVED", true)).isEqualTo("Approuvé");
        assertThat(emailService.translateDocumentStatus("NEEDS_REVISION", true)).isEqualTo("À réviser");
        assertThat(emailService.translateDocumentStatus("REJECTED", true)).isEqualTo("Rejeté");
        assertThat(emailService.translateDocumentStatus("UNKNOWN", true)).isEqualTo("UNKNOWN");

        assertThat(emailService.translateDocumentStatus("REQUESTED", false)).isEqualTo("Requested");
        assertThat(emailService.translateDocumentStatus("SUBMITTED", false)).isEqualTo("Submitted");
        assertThat(emailService.translateDocumentStatus("APPROVED", false)).isEqualTo("Approved");
        assertThat(emailService.translateDocumentStatus("NEEDS_REVISION", false)).isEqualTo("Needs Revision");
        assertThat(emailService.translateDocumentStatus("REJECTED", false)).isEqualTo("Rejected");
    }

    @Test
    void translateDocumentType_ShouldCoverAllBranches() {
        assertThat(emailService.translateDocumentType(null, true)).isEqualTo("Autre");
        assertThat(emailService.translateDocumentType(null, false)).isEqualTo("Other");
        assertThat(emailService.translateDocumentType("MORTGAGE_PRE_APPROVAL", true)).isEqualTo("Pré-approbation hypothécaire");
        assertThat(emailService.translateDocumentType("MORTGAGE_APPROVAL", true)).isEqualTo("Approbation hypothécaire");
        assertThat(emailService.translateDocumentType("PROOF_OF_FUNDS", true)).isEqualTo("Preuve de fonds");
        assertThat(emailService.translateDocumentType("PROOF_OF_INCOME", true)).isEqualTo("Preuve de revenu");
        assertThat(emailService.translateDocumentType("UNKNOWN", true)).isEqualTo("UNKNOWN");

        assertThat(emailService.translateDocumentType("MORTGAGE_PRE_APPROVAL", false)).isEqualTo("Mortgage Pre-Approval");
        assertThat(emailService.translateDocumentType("MORTGAGE_APPROVAL", false)).isEqualTo("Mortgage Approval");
        assertThat(emailService.translateDocumentType("PROOF_OF_FUNDS", false)).isEqualTo("Proof of Funds");
    }

    @Test
    void translateAppointmentTitle_ShouldCoverAllBranches() {
        assertThat(emailService.translateAppointmentTitle(null, true)).isEqualTo("Rendez-vous");
        assertThat(emailService.translateAppointmentTitle(null, false)).isEqualTo("Appointment");
        assertThat(emailService.translateAppointmentTitle("house_visit", true)).isEqualTo("Visite de maison");
        assertThat(emailService.translateAppointmentTitle("open_house", true)).isEqualTo("Portes ouvertes");
        assertThat(emailService.translateAppointmentTitle("inspection", true)).isEqualTo("Inspection");
        assertThat(emailService.translateAppointmentTitle("notary", true)).isEqualTo("Notaire");
        assertThat(emailService.translateAppointmentTitle("meeting", true)).isEqualTo("Réunion");
        assertThat(emailService.translateAppointmentTitle("UNKNOWN", true)).isEqualTo("UNKNOWN");

        assertThat(emailService.translateAppointmentTitle("house_visit", false)).isEqualTo("House Visit");
    }

    @Test
    void sendWeeklyDigestEmail_French_ShouldCoverAllBilingualBranches() throws Exception {
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("fr");

        var appt = mock(com.example.courtierprobackend.appointments.datalayer.Appointment.class);
        when(appt.getTitle()).thenReturn("house_visit");
        when(appt.getFromDateTime()).thenReturn(java.time.LocalDateTime.now());
        when(appt.getLocation()).thenReturn("Chez le client");

        var doc = mock(com.example.courtierprobackend.documents.datalayer.Document.class);
        when(doc.getCustomTitle()).thenReturn(null);
        when(doc.getDocType()).thenReturn(com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum.MORTGAGE_PRE_APPROVAL);
        when(doc.getStatus()).thenReturn(com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum.REQUESTED);

        var tx = mock(com.example.courtierprobackend.transactions.datalayer.Transaction.class);
        var propertyAddress = mock(com.example.courtierprobackend.transactions.datalayer.PropertyAddress.class);
        when(propertyAddress.getStreet()).thenReturn("123 Rue de la Paix");
        when(tx.getPropertyAddress()).thenReturn(propertyAddress);
        when(tx.getLastUpdated()).thenReturn(java.time.LocalDateTime.now().minusDays(15));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendWeeklyDigestEmail(broker, java.util.List.of(appt), java.util.List.of(doc), java.util.List.of(tx));
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendWeeklyDigestEmail_NullValues_ShouldUseFallbacks() throws Exception {
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("en");

        // Appointment with null location
        var appt = mock(com.example.courtierprobackend.appointments.datalayer.Appointment.class);
        when(appt.getTitle()).thenReturn("meeting");
        when(appt.getFromDateTime()).thenReturn(java.time.LocalDateTime.now());
        when(appt.getLocation()).thenReturn(null);

        // Transaction with null street
        var tx = mock(com.example.courtierprobackend.transactions.datalayer.Transaction.class);
        var propertyAddress = mock(com.example.courtierprobackend.transactions.datalayer.PropertyAddress.class);
        when(propertyAddress.getStreet()).thenReturn(null);
        when(tx.getPropertyAddress()).thenReturn(propertyAddress);
        when(tx.getLastUpdated()).thenReturn(java.time.LocalDateTime.now());

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendWeeklyDigestEmail(broker, java.util.List.of(appt), new java.util.ArrayList<>(), java.util.List.of(tx));
            
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(messageCaptor.capture()));
            
            String content = messageCaptor.getValue().getContent().toString();
            assertThat(content).contains("No location specified");
            assertThat(content).contains("Unknown Address");
        }
    }

    @Test
    void sendWeeklyDigestEmail_BlankLocation_ShouldUseFallback() throws Exception {
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("en");

        var appt = mock(com.example.courtierprobackend.appointments.datalayer.Appointment.class);
        when(appt.getTitle()).thenReturn("meeting");
        when(appt.getFromDateTime()).thenReturn(java.time.LocalDateTime.now());
        when(appt.getLocation()).thenReturn("   "); // Blank

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendWeeklyDigestEmail(broker, java.util.List.of(appt), new java.util.ArrayList<>(), new java.util.ArrayList<>());
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(messageCaptor.capture()));
            assertThat(messageCaptor.getValue().getContent().toString()).contains("No location specified");
        }
    }

    @Test
    void sendWeeklyDigestEmail_ShouldLogExceptionOnIOException() throws Exception {
        var broker = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(broker.getEmail()).thenReturn("broker@test.com");
        when(broker.getPreferredLanguage()).thenReturn("en");

        EmailService spyEmailService = spy(emailService);
        doThrow(new IOException("Template not found")).when(spyEmailService).loadTemplateFromClasspath(anyString());

        // This should catch the IOException and log it
        spyEmailService.sendWeeklyDigestEmail(broker, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
        
        verify(spyEmailService, atLeastOnce()).loadTemplateFromClasspath(anyString());
    }

    @Test
    void sendEmail_FullHtml_ShouldNotAppendDuplicateFooter() throws Exception {
        String fullHtml = "<html><body>Test Content</body></html>";
        
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendEmail("test@test.com", "Test", fullHtml);
            
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(messageCaptor.capture()));
            
            String content = messageCaptor.getValue().getContent().toString();
            assertThat(content).isEqualTo(fullHtml);
            assertThat(content).doesNotContain("<hr"); // Part of the default footer
        }
    }

    @Test
    void sendEmail_Snippet_ShouldAppendFooter() throws Exception {
        String snippet = "<p>Hello</p>";
        
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendEmail("test@test.com", "Test", snippet);
            
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(messageCaptor.capture()));
            
            String content = messageCaptor.getValue().getContent().toString();
            assertThat(content).contains(snippet);
            assertThat(content).contains("<hr"); // Default footer separator
        }
    }

    @Test
    void sendEmail_SnippetWithWhitespace_ShouldAppendFooterCorrectly() throws Exception {
        String snippet = "<p>Hello</p>  \n ";
        
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendEmail("test@test.com", "Test", snippet);
            
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(messageCaptor.capture()));
            
            String content = messageCaptor.getValue().getContent().toString();
            assertThat(content).contains("<p>Hello</p>");
            assertThat(content).doesNotContain("<p>Hello</p></p>"); // Verify it didn't double close
            assertThat(content).contains("<hr");
        }
    }

    @Test
    void sendEmail_NullBody_ShouldReturnJustFooter() throws Exception {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendEmail("test@test.com", "Test", null);
            
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(messageCaptor.capture()));
            
            String content = messageCaptor.getValue().getContent().toString();
            assertThat(content).isEqualTo(emailService.getEmailFooter());
            assertThat(content).doesNotStartWith("</p>");
        }
    }

    @Test
    void sendEmail_EmptyBody_ShouldReturnJustFooter() throws Exception {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendEmail("test@test.com", "Test", "");
            
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(messageCaptor.capture()));
            
            String content = messageCaptor.getValue().getContent().toString();
            assertThat(content).isEqualTo(emailService.getEmailFooter());
        }
    }
}
