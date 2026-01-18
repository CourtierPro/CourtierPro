package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailServiceSendEmailTest {
    @Test
    void sendEmail_sendsSuccessfully() throws Exception {
        EmailService service = new EmailService("a", "b", null, null);
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            boolean result = service.sendEmail("to@x.com", "subj", "body");
            assertThat(result).isTrue();
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }

    @Test
    void sendEmail_throwsMessagingException() throws Exception {
        EmailService service = new EmailService("a", "b", null, null);
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(Message.class))).thenThrow(new jakarta.mail.MessagingException("fail"));
            try {
                service.sendEmail("to@x.com", "subj", "body");
            } catch (jakarta.mail.MessagingException e) {
                assertThat(e).hasMessageContaining("fail");
            }
        }
    }
}
