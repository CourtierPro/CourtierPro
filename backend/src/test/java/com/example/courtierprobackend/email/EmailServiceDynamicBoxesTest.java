package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceDynamicBoxesTest {
    @Test
    void handleDynamicBoxes_appliesColorStyles() throws Exception {
        EmailService service = new EmailService(null, null, null, null);
        String text = "[BOX-blue]Blue![/BOX-blue][BOX-RED]Red![/BOX-RED][BOX-green]Green![/BOX-green]";
        String result = service.handleDynamicBoxes(text);
        assertThat(result).contains("border-left: 4px solid #3b82f6");
        assertThat(result).contains("border-left: 4px solid #ef4444");
        assertThat(result).contains("border-left: 4px solid #22c55e");
    }
}
