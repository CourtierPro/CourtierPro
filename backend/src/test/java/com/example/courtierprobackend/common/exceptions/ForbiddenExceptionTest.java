package com.example.courtierprobackend.common.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ForbiddenException.
 * Covers all constructor variants.
 */
class ForbiddenExceptionTest {

    @Test
    void defaultConstructor_createsExceptionWithNoMessage() {
        ForbiddenException exception = new ForbiddenException();
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void messageConstructor_createsExceptionWithMessage() {
        String message = "Access denied";
        
        ForbiddenException exception = new ForbiddenException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void messageAndCauseConstructor_createsExceptionWithBoth() {
        String message = "Access denied";
        Throwable cause = new RuntimeException("Permission check failed");
        
        ForbiddenException exception = new ForbiddenException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exception_isRuntimeException() {
        ForbiddenException exception = new ForbiddenException("Test");
        
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
