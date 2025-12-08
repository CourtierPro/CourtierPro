package com.example.courtierprobackend.transactions.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TransactionControllerExceptionHandler.
 * Tests exception handling for transaction-related exceptions.
 */
class TransactionControllerExceptionHandlerTest {

    private TransactionControllerExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionControllerExceptionHandler();
    }

    // ========== handleInvalidInput Tests ==========

    @Test
    void handleInvalidInput_WithMessage_ReturnsBadRequestWithMessage() {
        // Arrange
        InvalidInputException exception = new InvalidInputException("Invalid transaction data");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleInvalidInput(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid transaction data");
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleInvalidInput_WithNullMessage_ReturnsDefaultMessage() {
        // Arrange
        InvalidInputException exception = new InvalidInputException((String) null);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleInvalidInput(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid input");
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_INPUT");
    }

    @Test
    void handleInvalidInput_WithEmptyConstructor_ReturnsDefaultMessage() {
        // Arrange
        InvalidInputException exception = new InvalidInputException();

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleInvalidInput(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid input");
    }

    // ========== handleNotFound Tests ==========

    @Test
    void handleNotFound_WithMessage_ReturnsNotFoundWithMessage() {
        // Arrange
        NotFoundException exception = new NotFoundException("Transaction not found");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Transaction not found");
        assertThat(response.getBody().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleNotFound_WithNullMessage_ReturnsDefaultMessage() {
        // Arrange
        NotFoundException exception = new NotFoundException(null);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Resource not found");
        assertThat(response.getBody().getCode()).isEqualTo("NOT_FOUND");
    }

    // ========== Exception Class Tests ==========

    @Test
    void invalidInputException_WithCause_PreservesCause() {
        // Arrange
        RuntimeException cause = new RuntimeException("Root cause");
        
        // Act
        InvalidInputException exception = new InvalidInputException(cause);

        // Assert
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void invalidInputException_WithMessageAndCause_PreservesBoth() {
        // Arrange
        RuntimeException cause = new RuntimeException("Root cause");
        
        // Act
        InvalidInputException exception = new InvalidInputException("Error message", cause);

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Error message");
        assertThat(exception.getCause()).isEqualTo(cause);
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
}
