package com.example.courtierprobackend.feedback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedbackController.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    private FeedbackController feedbackController;

    @BeforeEach
    void setUp() {
        feedbackController = new FeedbackController(feedbackService);
    }

    @Test
    void submitFeedback_WithValidRequest_ReturnsOk() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("This is a valid bug report message")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "user@example.com")
                .build();

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .success(true)
                .issueNumber(123)
                .issueUrl("https://github.com/CourtierPro/CourtierPro/issues/123")
                .build();

        when(feedbackService.submitFeedback(any(FeedbackRequest.class), eq("user@example.com")))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<FeedbackResponse> response = feedbackController.submitFeedback(request, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getIssueNumber()).isEqualTo(123);
        verify(feedbackService).submitFeedback(request, "user@example.com");
    }

    @Test
    void submitFeedback_WithFeatureRequest_ReturnsOk() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("feature")
                .message("Please add dark mode to the application")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "developer@courtierpro.com")
                .build();

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .success(true)
                .issueNumber(456)
                .issueUrl("https://github.com/CourtierPro/CourtierPro/issues/456")
                .build();

        when(feedbackService.submitFeedback(any(FeedbackRequest.class), eq("developer@courtierpro.com")))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<FeedbackResponse> response = feedbackController.submitFeedback(request, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    void submitFeedback_WithAlternativeEmailClaim_ExtractsEmail() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Bug report with custom claim")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("https://courtierpro.dev/email", "custom@example.com")
                .build();

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .success(true)
                .issueNumber(789)
                .issueUrl("https://github.com/CourtierPro/CourtierPro/issues/789")
                .build();

        when(feedbackService.submitFeedback(any(FeedbackRequest.class), eq("custom@example.com")))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<FeedbackResponse> response = feedbackController.submitFeedback(request, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedbackService).submitFeedback(request, "custom@example.com");
    }

    @Test
    void submitFeedback_WithNullJwt_PassesNullEmail() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Bug report without JWT")
                .build();

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .success(true)
                .issueNumber(100)
                .issueUrl("https://github.com/CourtierPro/CourtierPro/issues/100")
                .build();

        when(feedbackService.submitFeedback(any(FeedbackRequest.class), isNull()))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<FeedbackResponse> response = feedbackController.submitFeedback(request, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedbackService).submitFeedback(request, null);
    }

    @Test
    void submitFeedback_WithJwtWithoutEmailClaim_PassesNullEmail() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("feature")
                .message("Feature request without email in JWT")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-id-123")
                .build();

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .success(true)
                .issueNumber(200)
                .issueUrl("https://github.com/CourtierPro/CourtierPro/issues/200")
                .build();

        when(feedbackService.submitFeedback(any(FeedbackRequest.class), isNull()))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<FeedbackResponse> response = feedbackController.submitFeedback(request, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedbackService).submitFeedback(request, null);
    }

    @Test
    void submitFeedback_WhenServiceReturnsFailure_ReturnsInternalServerError() {
        // Arrange
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("This will fail")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "user@example.com")
                .build();

        FeedbackResponse failedResponse = FeedbackResponse.builder()
                .success(false)
                .build();

        when(feedbackService.submitFeedback(any(FeedbackRequest.class), anyString()))
                .thenReturn(failedResponse);

        // Act
        ResponseEntity<FeedbackResponse> response = feedbackController.submitFeedback(request, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void submitFeedback_PrefersStandardEmailClaimOverCustom() {
        // Arrange - JWT has both standard and custom email claims
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Test with both email claims")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "standard@example.com")
                .claim("https://courtierpro.dev/email", "custom@example.com")
                .build();

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .success(true)
                .issueNumber(300)
                .issueUrl("https://github.com/CourtierPro/CourtierPro/issues/300")
                .build();

        when(feedbackService.submitFeedback(any(FeedbackRequest.class), eq("standard@example.com")))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<FeedbackResponse> response = feedbackController.submitFeedback(request, jwt);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedbackService).submitFeedback(request, "standard@example.com");
    }
}
