package com.example.courtierprobackend.transactions.datalayer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitorResponseDTO(
        UUID visitorId,
        UUID transactionId,
        String name,
        String email,
        String phoneNumber,
        int timesVisited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
