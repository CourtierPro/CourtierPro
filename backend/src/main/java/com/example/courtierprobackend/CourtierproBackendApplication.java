package com.example.courtierprobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class CourtierproBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourtierproBackendApplication.class, args);
    }

}
