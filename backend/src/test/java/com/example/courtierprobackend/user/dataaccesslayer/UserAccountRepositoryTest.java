package com.example.courtierprobackend.user.dataaccesslayer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class UserAccountRepositoryTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void findByActiveTrue_shouldReturnOnlyActiveUsers() {
        // Arrange
        UserAccount activeUser1 = new UserAccount("auth0|1", "user1@example.com", "First1", "Last1", UserRole.CLIENT,
                "en");
        UserAccount activeUser2 = new UserAccount("auth0|2", "user2@example.com", "First2", "Last2", UserRole.CLIENT,
                "fr");

        UserAccount inactiveUser = new UserAccount("auth0|3", "user3@example.com", "First3", "Last3", UserRole.CLIENT,
                "en");
        inactiveUser.setActive(false);

        userAccountRepository.save(activeUser1);
        userAccountRepository.save(activeUser2);
        userAccountRepository.save(inactiveUser);

        // Act
        List<UserAccount> activeUsers = userAccountRepository.findByActiveTrue();

        // Assert
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting(UserAccount::getAuth0UserId)
                .containsExactlyInAnyOrder("auth0|1", "auth0|2");
        assertThat(activeUsers).extracting(UserAccount::isActive)
                .containsOnly(true);
    }
}
