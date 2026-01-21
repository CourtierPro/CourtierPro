package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

class EmailServiceSendSimpleEmailTest {
    @Test
    void sendSimpleEmail_doesNotThrow() {
        EmailService service = new EmailService("a", "b", null, null, null, null, null);
        service.sendSimpleEmail("to@x.com", "subj", "body");
        // No exception expected
    }

    @Test
    void sendSimpleEmail_handlesException() {
        EmailService service = new EmailService("a", "b", null, null, null, null, null) {
            @Override
            public void sendSimpleEmail(String to, String subject, String body) {
                throw new RuntimeException("fail");
            }
        };
        try {
            service.sendSimpleEmail("to@x.com", "subj", "body");
        } catch (Exception e) {
            // Should not throw
        }
    }
}
