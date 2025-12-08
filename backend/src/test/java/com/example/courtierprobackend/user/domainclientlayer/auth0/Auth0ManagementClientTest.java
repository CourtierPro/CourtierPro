package com.example.courtierprobackend.user.domainclientlayer.auth0;

import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for Auth0ManagementClient.
 * Tests simple utility methods (password generation, initialization).
 */
class Auth0ManagementClientTest {

    @Test
    void generateTemporaryPassword_hasUpperLowerDigitAndSymbol() {
        Auth0ManagementClient client = new Auth0ManagementClient(
                "example.auth0.com",
                "clientId",
                "clientSecret",
                "https://example.auth0.com/api/v2/",
                "http://localhost/result"
        );

        String password = (String) ReflectionTestUtils.invokeMethod(client, "generateTemporaryPassword");

        assertThat(password).containsPattern("[A-Z]");
        assertThat(password).containsPattern("[a-z]");
        assertThat(password).containsPattern("[0-9]");
        assertThat(password).contains("!");
        assertThat(password.length()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void assignRole_postsRoleForUser() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        Auth0ManagementClient client = new Auth0ManagementClient(
                "example.auth0.com",
                "clientId",
                "clientSecret",
                "https://example.auth0.com/api/v2/",
                "http://localhost/result"
        );
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        when(restTemplate.postForEntity(any(String.class), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        ReflectionTestUtils.invokeMethod(client, "assignRole", "token123", "auth0|user", UserRole.BROKER);
    }


}
