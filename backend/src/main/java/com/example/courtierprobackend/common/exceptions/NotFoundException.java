package com.example.courtierprobackend.common.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 Not Found.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException() {
        super();
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
