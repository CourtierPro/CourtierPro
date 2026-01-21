package com.example.courtierprobackend.common.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InternalServerException.
 * Covers all constructor variants.
 */
class InternalServerExceptionTest {

    @Test
    void defaultConstructor_createsExceptionWithNoMessage() {
        InternalServerException exception = new InternalServerException();
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void messageConstructor_createsExceptionWithMessage() {
        String message = "Something went wrong";
        
        InternalServerException exception = new InternalServerException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void causeConstructor_createsExceptionWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        
        InternalServerException exception = new InternalServerException(cause);
        
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void messageAndCauseConstructor_createsExceptionWithBoth() {
        String message = "Something went wrong";
        Throwable cause = new RuntimeException("Root cause");
        
        InternalServerException exception = new InternalServerException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exception_isRuntimeException() {
        InternalServerException exception = new InternalServerException("Test");
        
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
