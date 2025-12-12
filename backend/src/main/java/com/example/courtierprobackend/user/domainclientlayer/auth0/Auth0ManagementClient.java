package com.example.courtierprobackend.user.domainclientlayer.auth0;

import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class Auth0ManagementClient {

    private static final Logger log = LoggerFactory.getLogger(Auth0ManagementClient.class);
    
    // Token cache - Auth0 tokens are valid for 24h, we cache for 23h to be safe
    private static final Duration TOKEN_CACHE_DURATION = Duration.ofHours(23);
    private String cachedToken;
    private Instant tokenExpiresAt;

    // RestTemplate to support PATCH
    private final RestTemplate restTemplate;

    private final String domain;
    private final String clientId;
    private final String clientSecret;
    private final String audience;

    private final String managementBaseUrl;
    private final String tokenUrl;

    // Auth0 role IDs (from Dashboard → User Management → Roles)
    private final String adminRoleId  = "rol_2zQ5SYaHM3eDsUF3";
    private final String brokerRoleId = "rol_l9MqshX9J77aopLk";
    private final String clientRoleId = "rol_29T806IHSWNeRS2Z";

    // “Username-Password-Authentication” is the default name of the “Database” connection created by Auth0.
    //This is where your users will be created when you provision them via the API with a generated password.
    //Connection Database → email + password stock in Auth0
    private final String dbConnection = "Username-Password-Authentication";

    public Auth0ManagementClient(
            @Value("${auth0.domain}") String domain,
            @Value("${auth0.management.client-id}") String clientId,
            @Value("${auth0.management.client-secret}") String clientSecret,
            @Value("${auth0.management.audience}") String audience
    ) {
        this.domain = domain;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = audience;

        // URLs factorisées
        this.managementBaseUrl = "https://" + domain + "/api/v2";
        this.tokenUrl = "https://" + domain + "/oauth/token";

        //  support PATCH
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();
        this.restTemplate = new RestTemplate(requestFactory);
    }


    // Obtention of token Management (with caching to avoid rate limits)
    private synchronized String getManagementToken() {
        // Return cached token if still valid
        if (cachedToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        
        log.debug("Fetching new Auth0 management token (previous token expired or not cached)");
        
        String url = tokenUrl;

        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "audience", audience,
                "grant_type", "client_credentials"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(url, entity, TokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to obtain Auth0 management token");
        }

        // Cache the token
        cachedToken = response.getBody().accessToken();
        tokenExpiresAt = Instant.now().plus(TOKEN_CACHE_DURATION);
        
        log.debug("Auth0 management token cached, expires at {}", tokenExpiresAt);
        
        return cachedToken;
    }

    public record TokenResponse(@JsonProperty("access_token") String accessToken) {}

    // Generate a random password (for the guest user)
    //until ticket CP-33 is not done, a random password is generated(you can modify it on Auth0 for now)
    private String generateRandomPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Create a user in Auth0.
     * We now also push preferredLanguage into user_metadata.preferred_language
     * so Auth0 Actions can use it to build the lang claim.
     */
    public String createUser(
            String email,
            String firstName,
            String lastName,
            UserRole role,
            String preferredLanguage
    ) {
        String token = getManagementToken();

        String url = managementBaseUrl + "/users";

        // Normalize language: en / fr only, default en
        String safeLang = (preferredLanguage != null && !preferredLanguage.isBlank())
                ? preferredLanguage.toLowerCase()
                : "en";
        if (!safeLang.equals("fr")) {
            safeLang = "en";
        }

        // Generate temporary password that user will never see
        String tempPassword = generateRandomPassword();

        // minimal metadata for language
        Map<String, Object> userMetadata = Map.of(
                "preferred_language", safeLang
        );

        // Create user with temporary password - mark email as verified since admin is creating them
        Map<String, Object> body = Map.of(
                "email", email,
                "given_name", firstName,
                "family_name", lastName,
                "connection", dbConnection,
                "password", tempPassword,
                "email_verified", true,  // Admin-created users are pre-verified
                "user_metadata", userMetadata // store language in Auth0
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Auth0UserResponse> response;
        try {
            response = restTemplate.postForEntity(url, entity, Auth0UserResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            // 409 Conflict - user already exists in Auth0
            throw new IllegalArgumentException("A user with email " + email + " already exists.");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Other client errors (400, 401, 403, etc.)
            throw new IllegalStateException("Auth0 error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to create Auth0 user");
        }

        String auth0UserId = response.getBody().userId();

        assignRole(token, auth0UserId, role);

        return auth0UserId;
    }

    public record Auth0UserResponse(@JsonProperty("user_id") String userId) {}

    // Assign a role in Auth0

    private void assignRole(String token, String auth0UserId, UserRole role) {

        String roleId = switch (role) {
            case ADMIN -> adminRoleId;
            case BROKER -> brokerRoleId;
            case CLIENT -> clientRoleId;
        };

        String url = managementBaseUrl + "/roles/" + roleId + "/users";

        Map<String, Object> body = Map.of(
                "users", List.of(auth0UserId)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response =
                restTemplate.postForEntity(url, entity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to assign role " + role + " to user " + auth0UserId);
        }
    }

    // block/unblock a user
    public void setBlocked(String auth0UserId, boolean blocked) {
        String token = getManagementToken();

        String url = managementBaseUrl + "/users/" + auth0UserId;

        Map<String, Object> body = Map.of(
                "blocked", blocked
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response =
                restTemplate.exchange(url, HttpMethod.PATCH, entity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to set blocked=" + blocked + " for " + auth0UserId);
        }
    }

    /**
     * Creates a password change ticket for a user to set their initial password.
     * Returns the URL that the user can use to set their password.
     */
    public String createPasswordChangeTicket(String auth0UserId) {
        String token = getManagementToken();

        String url = managementBaseUrl + "/tickets/password-change";

        Map<String, Object> body = Map.of(
                "user_id", auth0UserId,
                "result_url", "https://courtierpro.dev/login", // Redirect after password set
                "ttl_sec", 604800, // 7 days
                "mark_email_as_verified", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<PasswordChangeTicketResponse> response =
                restTemplate.postForEntity(url, entity, PasswordChangeTicketResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to create password change ticket for " + auth0UserId);
        }

        return response.getBody().ticket();
    }

    record PasswordChangeTicketResponse(String ticket) {}

    // ==================== USER SYNC METHODS ====================

    /**
     * DTO for users returned by the Auth0 Management API.
     */
    public record Auth0User(
            @JsonProperty("user_id") String userId,
            @JsonProperty("email") String email,
            @JsonProperty("given_name") String givenName,
            @JsonProperty("family_name") String familyName,
            @JsonProperty("user_metadata") Map<String, Object> userMetadata
    ) {
        public String getPreferredLanguage() {
            if (userMetadata == null) return "en";
            Object lang = userMetadata.get("preferred_language");
            return lang != null ? lang.toString() : "en";
        }
    }

    /**
     * DTO for roles returned by the Auth0 Management API.
     */
    public record Auth0Role(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {}

    /**
     * Fetches all users from Auth0 using pagination.
     * Returns a list of Auth0User objects.
     */
    public List<Auth0User> listAllUsers() {
        String token = getManagementToken();
        List<Auth0User> allUsers = new java.util.ArrayList<>();
        int page = 0;
        int perPage = 100;
        boolean hasMore = true;

        while (hasMore) {
            String url = managementBaseUrl + "/users?per_page=" + perPage + "&page=" + page + "&include_totals=false";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Auth0User[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Auth0User[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Auth0User[] users = response.getBody();
                allUsers.addAll(java.util.Arrays.asList(users));
                hasMore = users.length == perPage; // If we got a full page, there might be more
                page++;
            } else {
                hasMore = false;
            }
        }

        return allUsers;
    }

    /**
     * Fetches the roles assigned to a specific Auth0 user.
     * Returns a list of Auth0Role objects.
     */
    public List<Auth0Role> getUserRoles(String auth0UserId) {
        String token = getManagementToken();
        String url = managementBaseUrl + "/users/" + auth0UserId + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Auth0Role[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Auth0Role[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return java.util.Arrays.asList(response.getBody());
        }

        return List.of();
    }

    /**
     * Maps an Auth0 role name to the local UserRole enum.
     * Returns CLIENT as default if no matching role is found.
     */
    public UserRole mapRoleToUserRole(List<Auth0Role> roles) {
        for (Auth0Role role : roles) {
            String name = role.name().toLowerCase();
            if (name.contains("admin")) return UserRole.ADMIN;
            if (name.contains("broker")) return UserRole.BROKER;
            if (name.contains("client")) return UserRole.CLIENT;
        }
        return UserRole.CLIENT; // Default to CLIENT if no role found
    }
}
