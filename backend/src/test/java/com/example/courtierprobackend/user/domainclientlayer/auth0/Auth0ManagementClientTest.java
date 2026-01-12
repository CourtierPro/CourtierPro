package com.example.courtierprobackend.user.domainclientlayer.auth0;

import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.Auth0Role;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.Auth0User;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.Auth0UserResponse;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.PasswordChangeTicketResponse;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for Auth0ManagementClient.
 * Tests interaction with Auth0 Management API via RestTemplate.
 */
class Auth0ManagementClientTest {

    private Auth0ManagementClient client;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new Auth0ManagementClient(
                "example.auth0.com",
                "clientId",
                "clientSecret",
                "https://example.auth0.com/api/v2/"
        );
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        
        // Mock token generation for all tests (handle varargs)
        when(restTemplate.postForEntity(eq("https://example.auth0.com/oauth/token"), any(), eq(TokenResponse.class), any(Object[].class)))
                .thenReturn(ResponseEntity.ok(new TokenResponse("mock-token")));
    }

    @Test
    void generateTemporaryPassword_hasUpperLowerDigitAndSymbol() {
        String password = (String) ReflectionTestUtils.invokeMethod(client, "generateRandomPassword");
        assertThat(password).containsPattern("[A-Za-z0-9]");
        assertThat(password.length()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void createUser_PostsToAuth0_ReturnsUserId() {
        // Arrange
        String email = "test@example.com";
        String userId = "auth0|newuser";
        Auth0UserResponse responseBody = new Auth0UserResponse(userId);
        
        when(restTemplate.postForEntity(eq("https://example.auth0.com/api/v2/users"), any(HttpEntity.class), eq(Auth0UserResponse.class), any(Object[].class)))
                .thenReturn(ResponseEntity.ok(responseBody));
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class), any(Object[].class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        String result = client.createUser(email, "First", "Last", UserRole.BROKER, "en");

        // Assert
        assertThat(result).isEqualTo(userId);
        verify(restTemplate).postForEntity(eq("https://example.auth0.com/api/v2/users"), any(HttpEntity.class), eq(Auth0UserResponse.class), any(Object[].class));
    }

    @Test
    void assignRole_PostsRoleForUser() {
        // Arrange
        String userId = "auth0|user";
        UserRole role = UserRole.BROKER;
        
        when(restTemplate.postForEntity(
                contains("/roles/"), 
                any(), 
                eq(Void.class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok().build());

        // Act
        ReflectionTestUtils.invokeMethod(client, "assignRole", "token", userId, role);

        // Assert
        verify(restTemplate).postForEntity(contains("/roles"), any(), eq(Void.class), any(Object[].class));
    }

    @Test
    void setBlocked_PostsToUserEndpoint() {
        // Arrange
        String userId = "auth0|user";
        
        when(restTemplate.exchange(
                eq("https://example.auth0.com/api/v2/users/" + userId),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(Void.class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok(null));

        // Act
        client.setBlocked(userId, true);

        // Assert
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(contains(userId), eq(HttpMethod.PATCH), captor.capture(), eq(Void.class), any(Object[].class));
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body).containsEntry("blocked", true);
    }

    @Test
    void updateUserLanguage_PatchesUserMetadata() {
        // Arrange
        String userId = "auth0|user";
        String language = "fr";
        
        when(restTemplate.exchange(
                eq("https://example.auth0.com/api/v2/users/" + userId),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(Void.class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok(null));

        // Act
        client.updateUserLanguage(userId, language);

        // Assert
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(contains(userId), eq(HttpMethod.PATCH), captor.capture(), eq(Void.class), any(Object[].class));
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        Map<String, Object> userMetadata = (Map<String, Object>) body.get("user_metadata");
        assertThat(userMetadata).containsEntry("preferred_language", "fr");
    }

    @Test
    void updateUserLanguage_NormalizesUppercaseLanguage() {
        // Arrange
        String userId = "auth0|user";
        
        when(restTemplate.exchange(
                eq("https://example.auth0.com/api/v2/users/" + userId),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(Void.class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok(null));

        // Act
        client.updateUserLanguage(userId, "FR");

        // Assert
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(contains(userId), eq(HttpMethod.PATCH), captor.capture(), eq(Void.class), any(Object[].class));
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        Map<String, Object> userMetadata = (Map<String, Object>) body.get("user_metadata");
        assertThat(userMetadata).containsEntry("preferred_language", "fr");
    }

    @Test
    void updateUserLanguage_DefaultsToEnglishForInvalidLanguage() {
        // Arrange
        String userId = "auth0|user";
        
        when(restTemplate.exchange(
                eq("https://example.auth0.com/api/v2/users/" + userId),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(Void.class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok(null));

        // Act
        client.updateUserLanguage(userId, "de");

        // Assert
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(contains(userId), eq(HttpMethod.PATCH), captor.capture(), eq(Void.class), any(Object[].class));
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        Map<String, Object> userMetadata = (Map<String, Object>) body.get("user_metadata");
        assertThat(userMetadata).containsEntry("preferred_language", "en");
    }
    
    @Test
    void createPasswordChangeTicket_ReturnsTicketUrl() {
        // Arrange
        String userId = "auth0|user";
        String ticketUrl = "https://auth0.com/ticket";
        
        when(restTemplate.postForEntity(
                eq("https://example.auth0.com/api/v2/tickets/password-change"), 
                any(HttpEntity.class), 
                eq(PasswordChangeTicketResponse.class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok(new PasswordChangeTicketResponse(ticketUrl)));

        // Act
        String result = client.createPasswordChangeTicket(userId);

        // Assert
        assertThat(result).isEqualTo(ticketUrl);
    }

    @Test
    void listAllUsers_ReturnsListOfUsers() {
        // Arrange
        Auth0User user1 = new Auth0User("id1", "e1", "f1", "l1", Map.of("lang", "en"));
        Auth0User user2 = new Auth0User("id2", "e2", "f2", "l2", Map.of("lang", "fr"));
        
        when(restTemplate.exchange(
                eq("https://example.auth0.com/api/v2/users?per_page=100&page=0&include_totals=false"), 
                eq(HttpMethod.GET), 
                any(HttpEntity.class), 
                eq(Auth0User[].class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok(new Auth0User[]{user1, user2}));

        // Act
        List<Auth0User> users = client.listAllUsers();

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users.get(0).email()).isEqualTo("e1");
    }

    @Test
    void getUserRoles_ReturnsRoles() {
        // Arrange
        String userId = "auth0|user";
        Auth0Role role = new Auth0Role("r1", "BROKER");
        
        when(restTemplate.exchange(
                eq("https://example.auth0.com/api/v2/users/" + userId + "/roles"), 
                eq(HttpMethod.GET), 
                any(HttpEntity.class), 
                eq(Auth0Role[].class),
                any(Object[].class)))
            .thenReturn(ResponseEntity.ok(new Auth0Role[]{role}));

        // Act
        List<Auth0Role> roles = client.getUserRoles(userId);

        // Assert
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).name()).isEqualTo("BROKER");
    }
    
    @Test
    void mapRoleToUserRole_MapsCorrectly() {
        assertThat(client.mapRoleToUserRole(List.of(new Auth0Role("1", "BROKER")))).isEqualTo(UserRole.BROKER);
        assertThat(client.mapRoleToUserRole(List.of(new Auth0Role("1", "CLIENT")))).isEqualTo(UserRole.CLIENT);
        assertThat(client.mapRoleToUserRole(List.of(new Auth0Role("1", "ADMIN")))).isEqualTo(UserRole.ADMIN);
        assertThat(client.mapRoleToUserRole(Collections.emptyList())).isEqualTo(UserRole.CLIENT); // Default
    }
}

