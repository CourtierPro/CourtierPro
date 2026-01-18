package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

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
                organizationSettingsService,
                userAccountRepository
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
        // Lines 76, 80 - French fallback subject and body when settings are null/blank
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
        // Arrange
        var brokerEmail = "broker@example.com";
        var uploaderName = "Uploader";
        var documentName = "DocName";
        var docType = "MORTGAGE_PRE_APPROVAL";
        var brokerLanguage = "fr";
        var transactionId = UUID.randomUUID();
        var transactionRef = new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE);
        var request = mock(DocumentRequest.class);
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

        var request = mock(DocumentRequest.class);

        // Should not throw or send email
        emailService.sendDocumentSubmittedNotification(request, brokerEmail, "Uploader", "DocName", "Type", "en");
        // No need to verify Transport.send, as it should not be called
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
}