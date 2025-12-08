package com.example.courtierprobackend.config;

import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Seeds the database with test data for local development.
 * Only runs in the "dev" profile.
 */
@Configuration
@Profile("dev")
public class DevDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    @Bean
    CommandLineRunner seedTestClients(UserAccountRepository repository) {
        return args -> {
            // Always ensure the real Auth0 client account exists (for end-to-end testing)
            String realClientAuth0Id = "auth0|692d326406d8f1855384d114";
            if (repository.findByAuth0UserId(realClientAuth0Id).isEmpty()) {
                log.info("Seeding real Auth0 client account for end-to-end testing...");
                repository.save(new UserAccount(
                        realClientAuth0Id, "client.test@example.com",
                        "Test", "Client", UserRole.CLIENT, "en"));
                log.info("Seeded real Auth0 client: client.test@example.com");
            }

            // Seed fake test clients only if none exist (first run)
            if (repository.findByRole(UserRole.CLIENT).size() <= 1) {
                log.info("Seeding additional test clients for development...");

                saveIfNotExists(repository, "auth0|client001", "marie.dupont@example.com",
                        "Marie", "Dupont", UserRole.CLIENT, "fr");
                saveIfNotExists(repository, "auth0|client002", "jean.tremblay@example.com",
                        "Jean", "Tremblay", UserRole.CLIENT, "fr");
                saveIfNotExists(repository, "auth0|client003", "sophie.lavoie@example.com",
                        "Sophie", "Lavoie", UserRole.CLIENT, "fr");
                saveIfNotExists(repository, "auth0|client004", "michael.smith@example.com",
                        "Michael", "Smith", UserRole.CLIENT, "en");
                saveIfNotExists(repository, "auth0|client005", "sarah.johnson@example.com",
                        "Sarah", "Johnson", UserRole.CLIENT, "en");
                saveIfNotExists(repository, "auth0|client006", "pierre.gagnon@example.com",
                        "Pierre", "Gagnon", UserRole.CLIENT, "fr");

                log.info("Seeded additional test clients.");
            }

            // Seed a test broker if none exist
            if (repository.findByRole(UserRole.BROKER).isEmpty()) {
                log.info("Seeding test broker for development...");
                repository.save(new UserAccount(
                        "auth0|broker001", "broker.test@example.com",
                        "Test", "Broker", UserRole.BROKER, "fr"));
                log.info("Seeded 1 test broker.");
            }
        };
    }

    private void saveIfNotExists(UserAccountRepository repository, String auth0Id, String email,
                                  String firstName, String lastName, UserRole role, String lang) {
        if (repository.findByAuth0UserId(auth0Id).isEmpty()) {
            repository.save(new UserAccount(auth0Id, email, firstName, lastName, role, lang));
        }
    }
}
