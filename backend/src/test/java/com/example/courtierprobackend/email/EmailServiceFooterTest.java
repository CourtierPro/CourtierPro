package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceFooterTest {
    @Test
    void getEmailFooter_returnsFooterHtml() {
        EmailService service = new EmailService(null, null, null, null);
        String footer = service.getEmailFooter();
        assertThat(footer).contains("<hr");
    }
}
