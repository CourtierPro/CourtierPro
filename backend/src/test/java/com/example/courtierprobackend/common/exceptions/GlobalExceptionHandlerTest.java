package com.example.courtierprobackend.common.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests exception handling for all global exceptions.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // ========== handleBadRequest Tests ==========

    @Test
    void handleBadRequest_WithMessage_ReturnsBadRequestWithMessage() {
        // Arrange
        BadRequestException exception = new BadRequestException("Invalid input data");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid input data");
        assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleBadRequest_WithNullMessage_ReturnsDefaultMessage() {
        // Arrange
        BadRequestException exception = new BadRequestException((String) null);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid input");
        assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
    }

    // ========== handleNotFound Tests ==========

    @Test
    void handleNotFound_WithMessage_ReturnsNotFoundWithMessage() {
        // Arrange
        NotFoundException exception = new NotFoundException("Resource not found");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Resource not found");
        assertThat(response.getBody().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleNotFound_WithNullMessage_ReturnsDefaultMessage() {
        // Arrange
        NotFoundException exception = new NotFoundException(null);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Resource not found");
        assertThat(response.getBody().getCode()).isEqualTo("NOT_FOUND");
    }

    // ========== handleForbidden Tests ==========

    @Test
    void handleForbidden_WithMessage_ReturnsForbiddenWithMessage() {
        // Arrange
        ForbiddenException exception = new ForbiddenException("Access denied");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Access denied");
        assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleForbidden_WithNullMessage_ReturnsDefaultMessage() {
        // Arrange
        ForbiddenException exception = new ForbiddenException((String) null);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Access denied");
        assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
    }

    // ========== handleUnauthorized Tests ==========

    @Test
    void handleUnauthorized_WithMessage_ReturnsUnauthorizedWithMessage() {
        // Arrange
        UnauthorizedException exception = new UnauthorizedException("Authentication required");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Authentication required");
        assertThat(response.getBody().getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    // ========== handleInternalError Tests ==========

    @Test
    void handleInternalError_WithMessage_ReturnsInternalServerErrorWithMessage() {
        // Arrange
        InternalServerException exception = new InternalServerException("Something went wrong");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleInternalError(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Something went wrong");
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    // ========== handleIllegalArgument Tests ==========

    @Test
    void handleIllegalArgument_WithMessage_ReturnsBadRequest() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid argument");
        assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
    }

    // ========== ErrorResponse Tests ==========

    @Test
    void errorResponse_OfWithErrorAndCode_CreatesResponse() {
        // Act
        ErrorResponse response = ErrorResponse.of("Test error", "TEST_CODE");

        // Assert
        assertThat(response.getError()).isEqualTo("Test error");
        assertThat(response.getCode()).isEqualTo("TEST_CODE");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isNull();
    }

    @Test
    void errorResponse_OfWithPath_IncludesPath() {
        // Act
        ErrorResponse response = ErrorResponse.of("Test error", "TEST_CODE", "/api/transactions");

        // Assert
        assertThat(response.getError()).isEqualTo("Test error");
        assertThat(response.getCode()).isEqualTo("TEST_CODE");
        assertThat(response.getPath()).isEqualTo("/api/transactions");
        assertThat(response.getTimestamp()).isNotNull();
    }

    // ========== Exception Class Tests ==========

    @Test
    void badRequestException_WithCause_PreservesCause() {
        // Arrange
        RuntimeException cause = new RuntimeException("Root cause");
        
        // Act
        BadRequestException exception = new BadRequestException(cause);

        // Assert
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void badRequestException_WithMessageAndCause_PreservesBoth() {
        // Arrange
        RuntimeException cause = new RuntimeException("Root cause");
        
        // Act
        BadRequestException exception = new BadRequestException("Error message", cause);

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Error message");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
