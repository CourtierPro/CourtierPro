package com.example.courtierprobackend.feedback;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FeedbackRequest validation.
 */
class FeedbackRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validBugRequest_PassesValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("This is a valid bug report message")
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validFeatureRequest_PassesValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("feature")
                .message("Please add this feature to the app")
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullType_FailsValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type(null)
                .message("Valid message here")
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("required");
    }

    @Test
    void invalidType_FailsValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("invalid")
                .message("Valid message here")
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("bug");
    }

    @Test
    void nullMessage_FailsValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message(null)
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void emptyMessage_FailsValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("")
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void blankMessage_FailsValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("   ")
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void messageTooShort_FailsValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("short")
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("10");
    }

    @Test
    void messageExactlyMinLength_PassesValidation() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("1234567890") // exactly 10 characters
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void messageAtMaxLength_PassesValidation() {
        String maxMessage = "a".repeat(5000);
        FeedbackRequest request = FeedbackRequest.builder()
                .type("feature")
                .message(maxMessage)
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void messageTooLong_FailsValidation() {
        String tooLongMessage = "a".repeat(5001);
        FeedbackRequest request = FeedbackRequest.builder()
                .type("feature")
                .message(tooLongMessage)
                .build();

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("5000");
    }

    @Test
    void builderCreatesCorrectObject() {
        FeedbackRequest request = FeedbackRequest.builder()
                .type("bug")
                .message("Test message")
                .build();

        assertThat(request.getType()).isEqualTo("bug");
        assertThat(request.getMessage()).isEqualTo("Test message");
    }

    @Test
    void noArgsConstructorWorks() {
        FeedbackRequest request = new FeedbackRequest();
        request.setType("feature");
        request.setMessage("Feature request");

        assertThat(request.getType()).isEqualTo("feature");
        assertThat(request.getMessage()).isEqualTo("Feature request");
    }

    @Test
    void allArgsConstructorWorks() {
        FeedbackRequest request = new FeedbackRequest("bug", "Bug description", false);

        assertThat(request.getType()).isEqualTo("bug");
        assertThat(request.getMessage()).isEqualTo("Bug description");
        assertThat(request.getAnonymous()).isEqualTo(false);
    }
}
