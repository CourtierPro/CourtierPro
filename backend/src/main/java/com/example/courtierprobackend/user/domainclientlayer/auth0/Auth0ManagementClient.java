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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class Auth0ManagementClient {

    private static final Logger logger =
            LoggerFactory.getLogger(Auth0ManagementClient.class);

    // 7 days validity in seconds
    private static final int PASSWORD_TICKET_TTL_SECONDS = 7 * 24 * 60 * 60;

    // RestTemplate to support PATCH
    private final RestTemplate restTemplate;

    private final String domain;
    private final String clientId;
    private final String clientSecret;
    private final String audience;

    private final String managementBaseUrl;
    private final String tokenUrl;

    // Frontend URL where the user gets redirected after setting the password
    private final String passwordSetupResultUrl;

    // Auth0 role IDs (from Dashboard → User Management → Roles)
    private final String adminRoleId  = "rol_2zQ5SYaHM3eDsUF3";
    private final String brokerRoleId = "rol_l9MqshX9J77aopLk";
    private final String clientRoleId = "rol_29T806IHSWNeRS2Z";

    // “Username-Password-Authentication” is the default name of the “Database” connection created by Auth0.
    // This is where your users will be created when you provision them via the API with a generated password.
    private final String dbConnection = "Username-Password-Authentication";

    public Auth0ManagementClient(
            @Value("${auth0.domain}") String domain,
            @Value("${auth0.management.client-id}") String clientId,
            @Value("${auth0.management.client-secret}") String clientSecret,
            @Value("${auth0.management.audience}") String audience,
            // configurable result URL, with a sensible local default
            @Value("${app.frontend.password-setup-url:http://localhost:8081/login?passwordSet=true}")
            String passwordSetupResultUrl
    ) {
        this.domain = domain;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = audience;
        this.passwordSetupResultUrl = passwordSetupResultUrl;

        this.managementBaseUrl = "https://" + domain + "/api/v2";
        this.tokenUrl = "https://" + domain + "/oauth/token";

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();
        this.restTemplate = new RestTemplate(requestFactory);
    }

    // Obtention of token Management
    private String getManagementToken() {
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

        ResponseEntity<TokenResponse> response =
                restTemplate.postForEntity(url, entity, TokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to obtain Auth0 management token");
        }

        return response.getBody().accessToken();
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}

    // Generate a temporary random password (user will never use this)
    private String generateTemporaryPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "!Aa1";
    }

    private String createPasswordChangeTicket(String auth0UserId) {
        String token = getManagementToken();
        String url = managementBaseUrl + "/tickets/password-change";

        Map<String, Object> body = Map.of(
                "user_id", auth0UserId,
                "result_url", passwordSetupResultUrl,
                "ttl_sec", PASSWORD_TICKET_TTL_SECONDS,
                "mark_email_as_verified", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to create password change ticket");
        }

        return (String) response.getBody().get("ticket");
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
        String tempPassword = generateTemporaryPassword();

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

        ResponseEntity<Auth0UserResponse> response =
                restTemplate.postForEntity(url, entity, Auth0UserResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to create Auth0 user");
        }

        String auth0UserId = response.getBody().userId();

        // Assign role
        assignRole(token, auth0UserId, role);

        // Create password change ticket and return the URL
        // The calling service will send the email
        try {
            String passwordSetupUrl = createPasswordChangeTicket(auth0UserId);
            logger.info("Password setup URL created for {} (Auth0 user {}): {}",
                    email, auth0UserId, passwordSetupUrl);

            // NOTE: we still return a pipe-delimited string for backward compatibility.
            // The service layer parses this safely (using lastIndexOf('|')).
            return auth0UserId + "|" + passwordSetupUrl;

        } catch (Exception e) {
            // Don't block user creation if ticket creation fails,
            // but log clearly so we can act on it.
            logger.error(
                    "Failed to create password setup ticket for Auth0 user {} ({})",
                    auth0UserId, email, e
            );
            // Caller will handle missing URL (only the ID is returned).
            return auth0UserId;
        }
    }

    private record Auth0UserResponse(@JsonProperty("user_id") String userId) {}

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
            throw new IllegalStateException(
                    "Failed to assign role " + role + " to user " + auth0UserId
            );
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
            throw new IllegalStateException(
                    "Failed to set blocked=" + blocked + " for " + auth0UserId
            );
        }
    }
}
