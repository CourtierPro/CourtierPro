package com.example.courtierprobackend.email;

import org.springframework.test.util.ReflectionTestUtils;

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
    void sendPasswordSetupEmail_WithNullSettingsSubjectAndBody_UsesHardcodedDefaults() {
        // Arrange
        OrganizationSettingsResponseModel settings = createSettings("en", null, null, null, null);
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "https://reset.url", "en");

            // Assert
            assertThat(result).isTrue();
            
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(captor.capture()), times(1));
            Message message = captor.getValue();
            
            // Allow generic unchecked exception for getSubject/getContent
            try {
                assertThat(message.getSubject()).isEqualTo("CourtierPro Invitation");
                String body = (String) ((MimeMessage) message).getContent();
                assertThat(body).contains("Hi user@example.com, your CourtierPro account has been created.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
            emailService.sendDocumentSubmittedNotification(request, "broker@example.com", "John Doe", "Tax Return", "PAY_STUBS", "en");

            // Assert
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendDocumentRequestedNotification_SendsEmail() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Act
            emailService.sendDocumentRequestedNotification("client@example.com", "Client Name", "Broker Name", "ID Document", "ID_VERIFICATION", "en");

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
                        "Proof of Income",
                        "PAY_STUBS",
                        "en"
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
                        "Bank Statement",
                        "BANK_STATEMENT",
                        "en"
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
    
    @Test
    void sendStageUpdateEmail_sendsEmail() {
        when(organizationSettingsService.getSettings()).thenReturn(createSettings("en", "Stage Update", "Stage body", null, null));
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            ReflectionTestUtils.invokeMethod(emailService, "sendStageUpdateEmail", "user@example.com", "Client", "Broker", "123 Main St", "OFFER", "en");
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void translateDocumentType_returnsTranslation() {
        String result = ReflectionTestUtils.invokeMethod(emailService, "translateDocumentType", "PAY_STUBS", true);
        assertThat(result).isNotBlank();
        String result2 = ReflectionTestUtils.invokeMethod(emailService, "translateDocumentType", "PAY_STUBS", false);
        assertThat(result2).isNotBlank();
    }

    @Test
    void escapeHtml_escapesTags() {
        String input = "<b>bold</b> & <script>";
        String result = ReflectionTestUtils.invokeMethod(emailService, "escapeHtml", input);
        assertThat(result).contains("&lt;b&gt;bold&lt;/b&gt;");
        assertThat(result).contains("&lt;script&gt;");
    }

    @Test
    void sendEmail_returnsTrue() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = ReflectionTestUtils.invokeMethod(emailService, "sendEmail", "to@mail.com", "subject", "body");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void loadTemplateFromClasspath_returnsContent() {
        String content = ReflectionTestUtils.invokeMethod(emailService, "loadTemplateFromClasspath", "email-templates/password-setup-en.html");
        assertThat(content).isNotNull();
    }
    
    @Test
    void sendPasswordSetupEmail_MessagingException_ReturnsFalse() {
        OrganizationSettingsResponseModel settings = createSettings("en", "S", "B", null, null);
        when(organizationSettingsService.getSettings()).thenReturn(settings);

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));
            
            boolean result = emailService.sendPasswordSetupEmail("user@example.com", "url", "en");
            
            assertThat(result).isFalse();
        }
    }

    @Test
    void sendDocumentSubmittedNotification_MessagingException_LogsError() {
        DocumentRequest request = new DocumentRequest();
        request.setTransactionRef(new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));
        request.setCustomTitle("Doc");

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));
            
            // Should not throw
            emailService.sendDocumentSubmittedNotification(request, "broker@mail.com", "User", "Doc", "TYPE", "en");
            
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendDocumentRequestedNotification_MessagingException_LogsError() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));
            
            // Should not throw
            emailService.sendDocumentRequestedNotification("client@mail.com", "Client", "Broker", "Doc", "TYPE", "en");
            
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendDocumentStatusUpdatedNotification_MessagingException_LogsError() {
        DocumentRequest request = new DocumentRequest();
        request.setStatus(DocumentStatusEnum.APPROVED);
        request.setCustomTitle("Doc");
        request.setTransactionRef(new TransactionRef(UUID.randomUUID(), UUID.randomUUID(), TransactionSide.BUY_SIDE));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));
            
            // Should not throw
            emailService.sendDocumentStatusUpdatedNotification(request, "client@mail.com", "Broker", "Doc", "TYPE", "en");
            
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendStageUpdateEmail_MessagingException_LogsError() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));
            
            // Should not throw
            emailService.sendStageUpdateEmail("client@mail.com", "Client", "Broker", "Addr", "OFFER", "en");
            
            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }
    
    @Test
    void translateDocumentType_UnknownType_ReturnsOriginal() {
        String result = ReflectionTestUtils.invokeMethod(emailService, "translateDocumentType", "UNKNOWN_TYPE", true);
        assertThat(result).isEqualTo("UNKNOWN_TYPE");
    }

    // ==================== PROPERTY OFFER EMAIL TESTS ====================

    @Test
    void sendPropertyOfferMadeNotification_SendsEmail() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendPropertyOfferMadeNotification(
                    "client@example.com",
                    "John Client",
                    "Jane Broker",
                    "123 Main St",
                    "$500,000",
                    1,
                    "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendPropertyOfferMadeNotification_French_UsesFrenchTemplate() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendPropertyOfferMadeNotification(
                    "client@example.com",
                    "Jean Client",
                    "Marie Courtier",
                    "123 Rue Principale",
                    "$500,000",
                    1,
                    "fr");

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(captor.capture()), times(1));
            Message message = captor.getValue();
            try {
                assertThat(message.getSubject()).contains("Offre soumise");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void sendPropertyOfferMadeNotification_MessagingException_LogsError() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));

            // Should not throw
            emailService.sendPropertyOfferMadeNotification(
                    "client@example.com", "Client", "Broker", "Address", "$100", 1, "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendPropertyOfferStatusChangedNotification_SendsEmail() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendPropertyOfferStatusChangedNotification(
                    "client@example.com",
                    "John Client",
                    "Jane Broker",
                    "123 Main St",
                    "OFFER_MADE",
                    "COUNTERED",
                    "Please consider $480,000",
                    "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendPropertyOfferStatusChangedNotification_MessagingException_LogsError() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));

            // Should not throw
            emailService.sendPropertyOfferStatusChangedNotification(
                    "client@example.com", "Client", "Broker", "Address",
                    "OFFER_MADE", "ACCEPTED", null, "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    // ==================== OFFER EMAIL TESTS (SELL-SIDE) ====================

    @Test
    void sendOfferReceivedNotification_SendsEmail() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendOfferReceivedNotification(
                    "seller@example.com",
                    "John Seller",
                    "Jane Broker",
                    "Bob Buyer",
                    "$450,000",
                    "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendOfferReceivedNotification_French_UsesFrenchTemplate() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendOfferReceivedNotification(
                    "seller@example.com",
                    "Jean Vendeur",
                    "Marie Courtier",
                    "Pierre Acheteur",
                    "$450,000",
                    "fr");

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            transportMock.verify(() -> Transport.send(captor.capture()), times(1));
            Message message = captor.getValue();
            try {
                assertThat(message.getSubject()).contains("Nouvelle offre");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void sendOfferReceivedNotification_MessagingException_LogsError() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));

            // Should not throw
            emailService.sendOfferReceivedNotification(
                    "seller@example.com", "Seller", "Broker", "Buyer", "$100", "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendOfferStatusChangedNotification_SendsEmail() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            emailService.sendOfferStatusChangedNotification(
                    "seller@example.com",
                    "John Seller",
                    "Jane Broker",
                    "Bob Buyer",
                    "PENDING",
                    "ACCEPTED",
                    "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    void sendOfferStatusChangedNotification_MessagingException_LogsError() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any())).thenThrow(new jakarta.mail.MessagingException("Fail"));

            // Should not throw
            emailService.sendOfferStatusChangedNotification(
                    "seller@example.com", "Seller", "Broker", "Buyer",
                    "PENDING", "DECLINED", "en");

            transportMock.verify(() -> Transport.send(any()), times(1));
        }
    }

    // ==================== OFFER STATUS TRANSLATION TESTS ====================

    @Test
    void translateOfferStatus_ReturnsTranslation() {
        String result = ReflectionTestUtils.invokeMethod(emailService, "translateOfferStatus", "ACCEPTED", false);
        assertThat(result).isEqualTo("Accepted");

        String resultFr = ReflectionTestUtils.invokeMethod(emailService, "translateOfferStatus", "ACCEPTED", true);
        assertThat(resultFr).isEqualTo("Acceptée");
    }

    @Test
    void translateOfferStatus_UnknownStatus_ReturnsOriginal() {
        String result = ReflectionTestUtils.invokeMethod(emailService, "translateOfferStatus", "UNKNOWN_STATUS", true);
        assertThat(result).isEqualTo("UNKNOWN_STATUS");
    }

    @Test
    void translateOfferStatus_NullStatus_ReturnsEmptyString() {
        String result = ReflectionTestUtils.invokeMethod(emailService, "translateOfferStatus", (String) null, false);
        assertThat(result).isEqualTo("");
    }
}

