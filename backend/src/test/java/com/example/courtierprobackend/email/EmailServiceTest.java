package com.example.courtierprobackend.email;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class EmailServiceTest {

    private EmailService emailService;

    @BeforeEach
    void setup() {
        // no Spring – we just pass dummy values
        emailService = new EmailService("test@example.com", "dummy-password");
    }

    @Test
    void sendPasswordSetupEmail_returnsTrue_whenTransportSucceeds() {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Default behavior: do nothing (no exception) when send is called

            boolean result = emailService.sendPasswordSetupEmail(
                    "user@example.com",
                    "https://example.com/password-setup"
            );

            assertTrue(result);
            // Verify we attempted to send an email
            transportMock.verify(() -> Transport.send(any(Message.class)));
        }
    }

    @Test
    void sendPasswordSetupEmail_returnsFalse_whenTransportThrowsException() throws MessagingException {
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            // Force Transport.send to fail
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
