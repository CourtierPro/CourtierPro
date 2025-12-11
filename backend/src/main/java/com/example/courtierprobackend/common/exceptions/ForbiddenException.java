package com.example.courtierprobackend.common.exceptions;

/**
 * Exception thrown when a user is authenticated but not authorized to access a resource.
 * Results in HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super();
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
