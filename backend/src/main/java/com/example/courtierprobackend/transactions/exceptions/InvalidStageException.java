package com.example.courtierprobackend.transactions.exceptions;

public class InvalidStageException extends RuntimeException {
    public InvalidStageException(String message) {
        super(message);
    }
}
