package com.example.courtierprobackend.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${github.app.id:}")
    private String appId;

    @Value("${github.app.installation-id:}")
    private String installationId;

    @Value("${github.app.private-key:}")
    private String privateKeyPem;

    @Value("${github.repo.owner:CourtierPro}")
    private String repoOwner;

    @Value("${github.repo.name:CourtierPro}")
    private String repoName;

    private static final String GITHUB_API_URL = "https://api.github.com";

    // Cache for installation access token
    private String cachedAccessToken;
    private Instant tokenExpiresAt;

    /**
     * Creates a GitHub issue from user feedback
     * 
     * @param type    The type of feedback (bug or feature)
     * @param message The feedback message
     * @param userEmail The email of the user submitting feedback (optional)
     * @return GitHubIssueResponse containing issue details
     */
    public GitHubIssueResponse createIssue(String type, String message, String userEmail) {
        if (!isConfigured()) {
            log.warn("GitHub App not configured. Feedback will be logged but not submitted to GitHub.");
            log.info("Feedback received - Type: {}, Message: {}, User: {}", type, message, userEmail);
            return GitHubIssueResponse.builder()
                    .number(0)
                    .htmlUrl("not-configured")
                    .build();
        }

        String accessToken = getInstallationAccessToken();
        if (accessToken == null) {
            log.error("Failed to obtain installation access token");
            throw new RuntimeException("Failed to authenticate with GitHub App");
        }

        String url = String.format("%s/repos/%s/%s/issues", GITHUB_API_URL, repoOwner, repoName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        String title = buildIssueTitle(type, message);
        String body = buildIssueBody(type, message, userEmail);
        List<String> labels = buildLabels(type);

        GitHubIssueRequest issueRequest = GitHubIssueRequest.builder()
                .title(title)
                .body(body)
                .labels(labels)
                .build();

        try {
            HttpEntity<GitHubIssueRequest> entity = new HttpEntity<>(issueRequest, headers);
            ResponseEntity<GitHubIssueResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GitHubIssueResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("GitHub issue created successfully: #{}", response.getBody().getNumber());
                return response.getBody();
            } else {
                log.error("Failed to create GitHub issue. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to create GitHub issue");
            }
        } catch (Exception e) {
            log.error("Error creating GitHub issue: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create GitHub issue: " + e.getMessage(), e);
        }
    }

    private boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && installationId != null && !installationId.isBlank()
                && privateKeyPem != null && !privateKeyPem.isBlank();
    }

    /**
     * Get an installation access token, using cache if valid
     */
    private synchronized String getInstallationAccessToken() {
        // Return cached token if still valid (with 1 minute buffer)
        if (cachedAccessToken != null && tokenExpiresAt != null 
                && Instant.now().plusSeconds(60).isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }

        try {
            String jwt = generateJwt();
            String url = String.format("%s/app/installations/%s/access_tokens", GITHUB_API_URL, installationId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwt);
            headers.set("Accept", "application/vnd.github+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cachedAccessToken = (String) response.getBody().get("token");
                String expiresAtStr = (String) response.getBody().get("expires_at");
                tokenExpiresAt = Instant.parse(expiresAtStr);
                log.debug("Obtained new GitHub installation access token, expires at {}", tokenExpiresAt);
                return cachedAccessToken;
            } else {
                log.error("Failed to get installation access token. Status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting installation access token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate a JWT for GitHub App authentication
     */
    private String generateJwt() throws Exception {
        PrivateKey privateKey = parsePrivateKey(privateKeyPem);

        Instant now = Instant.now();
        // JWT valid for 10 minutes (GitHub maximum)
        Instant expiration = now.plusSeconds(600);

        return Jwts.builder()
                .setIssuer(appId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Parse PEM-encoded private key
     */
    private PrivateKey parsePrivateKey(String pem) throws Exception {
        // Handle escaped newlines from environment variables
        String normalizedPem = pem.replace("\\n", "\n");
        
        // Remove PEM headers and whitespace
        String privateKeyContent = normalizedPem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);

        // Try PKCS8 format first (standard), fall back to PKCS1 (RSA-specific)
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            // If PKCS8 fails, the key might be PKCS1 format - convert it
            return parsePkcs1PrivateKey(keyBytes);
        }
    }

    /**
     * Parse PKCS1 (RSA-specific) private key format
     */
    private PrivateKey parsePkcs1PrivateKey(byte[] pkcs1Bytes) throws Exception {
        // PKCS1 to PKCS8 header for RSA private key
        byte[] pkcs8Header = {
                0x30, (byte) 0x82, 0x00, 0x00, // SEQUENCE, length placeholder
                0x02, 0x01, 0x00,              // INTEGER 0 (version)
                0x30, 0x0D,                     // SEQUENCE
                0x06, 0x09,                     // OID
                0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,  // RSA OID
                0x05, 0x00,                     // NULL
                0x04, (byte) 0x82, 0x00, 0x00   // OCTET STRING, length placeholder
        };

        int pkcs8Length = pkcs8Header.length + pkcs1Bytes.length;
        byte[] pkcs8Bytes = new byte[pkcs8Length];

        // Copy header
        System.arraycopy(pkcs8Header, 0, pkcs8Bytes, 0, pkcs8Header.length);
        // Copy PKCS1 key
        System.arraycopy(pkcs1Bytes, 0, pkcs8Bytes, pkcs8Header.length, pkcs1Bytes.length);

        // Fix lengths in header
        int seqLen = pkcs8Length - 4;
        pkcs8Bytes[2] = (byte) ((seqLen >> 8) & 0xFF);
        pkcs8Bytes[3] = (byte) (seqLen & 0xFF);

        int octetLen = pkcs1Bytes.length;
        pkcs8Bytes[pkcs8Header.length - 2] = (byte) ((octetLen >> 8) & 0xFF);
        pkcs8Bytes[pkcs8Header.length - 1] = (byte) (octetLen & 0xFF);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private String buildIssueTitle(String type, String message) {
        String prefix = "bug".equalsIgnoreCase(type) ? "[Bug]" : "[Feature Request]";
        // Use first 50 chars of message as title, or the whole message if shorter
        String titleContent = message.length() > 50 
                ? message.substring(0, 50) + "..." 
                : message;
        // Remove newlines from title
        titleContent = titleContent.replace("\n", " ").replace("\r", " ");
        return prefix + " " + titleContent;
    }

    private String buildIssueBody(String type, String message, String userEmail) {
        StringBuilder body = new StringBuilder();
        
        body.append("## ").append("bug".equalsIgnoreCase(type) ? "Bug Report" : "Feature Request").append("\n\n");
        
        body.append("### Description\n\n");
        body.append(message).append("\n\n");
        
        body.append("---\n\n");
        body.append("*Submitted via CourtierPro Feedback Form*\n");
        
        if (userEmail != null && !userEmail.isBlank()) {
            body.append("*Submitted by: ").append(userEmail).append("*\n");
        }
        
        return body.toString();
    }

    private List<String> buildLabels(String type) {
        if ("bug".equalsIgnoreCase(type)) {
            return List.of("bug", "user-feedback");
        } else {
            return List.of("enhancement", "user-feedback");
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubIssueRequest {
        private String title;
        private String body;
        private List<String> labels;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubIssueResponse {
        private Integer number;
        
        @JsonProperty("html_url")
        private String htmlUrl;
    }
}
