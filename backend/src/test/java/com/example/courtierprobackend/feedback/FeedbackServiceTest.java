package com.example.courtierprobackend.feedback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedbackService.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private GitHubService gitHubService;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(gitHubService);
    }

    @Test
    void submitFeedback_WithValidBugReport_ReturnsSuccess() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("This is a bug report with enough characters")
                .build();
        String userEmail = "test@example.com";

        GitHubService.GitHubIssueResponse issueResponse = GitHubService.GitHubIssueResponse.builder()
                .number(123)
                .htmlUrl("https://github.com/CourtierPro/CourtierPro/issues/123")
                .build();

        when(gitHubService.createIssue(anyString(), anyString(), anyString()))
                .thenReturn(issueResponse);

        // Act
        FeedbackResponse response = feedbackService.submitFeedback(request, userEmail);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueNumber()).isEqualTo(123);
        assertThat(response.getIssueUrl()).isEqualTo("https://github.com/CourtierPro/CourtierPro/issues/123");
        verify(gitHubService).createIssue("bug", "This is a bug report with enough characters", "test@example.com");
    }

    @Test
    void submitFeedback_WithValidFeatureRequest_ReturnsSuccess() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("feature")
                .message("Please add this new feature to the application")
                .build();
        String userEmail = "user@courtierpro.com";

        GitHubService.GitHubIssueResponse issueResponse = GitHubService.GitHubIssueResponse.builder()
                .number(456)
                .htmlUrl("https://github.com/CourtierPro/CourtierPro/issues/456")
                .build();

        when(gitHubService.createIssue(anyString(), anyString(), anyString()))
                .thenReturn(issueResponse);

        // Act
        FeedbackResponse response = feedbackService.submitFeedback(request, userEmail);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueNumber()).isEqualTo(456);
        verify(gitHubService).createIssue("feature", "Please add this new feature to the application", "user@courtierpro.com");
    }

    @Test
    void submitFeedback_WithNullUserEmail_StillSubmits() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Bug report without user email")
                .build();

        GitHubService.GitHubIssueResponse issueResponse = GitHubService.GitHubIssueResponse.builder()
                .number(789)
                .htmlUrl("https://github.com/CourtierPro/CourtierPro/issues/789")
                .build();

        when(gitHubService.createIssue(anyString(), anyString(), isNull()))
                .thenReturn(issueResponse);

        // Act
        FeedbackResponse response = feedbackService.submitFeedback(request, null);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueNumber()).isEqualTo(789);
        verify(gitHubService).createIssue("bug", "Bug report without user email", null);
    }

    @Test
    void submitFeedback_WhenGitHubServiceThrowsException_ReturnsFailure() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("This should fail")
                .build();

        when(gitHubService.createIssue(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("GitHub API error"));

        // Act
        FeedbackResponse response = feedbackService.submitFeedback(request, "test@example.com");

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getIssueNumber()).isNull();
        assertThat(response.getIssueUrl()).isNull();
        assertThat(response.getErrorMessage()).isEqualTo("An unexpected error occurred while submitting feedback.");
    }

    @Test
    void submitFeedback_WhenConnectionException_ReturnsServiceUnavailableError() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Connection failure test")
                .build();

        // GitHubService wraps connection exceptions in RuntimeException
        when(gitHubService.createIssue(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection failed", new java.net.ConnectException("Connection refused")));

        // Act
        FeedbackResponse response = feedbackService.submitFeedback(request, "test@example.com");

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("GitHub service unavailable. Please try again later.");
    }

    @Test
    void submitFeedback_WhenIllegalArgumentException_ReturnsInvalidConfigError() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Invalid config test")
                .build();

        when(gitHubService.createIssue(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid argument"));

        // Act
        FeedbackResponse response = feedbackService.submitFeedback(request, "test@example.com");

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Invalid configuration or request data.");
    }
    @Test
    void submitFeedback_AnonymousRequest_IgnoresUserEmail() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Anonymous bug")
                .anonymous(true)
                .build();

        GitHubService.GitHubIssueResponse issueResponse = GitHubService.GitHubIssueResponse.builder()
                .number(999)
                .htmlUrl("http://url")
                .build();
        
        when(gitHubService.createIssue(anyString(), anyString(), isNull())).thenReturn(issueResponse);

        // Act
        // Pass a user email, but expect it to be ignored (passed as null to gitHubService)
        FeedbackResponse response = feedbackService.submitFeedback(request, "user@example.com");

        // Assert
        assertThat(response.isSuccess()).isTrue();
        verify(gitHubService).createIssue("bug", "Anonymous bug", null);
    }
}
