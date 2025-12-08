package com.example.courtierprobackend.config;

import com.example.courtierprobackend.user.businesslayer.Auth0UserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Syncs Auth0 users to the local database on application startup.
 * Ensures local database stays in sync with Auth0 as the source of truth.
 */
@Configuration
public class Auth0UserSyncConfig {

    private static final Logger log = LoggerFactory.getLogger(Auth0UserSyncConfig.class);

    @Bean
    CommandLineRunner syncAuth0Users(Auth0UserSyncService syncService) {
        return args -> {
            log.info("Syncing users from Auth0...");
            syncService.syncUsersFromAuth0();
        };
    }
}
