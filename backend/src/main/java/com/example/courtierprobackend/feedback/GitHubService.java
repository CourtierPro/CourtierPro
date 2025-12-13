package com.example.courtierprobackend.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${github.token:}")
    private String githubToken;

    @Value("${github.repo.owner:CourtierPro}")
    private String repoOwner;

    @Value("${github.repo.name:CourtierPro}")
    private String repoName;

    private static final String GITHUB_API_URL = "https://api.github.com";

    /**
     * Creates a GitHub issue from user feedback
     * 
     * @param type    The type of feedback (bug or feature)
     * @param message The feedback message
     * @param userEmail The email of the user submitting feedback (optional)
     * @return GitHubIssueResponse containing issue details
     */
    public GitHubIssueResponse createIssue(String type, String message, String userEmail) {
        if (githubToken == null || githubToken.isBlank()) {
            log.warn("GitHub token not configured. Feedback will be logged but not submitted to GitHub.");
            log.info("Feedback received - Type: {}, Message: {}, User: {}", type, message, userEmail);
            return GitHubIssueResponse.builder()
                    .number(0)
                    .htmlUrl("not-configured")
                    .build();
        }

        String url = String.format("%s/repos/%s/%s/issues", GITHUB_API_URL, repoOwner, repoName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(githubToken);
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
