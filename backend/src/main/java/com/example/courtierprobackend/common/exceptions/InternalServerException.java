package com.example.courtierprobackend.common.exceptions;

/**
 * Exception for unexpected internal server errors.
 * Results in HTTP 500 Internal Server Error.
 */
public class InternalServerException extends RuntimeException {

    public InternalServerException() {
        super();
    }

    public InternalServerException(String message) {
        super(message);
    }

    public InternalServerException(Throwable cause) {
        super(cause);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
