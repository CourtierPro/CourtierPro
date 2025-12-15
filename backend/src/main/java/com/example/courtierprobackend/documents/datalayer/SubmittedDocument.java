package com.example.courtierprobackend.documents.datalayer;

import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.UploadedBy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "submitted_documents")
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubmittedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID documentId; // Public ID for this specific submission

    private LocalDateTime uploadedAt;

    @Embedded
    private UploadedBy uploadedBy;

    @Embedded
    private StorageObject storageObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_request_id")
    private DocumentRequest documentRequest;

    // Soft delete fields
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}

