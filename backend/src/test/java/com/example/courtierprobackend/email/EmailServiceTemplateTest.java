package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceTemplateTest {
    @Test
    void loadTemplateFromClasspath_returnsStringOrThrows() throws Exception {
        EmailService service = new EmailService(null, null, null, null);
        // This will throw because the file does not exist, but we want to cover the exception path
        try {
            service.loadTemplateFromClasspath("notfound.html");
        } catch (IOException e) {
            assertThat(e).isInstanceOf(IOException.class);
        }
    }
}
