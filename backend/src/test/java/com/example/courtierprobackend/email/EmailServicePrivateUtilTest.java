package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServicePrivateUtilTest {
    @Test
    void escapeHtml_escapesSpecialCharacters() {
        EmailService service = new EmailService(null, null, null, null);
        assertThat(service.escapeHtml("<b>&'\"test")).isEqualTo("&lt;b&gt;&amp;&#39;&quot;test");
        assertThat(service.escapeHtml(null)).isEqualTo("");
    }

    @Test
    void convertPlainTextToHtml_wrapsAndEscapes() {
        EmailService service = new EmailService(null, null, null, null);
        String input = "Hello\n\nWorld <b>!";
        String html = service.convertPlainTextToHtml(input);
        assertThat(html).contains("<div").contains("<p>").contains("Hello").contains("World").contains("&lt;b&gt;!");
    }
}
