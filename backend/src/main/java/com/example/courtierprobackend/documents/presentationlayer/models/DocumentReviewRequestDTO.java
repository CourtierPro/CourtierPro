package com.example.courtierprobackend.documents.presentationlayer.models;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentReviewRequestDTO {
    
    @NotNull(message = "Decision is required")
    private DocumentStatusEnum decision;  
    
    private String comments; 
}