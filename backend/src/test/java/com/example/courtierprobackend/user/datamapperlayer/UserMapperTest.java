package com.example.courtierprobackend.user.datamapperlayer;

import com.example.courtierprobackend.user.mapper.UserMapper;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.presentationlayer.request.CreateUserRequest;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for UserMapper.
 * Tests mapping logic between request/response and entity layers.
 */
class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toNewUserEntity_mapsAllFields() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .role("BROKER")
                .preferredLanguage("fr")
                .build();

        UserAccount entity = mapper.toNewUserEntity(request, "auth0|123");

        assertThat(entity.getAuth0UserId()).isEqualTo("auth0|123");
        assertThat(entity.getEmail()).isEqualTo("user@test.com");
        assertThat(entity.getFirstName()).isEqualTo("John");
        assertThat(entity.getLastName()).isEqualTo("Doe");
        assertThat(entity.getRole()).isEqualTo(UserRole.BROKER);
        assertThat(entity.getPreferredLanguage()).isEqualTo("fr");
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    void toResponse_mapsAllFields() {
        UserAccount account = new UserAccount(
                "auth0|999",
                "broker@test.com",
                "Jane",
                "Smith",
                UserRole.ADMIN,
                "en"
        );
        account.setActive(false);

        UserResponse response = mapper.toResponse(account);

        assertThat(response.getId()).isEqualTo(account.getId() != null ? account.getId().toString() : null);
        assertThat(response.getEmail()).isEqualTo("broker@test.com");
        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Smith");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getPreferredLanguage()).isEqualTo("en");
        assertThat(response.isActive()).isFalse();
    }
}
