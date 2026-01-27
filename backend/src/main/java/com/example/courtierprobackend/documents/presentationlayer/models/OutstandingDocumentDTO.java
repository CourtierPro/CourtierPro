package com.example.courtierprobackend.documents.presentationlayer.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutstandingDocumentDTO {
    private UUID id; // Public Request ID
    private String title;
    private String transactionAddress;
    private String clientName;
    private String clientEmail;
    private LocalDateTime dueDate;
    private Integer daysOutstanding;
    private String status;
}
