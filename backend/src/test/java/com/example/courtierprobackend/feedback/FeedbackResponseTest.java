package com.example.courtierprobackend.feedback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FeedbackResponse.
 */
class FeedbackResponseTest {

    @Test
    void builder_CreatesSuccessResponse() {
        FeedbackResponse response = FeedbackResponse.builder()
                .success(true)
                .issueNumber(123)
                .issueUrl("https://github.com/CourtierPro/CourtierPro/issues/123")
                .build();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueNumber()).isEqualTo(123);
        assertThat(response.getIssueUrl()).isEqualTo("https://github.com/CourtierPro/CourtierPro/issues/123");
    }

    @Test
    void builder_CreatesFailureResponse() {
        FeedbackResponse response = FeedbackResponse.builder()
                .success(false)
                .errorMessage("An error occurred")
                .build();

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getIssueNumber()).isNull();
        assertThat(response.getIssueUrl()).isNull();
        assertThat(response.getErrorMessage()).isEqualTo("An error occurred");
    }

    @Test
    void builder_CreatesFailureResponseWithoutErrorMessage() {
        FeedbackResponse response = FeedbackResponse.builder()
                .success(false)
                .build();

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void noArgsConstructor_Works() {
        FeedbackResponse response = new FeedbackResponse();
        response.setSuccess(true);
        response.setIssueNumber(456);
        response.setIssueUrl("https://github.com/test/test/issues/456");
        response.setErrorMessage(null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueNumber()).isEqualTo(456);
        assertThat(response.getIssueUrl()).isEqualTo("https://github.com/test/test/issues/456");
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void allArgsConstructor_Works() {
        FeedbackResponse response = new FeedbackResponse(true, "https://github.com/test/issues/789", 789, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getIssueNumber()).isEqualTo(789);
        assertThat(response.getIssueUrl()).isEqualTo("https://github.com/test/issues/789");
    }

    @Test
    void equals_WorksCorrectly() {
        FeedbackResponse response1 = FeedbackResponse.builder()
                .success(true)
                .issueNumber(100)
                .issueUrl("https://github.com/test/issues/100")
                .build();

        FeedbackResponse response2 = FeedbackResponse.builder()
                .success(true)
                .issueNumber(100)
                .issueUrl("https://github.com/test/issues/100")
                .build();

        assertThat(response1).isEqualTo(response2);
    }

    @Test
    void hashCode_WorksCorrectly() {
        FeedbackResponse response1 = FeedbackResponse.builder()
                .success(true)
                .issueNumber(100)
                .issueUrl("https://github.com/test/issues/100")
                .build();

        FeedbackResponse response2 = FeedbackResponse.builder()
                .success(true)
                .issueNumber(100)
                .issueUrl("https://github.com/test/issues/100")
                .build();

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void toString_ContainsAllFields() {
        FeedbackResponse response = FeedbackResponse.builder()
                .success(true)
                .issueNumber(123)
                .issueUrl("https://github.com/test/issues/123")
                .build();

        String toString = response.toString();

        assertThat(toString).contains("success=true");
        assertThat(toString).contains("issueNumber=123");
        assertThat(toString).contains("issueUrl=https://github.com/test/issues/123");
    }
}
