package com.example.courtierprobackend.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GitHubService.
 */
@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GitHubService gitHubService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        gitHubService = new GitHubService(restTemplate, objectMapper);
        ReflectionTestUtils.setField(gitHubService, "githubToken", "test-token");
        ReflectionTestUtils.setField(gitHubService, "repoOwner", "TestOwner");
        ReflectionTestUtils.setField(gitHubService, "repoName", "TestRepo");
    }

    @Test
    void createIssue_WithBugType_CreatesIssueWithBugLabels() {
        // Arrange
        GitHubService.GitHubIssueResponse expectedResponse = GitHubService.GitHubIssueResponse.builder()
                .number(42)
                .htmlUrl("https://github.com/TestOwner/TestRepo/issues/42")
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitHubService.GitHubIssueResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        // Act
        GitHubService.GitHubIssueResponse response = gitHubService.createIssue(
                "bug",
                "This is a bug description",
                "user@example.com"
        );

        // Assert
        assertThat(response.getNumber()).isEqualTo(42);
        assertThat(response.getHtmlUrl()).isEqualTo("https://github.com/TestOwner/TestRepo/issues/42");

        // Verify the request was made with correct URL
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), any(), eq(GitHubService.GitHubIssueResponse.class));
        assertThat(urlCaptor.getValue()).isEqualTo("https://api.github.com/repos/TestOwner/TestRepo/issues");
    }

    @Test
    void createIssue_WithFeatureType_CreatesIssueWithEnhancementLabels() {
        // Arrange
        GitHubService.GitHubIssueResponse expectedResponse = GitHubService.GitHubIssueResponse.builder()
                .number(100)
                .htmlUrl("https://github.com/TestOwner/TestRepo/issues/100")
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitHubService.GitHubIssueResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        // Act
        GitHubService.GitHubIssueResponse response = gitHubService.createIssue(
                "feature",
                "Please add this awesome feature",
                "developer@example.com"
        );

        // Assert
        assertThat(response.getNumber()).isEqualTo(100);
    }

    @Test
    void createIssue_WithLongMessage_TruncatesTitleTo50Chars() {
        // Arrange
        String longMessage = "This is a very long message that exceeds fifty characters and should be truncated in the title";
        
        GitHubService.GitHubIssueResponse expectedResponse = GitHubService.GitHubIssueResponse.builder()
                .number(1)
                .htmlUrl("https://github.com/TestOwner/TestRepo/issues/1")
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitHubService.GitHubIssueResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        // Act
        gitHubService.createIssue("bug", longMessage, "user@example.com");

        // Assert - verify request contains truncated title
        ArgumentCaptor<HttpEntity<GitHubService.GitHubIssueRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(GitHubService.GitHubIssueResponse.class));
        
        GitHubService.GitHubIssueRequest request = entityCaptor.getValue().getBody();
        assertThat(request.getTitle()).contains("[Bug]");
        assertThat(request.getTitle()).contains("...");
    }

    @Test
    void createIssue_WithNullToken_LogsAndReturnsPlaceholder() {
        // Arrange
        ReflectionTestUtils.setField(gitHubService, "githubToken", null);

        // Act
        GitHubService.GitHubIssueResponse response = gitHubService.createIssue(
                "bug",
                "Test message",
                "user@example.com"
        );

        // Assert
        assertThat(response.getNumber()).isEqualTo(0);
        assertThat(response.getHtmlUrl()).isEqualTo("not-configured");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void createIssue_WithEmptyToken_LogsAndReturnsPlaceholder() {
        // Arrange
        ReflectionTestUtils.setField(gitHubService, "githubToken", "");

        // Act
        GitHubService.GitHubIssueResponse response = gitHubService.createIssue(
                "bug",
                "Test message",
                "user@example.com"
        );

        // Assert
        assertThat(response.getNumber()).isEqualTo(0);
        assertThat(response.getHtmlUrl()).isEqualTo("not-configured");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void createIssue_WithBlankToken_LogsAndReturnsPlaceholder() {
        // Arrange
        ReflectionTestUtils.setField(gitHubService, "githubToken", "   ");

        // Act
        GitHubService.GitHubIssueResponse response = gitHubService.createIssue(
                "bug",
                "Test message",
                "user@example.com"
        );

        // Assert
        assertThat(response.getNumber()).isEqualTo(0);
        assertThat(response.getHtmlUrl()).isEqualTo("not-configured");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void createIssue_WhenRestTemplateThrowsException_PropagatesException() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitHubService.GitHubIssueResponse.class)
        )).thenThrow(new RuntimeException("Connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> gitHubService.createIssue("bug", "Test message", "user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create GitHub issue");
    }

    @Test
    void createIssue_WithNullUserEmail_IncludesMessageWithoutUserInfo() {
        // Arrange
        GitHubService.GitHubIssueResponse expectedResponse = GitHubService.GitHubIssueResponse.builder()
                .number(55)
                .htmlUrl("https://github.com/TestOwner/TestRepo/issues/55")
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitHubService.GitHubIssueResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        // Act
        GitHubService.GitHubIssueResponse response = gitHubService.createIssue(
                "feature",
                "Feature without email",
                null
        );

        // Assert
        assertThat(response.getNumber()).isEqualTo(55);
    }

    @Test
    void createIssue_VerifiesCorrectHeaders() {
        // Arrange
        GitHubService.GitHubIssueResponse expectedResponse = GitHubService.GitHubIssueResponse.builder()
                .number(1)
                .htmlUrl("https://github.com/TestOwner/TestRepo/issues/1")
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitHubService.GitHubIssueResponse.class)
        )).thenReturn(ResponseEntity.ok(expectedResponse));

        // Act
        gitHubService.createIssue("bug", "Test message", "user@example.com");

        // Assert - verify headers
        ArgumentCaptor<HttpEntity<GitHubService.GitHubIssueRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(GitHubService.GitHubIssueResponse.class));
        
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer test-token");
        assertThat(headers.getFirst("Accept")).isEqualTo("application/vnd.github+json");
        assertThat(headers.getFirst("X-GitHub-Api-Version")).isEqualTo("2022-11-28");
    }

    @Test
    void createIssue_WhenResponseIsNotSuccessful_ThrowsException() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitHubService.GitHubIssueResponse.class)
        )).thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).body(null));

        // Act & Assert
        assertThatThrownBy(() -> gitHubService.createIssue("bug", "Test message", "user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create GitHub issue");
    }
}
