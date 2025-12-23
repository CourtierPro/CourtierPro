package com.example.courtierprobackend.notifications.datalayer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
class BroadcastAuditRepositoryTest {

    @Autowired
    private BroadcastAuditRepository broadcastAuditRepository;

    @Test
    void saveAndFind_shouldPersistBroadcastAudit() {
        // Arrange
        UUID id = UUID.randomUUID();
        BroadcastAudit audit = BroadcastAudit.builder()
                .id(id)
                .adminId("auth0|admin")
                .title("Test Broadcast")
                .message("Test Message")
                .sentAt(LocalDateTime.now())
                .recipientCount(10)
                .build();

        // Act
        broadcastAuditRepository.save(audit);

        // Assert
        BroadcastAudit found = broadcastAuditRepository.findById(id).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getAdminId()).isEqualTo("auth0|admin");
        assertThat(found.getTitle()).isEqualTo("Test Broadcast");
        assertThat(found.getMessage()).isEqualTo("Test Message");
        assertThat(found.getRecipientCount()).isEqualTo(10);
    }
}
