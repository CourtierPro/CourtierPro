package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceHighlightColorsTest {
    @Test
    void handleHighlightColors_appliesHighlightStyles() throws Exception {
        EmailService service = new EmailService(null, null, null, null, null, null, null);
        String text = "[HIGHLIGHT-yellow]Yellow![/HIGHLIGHT-yellow][HIGHLIGHT-pink]Pink![/HIGHLIGHT-pink]";
        String result = service.handleHighlightColors(text);
        assertThat(result).contains("background-color: #fef08a");
        assertThat(result).contains("background-color: #fbcfe8");
    }
}
