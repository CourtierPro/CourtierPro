package com.example.courtierprobackend.transactions.exceptions;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Structured error response DTO for consistent API error responses.
 */
@Data
@Builder
public class ErrorResponse {
    private String error;
    private String code;
    private Instant timestamp;
    private String path;
    
    public static ErrorResponse of(String error, String code) {
        return ErrorResponse.builder()
                .error(error)
                .code(code)
                .timestamp(Instant.now())
                .build();
    }
    
    public static ErrorResponse of(String error, String code, String path) {
        return ErrorResponse.builder()
                .error(error)
                .code(code)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }
}
