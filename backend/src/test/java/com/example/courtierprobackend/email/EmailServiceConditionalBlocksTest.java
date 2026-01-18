package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceConditionalBlocksTest {
    @Test
    void handleConditionalBlocks_includesOrExcludesContent() throws Exception {
        EmailService service = new EmailService(null, null, null, null);
        String text = "Hello [IF-brokerNotes]Notes: {{brokerNotes}}[/IF-brokerNotes] End.";
        Map<String, String> vars = Map.of("brokerNotes", "abc");
        String result = service.handleConditionalBlocks(text, vars);
        assertThat(result).contains("Notes: {{brokerNotes}}");
        result = service.handleConditionalBlocks(text, Map.of());
        assertThat(result).doesNotContain("Notes:");
    }
}
