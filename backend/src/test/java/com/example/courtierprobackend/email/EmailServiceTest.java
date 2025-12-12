package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 * Mocks OrganizationSettingsService and Transport.send() to test email logic without sending real emails.
 */
class EmailServiceTest {

    @Mock
    private OrganizationSettingsService organizationSettingsService;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService("test@gmail.com", "password", organizationSettingsService);
    }

    private OrganizationSettingsResponseModel createSettings(String lang, String subjectEn, String bodyEn, String subjectFr, String bodyFr) {
        return OrganizationSettingsResponseModel.builder()
                .defaultLanguage(lang)
                .inviteSubjectEn(subjectEn)
                .inviteBodyEn(bodyEn)
                .inviteSubjectFr(subjectFr)
                .inviteBodyFr(bodyFr)
                .build();
    }

    @Test
    void sendPasswordSetupEmail_WithEnglish_UsesEnglishTemplate() {
        // Arrange
        OrganizationSettingsResponseModel settings = createSettings("en", "Welcome", "Hello {{name}}", null, null);
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "https://reset.url", "en");

            // Assert
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_WithFrench_UsesFrenchTemplate() {
        // Arrange
        OrganizationSettingsResponseModel settings = createSettings("fr", null, null, "Bienvenue", "Bonjour {{name}}");
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "https://reset.url", "fr");

            // Assert
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendPasswordSetupEmail_WithNullLanguage_FallsBackToDefault() {
        // Arrange
        OrganizationSettingsResponseModel settings = createSettings("en", "Welcome", "Hello", null, null);
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "https://reset.url", null);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Test
    void sendPasswordSetupEmail_Overload_DelegatesToMain() {
        // Arrange
        OrganizationSettingsResponseModel settings = createSettings("en", "Welcome", "Hello", null, null);
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "https://reset.url");

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Test
    void sendDocumentSubmittedNotification_SendsEmail() {
        // Arrange
        DocumentRequest request = new DocumentRequest();
        TransactionRef txRef = new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE);
        request.setTransactionRef(txRef);
        request.setCustomTitle("Tax Return");

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            emailService.sendDocumentSubmittedNotification(request, "broker@example.com", "John Doe", "Tax Return");

            // Assert
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendDocumentRequestedNotification_SendsEmail() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            emailService.sendDocumentRequestedNotification("client@example.com", "Client Name", "Broker Name", "ID Document");

            // Assert
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendDocumentStatusUpdatedNotification_SendsEmail() {
        // Arrange
        DocumentRequest request = new DocumentRequest();
        request.setCustomTitle("Proof of Income");
        request.setStatus(DocumentStatusEnum.APPROVED);
        request.setTransactionRef(new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendDocumentStatusUpdatedNotification(
                    request,
                    "client@example.com",
                    "Broker Name",
                    "Proof of Income"
            );

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(captor.capture()), times(1));
            Message message = captor.getValue();
            assertThat(message.getSubject()).contains("Document");
            String body = (String) ((MimeMessage) message).getContent();
            assertThat(body).contains("approved the following document:");
            assertThat(body).contains("Proof of Income");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendDocumentStatusUpdatedNotification_Revision_UsesRevisionStatusLine() {
        DocumentRequest request = new DocumentRequest();
        request.setCustomTitle("Bank Statement");
        request.setStatus(DocumentStatusEnum.NEEDS_REVISION);
        request.setTransactionRef(new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));
        request.setBrokerNotes("Please resend the last 3 months.");

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendDocumentStatusUpdatedNotification(
                    request,
                    "client@example.com",
                    "Jane Broker",
                    "Bank Statement"
            );

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(captor.capture()), times(1));
            Message message = captor.getValue();
            String body = (String) ((MimeMessage) message).getContent();
            assertThat(body).contains("requested a revision for the following document:");
            assertThat(body).contains("Bank Statement");
            assertThat(body).contains("Please resend the last 3 months.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

