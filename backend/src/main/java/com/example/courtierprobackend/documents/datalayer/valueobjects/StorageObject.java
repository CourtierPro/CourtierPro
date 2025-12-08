package com.example.courtierprobackend.documents.datalayer.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StorageObject {

    private String s3Key;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
}
