package com.example.courtierprobackend.user.dataaccesslayer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

@DataJpaTest
class EmailChangeTokenRepositoryTest {
    @Autowired
    private EmailChangeTokenRepository repository;

    @Test
    void testSaveFindAndDeleteToken() {
        EmailChangeToken token = new EmailChangeToken();
        token.setUserId(UUID.randomUUID());
        token.setNewEmail("test@example.com");
        token.setToken("tok123");
        token.setExpiresAt(java.time.Instant.now().plusSeconds(600));
        token.setUsed(false);
        repository.save(token);

        // Find by token and used=false
        var found = repository.findByTokenAndUsedFalse("tok123");
        assertThat(found).isPresent();
        assertThat(found.get().getNewEmail()).isEqualTo("test@example.com");

        // Delete by userId
        repository.deleteByUserId(token.getUserId());
        var afterDelete = repository.findByTokenAndUsedFalse("tok123");
        assertThat(afterDelete).isEmpty();
    }
}
