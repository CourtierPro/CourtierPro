package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private EmailService emailService;
    private OrganizationSettingsService organizationSettingsService;

    @BeforeEach
    void setup() {
        organizationSettingsService = mock(OrganizationSettingsService.class);

        OrganizationSettingsResponseModel settings =
                OrganizationSettingsResponseModel.builder()
                        .id(UUID.randomUUID())
                        .defaultLanguage("en")
                        .inviteSubjectEn("Welcome")
                        .inviteBodyEn("Hi {{name}}, welcome to CourtierPro.")
                        .inviteSubjectFr("Bienvenue")
                        .inviteBodyFr("Bonjour {{name}}, bienvenue sur CourtierPro.")
                        .updatedAt(Instant.now())
                        .build();

        when(organizationSettingsService.getSettings()).thenReturn(settings);

        emailService = new EmailService(
                "test@example.com",
                "dummy-password",
                organizationSettingsService
        );
    }

    @Test
    void sendPasswordSetupEmail_returnsTrue_whenTransportSucceeds() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = emailService.sendPasswordSetupEmail(
                    "user@example.com",
                    "https://example.com/password-setup"
            );

            assertTrue(result);
            transportMock.verify(() -> Transport.send(any(Message.class)));
        }
    }

    @Test
    void sendPasswordSetupEmail_returnsFalse_whenTransportThrowsException() throws MessagingException {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock
                    .when(() -> Transport.send(any(Message.class)))
                    .thenThrow(new MessagingException("SMTP error"));

            boolean result = emailService.sendPasswordSetupEmail(
                    "user@example.com",
                    "https://example.com/password-setup"
            );

            assertFalse(result);
            transportMock.verify(() -> Transport.send(any(Message.class)));
        }
    }
}
