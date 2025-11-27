package com.example.courtierprobackend.transactions.exceptions;


import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransactionControllerExceptionHandler {

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<String> handleInvalidInput(InvalidInputException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage() != null ? ex.getMessage() : "Invalid input");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage() != null ? ex.getMessage() : "Resource not found");
    }
}
