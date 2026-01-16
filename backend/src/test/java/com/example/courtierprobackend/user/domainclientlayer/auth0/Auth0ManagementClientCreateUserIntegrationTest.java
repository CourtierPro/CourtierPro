package com.example.courtierprobackend.user.domainclientlayer.auth0;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Auth0ManagementClient HTTP interactions.
 * Tests the full flow of createUser including token retrieval, user creation,
 * role assignment, and password change ticket generation.
 *
 * Now that TokenResponse is public, we can properly mock RestTemplate responses
 * and test the complete HTTP call chain.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Auth0ManagementClientCreateUserIntegrationTest {

    @Mock
    private RestTemplate restTemplate;

    private Auth0ManagementClient client;

    @BeforeEach
    void setup() {
        client = new Auth0ManagementClient(
                "test.auth0.com",
                "test-client-id",
                "test-client-secret",
                "https://test.auth0.com/api/v2/"
        );
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
    }

    private void setupSuccessfulMocks(String userId, String ticketUrl) {
        // Mock token
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/oauth/token"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.TokenResponse.class)
        )).thenReturn(ResponseEntity.ok(new Auth0ManagementClient.TokenResponse("token")));

        // Mock user creation
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/api/v2/users"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.Auth0UserResponse.class)
        )).thenReturn(ResponseEntity.ok(new Auth0ManagementClient.Auth0UserResponse(userId)));

        // Mock role assignment
        when(restTemplate.postForEntity(
                contains("/roles/"),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        // Mock password ticket
        if (ticketUrl != null) {
            when(restTemplate.postForEntity(
                    eq("https://test.auth0.com/api/v2/tickets/password-change"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(ResponseEntity.ok(Map.of("ticket", ticketUrl)));
        } else {
            when(restTemplate.postForEntity(
                    eq("https://test.auth0.com/api/v2/tickets/password-change"),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenThrow(new RuntimeException("Ticket service unavailable"));
        }
    }

    @Test
    void createUser_successfulFlow_returnsUserIdWithPasswordTicket() {
        setupSuccessfulMocks("auth0|user123", "https://test.auth0.com/reset?ticket=abc123");

        String result = client.createUser("test@example.com", "John", "Doe", UserRole.BROKER, "en");

        assertThat(result).isEqualTo("auth0|user123");
    }

    @Test
    void createUser_withFrenchLanguage_storesPreferredLanguage() {
        // Setup mocks
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/oauth/token"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.TokenResponse.class)
        )).thenReturn(ResponseEntity.ok(new Auth0ManagementClient.TokenResponse("token")));

        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/api/v2/users"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.Auth0UserResponse.class)
        )).thenReturn(ResponseEntity.ok(new Auth0ManagementClient.Auth0UserResponse("auth0|fr-user")));

        when(restTemplate.postForEntity(
                contains("/roles/"),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/api/v2/tickets/password-change"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(Map.of("ticket", "https://test.auth0.com/ticket")));

        client.createUser("french@example.com", "Pierre", "Dupont", UserRole.CLIENT, "fr");

        // Verify user creation request contains French language in metadata
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeast(2)).postForEntity(anyString(), entityCaptor.capture(), any(Class.class));

        // Find the user creation request (contains user_metadata)
        boolean foundFrenchMetadata = entityCaptor.getAllValues().stream()
                .map(HttpEntity::getBody)
                .filter(body -> body != null && body.containsKey("user_metadata"))
                .anyMatch(body -> {
                    Map<String, Object> metadata = (Map<String, Object>) body.get("user_metadata");
                    return "fr".equals(metadata.get("preferred_language"));
                });

        assertThat(foundFrenchMetadata).isTrue();
    }

    @Test
    void createUser_withNullLanguage_defaultsToEnglish() {
        setupSuccessfulMocks("auth0|en-user", "https://test.auth0.com/ticket");

        client.createUser("default@example.com", "Default", "User", UserRole.ADMIN, null);

        // Verify user creation request defaults to English
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeast(2)).postForEntity(anyString(), entityCaptor.capture(), any(Class.class));

        boolean foundEnglishMetadata = entityCaptor.getAllValues().stream()
                .map(HttpEntity::getBody)
                .filter(body -> body != null && body.containsKey("user_metadata"))
                .anyMatch(body -> {
                    Map<String, Object> metadata = (Map<String, Object>) body.get("user_metadata");
                    return "en".equals(metadata.get("preferred_language"));
                });

        assertThat(foundEnglishMetadata).isTrue();
    }

    @Test
    void createUser_assignsCorrectRoleForAdmin() {
        setupSuccessfulMocks("auth0|admin", "https://test.auth0.com/ticket");

        client.createUser("admin@example.com", "Admin", "User", UserRole.ADMIN, "en");

        // Verify role assignment URL contains admin role ID
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate, atLeast(3)).postForEntity(urlCaptor.capture(), any(HttpEntity.class), any(Class.class));

        boolean foundAdminRoleUrl = urlCaptor.getAllValues().stream()
                .anyMatch(url -> url.contains("/roles/rol_2zQ5SYaHM3eDsUF3/users")); // Admin role ID

        assertThat(foundAdminRoleUrl).isTrue();
    }

    @Test
    void createUser_assignsCorrectRoleForBroker() {
        setupSuccessfulMocks("auth0|broker", "https://test.auth0.com/ticket");

        client.createUser("broker@example.com", "Broker", "User", UserRole.BROKER, "en");

        // Verify role assignment URL contains broker role ID
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate, atLeast(3)).postForEntity(urlCaptor.capture(), any(HttpEntity.class), any(Class.class));

        boolean foundBrokerRoleUrl = urlCaptor.getAllValues().stream()
                .anyMatch(url -> url.contains("/roles/rol_l9MqshX9J77aopLk/users")); // Broker role ID

        assertThat(foundBrokerRoleUrl).isTrue();
    }

    @Test
    void createUser_setsEmailVerifiedTrue() {
        setupSuccessfulMocks("auth0|verified", "https://test.auth0.com/ticket");

        client.createUser("verified@example.com", "Verified", "User", UserRole.CLIENT, "en");

        // Verify user creation body contains email_verified: true
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeast(2)).postForEntity(anyString(), entityCaptor.capture(), any(Class.class));

        boolean foundEmailVerified = entityCaptor.getAllValues().stream()
                .map(HttpEntity::getBody)
                .filter(body -> body != null && body.containsKey("email_verified"))
                .anyMatch(body -> Boolean.TRUE.equals(body.get("email_verified")));

        assertThat(foundEmailVerified).isTrue();
    }

    @Test
    void createUser_whenPasswordTicketFails_returnsUserIdOnly() {
        setupSuccessfulMocks("auth0|no-ticket", null); // null triggers exception

        String result = client.createUser("noticket@example.com", "No", "Ticket", UserRole.BROKER, "en");

        // Should return user ID without ticket URL when ticket creation fails
        assertThat(result).isEqualTo("auth0|no-ticket");
    }

    @Test
    void createUser_sendsAuthorizationHeader() {
        setupSuccessfulMocks("auth0|auth-test", "https://test.auth0.com/ticket");

        client.createUser("auth@example.com", "Auth", "Test", UserRole.CLIENT, "en");

        // Verify Bearer token is sent in subsequent requests
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, atLeast(2)).postForEntity(anyString(), entityCaptor.capture(), any(Class.class));

        // Check if any request (after token request) has Authorization header
        boolean foundAuthHeader = entityCaptor.getAllValues().stream()
                .filter(entity -> entity.getHeaders().containsKey("Authorization"))
                .anyMatch(entity -> {
                    String authHeader = entity.getHeaders().getFirst("Authorization");
                    return authHeader != null && authHeader.startsWith("Bearer ");
                });

        assertThat(foundAuthHeader).isTrue();
    }

    @Test
    void createUser_tokenRequestFails_throwsException() {
        // Mock failed token request
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/oauth/token"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.TokenResponse.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        assertThatThrownBy(() -> 
                client.createUser("fail@example.com", "Fail", "User", UserRole.BROKER, "en")
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Failed to obtain Auth0 management token");
    }

    @Test
    void createUser_userCreationFails_throwsException() {
        // Mock successful token
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/oauth/token"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.TokenResponse.class)
        )).thenReturn(ResponseEntity.ok(new Auth0ManagementClient.TokenResponse("token")));

        // Mock failed user creation
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/api/v2/users"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.Auth0UserResponse.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        assertThatThrownBy(() -> 
                client.createUser("bad@example.com", "Bad", "User", UserRole.CLIENT, "en")
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Failed to create Auth0 user");
    }

    @Test
    void createUser_roleAssignmentFails_throwsException() {
        // Mock successful token
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/oauth/token"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.TokenResponse.class)
        )).thenReturn(ResponseEntity.ok(new Auth0ManagementClient.TokenResponse("token")));

        // Mock successful user creation
        when(restTemplate.postForEntity(
                eq("https://test.auth0.com/api/v2/users"),
                any(HttpEntity.class),
                eq(Auth0ManagementClient.Auth0UserResponse.class)
        )).thenReturn(ResponseEntity.ok(new Auth0ManagementClient.Auth0UserResponse("auth0|role-fail")));

        // Mock failed role assignment
        when(restTemplate.postForEntity(
                contains("/roles/"),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build());

        assertThatThrownBy(() -> 
                client.createUser("rolefail@example.com", "Role", "Fail", UserRole.ADMIN, "en")
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Failed to assign role");
    }
}
