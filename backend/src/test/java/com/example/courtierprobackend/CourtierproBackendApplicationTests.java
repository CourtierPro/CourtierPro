package com.example.courtierprobackend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Disabled for now – full context test requires full config/env (Auth0, DB, etc.)")
@SpringBootTest
class CourtierproBackendApplicationTests {

    @Test
    void contextLoads() {
        // no-op
    }
}