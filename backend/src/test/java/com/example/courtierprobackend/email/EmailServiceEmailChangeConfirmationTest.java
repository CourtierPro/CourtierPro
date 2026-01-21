package com.example.courtierprobackend.email;

import jakarta.mail.Transport;
import jakarta.mail.Message;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceEmailChangeConfirmationTest {

    @Test
    void sendEmailChangeConfirmation_withFirstName() {
        EmailService service = spy(new EmailService("a", "b", null, null, null, null, null));
        var user = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(user.getFirstName()).thenReturn("John");
        doNothing().when(service).sendSimpleEmail(anyString(), anyString(), anyString());
        
        service.sendEmailChangeConfirmation(user, "to@x.com", "token123");
        verify(service).sendSimpleEmail(contains("to@x.com"), contains("Confirm your new email address"), contains("token123"));
    }

    @Test
    void sendEmailChangeConfirmation_withNullFirstName() {
        // Line 649 - firstName null fallback to "User"
        EmailService service = spy(new EmailService("a", "b", null, null, null, null, null));
        var user = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(user.getFirstName()).thenReturn(null);
        doNothing().when(service).sendSimpleEmail(anyString(), anyString(), anyString());
        
        service.sendEmailChangeConfirmation(user, "to@x.com", "token123");
        verify(service).sendSimpleEmail(eq("to@x.com"), anyString(), contains("Hello User"));
    }

    @Test
    void sendSimpleEmail_success() {
        // Lines 675, 678
        EmailService service = new EmailService("test@gmail.com", "pass", null, null, null, null, null);
        
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            service.sendSimpleEmail("to@x.com", "Subject", "Body");
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendSimpleEmail_exceptionHandling() {
        // Line 678 - exception handling in catch block
        EmailService service = new EmailService("test@gmail.com", "pass", null, null, null, null, null);
        
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(Message.class))).thenThrow(new jakarta.mail.MessagingException("fail"));
            // Should not throw - just logs
            service.sendSimpleEmail("to@x.com", "Subject", "Body");
        }
    }

    @Test
    void convertPlainTextToHtml_blankInput() {
        // Line 683 - blank input returns empty string
        EmailService service = new EmailService(null, null, null, null, null, null, null);
        
        assertThat(service.convertPlainTextToHtml(null)).isEmpty();
        assertThat(service.convertPlainTextToHtml("")).isEmpty();
        assertThat(service.convertPlainTextToHtml("   ")).isEmpty();
    }
}
