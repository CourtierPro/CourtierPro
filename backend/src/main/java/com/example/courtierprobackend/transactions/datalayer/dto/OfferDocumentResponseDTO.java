package com.example.courtierprobackend.transactions.datalayer.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferDocumentResponseDTO {

    private UUID documentId;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}
