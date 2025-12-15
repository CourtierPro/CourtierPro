package com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response showing what will be affected by deleting a resource.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeletionPreviewResponse {

    private UUID resourceId;
    private String resourceType;
    private String resourceSummary; // Human-readable summary of the resource

    /**
     * List of linked resources that will also be soft-deleted.
     */
    private List<LinkedResource> linkedResources;

    /**
     * List of S3 files that will be hard-deleted.
     */
    private List<S3FileToDelete> s3FilesToDelete;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LinkedResource {
        private String type;
        private UUID id;
        private String summary;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class S3FileToDelete {
        private String fileName;
        private String mimeType;
        private Long sizeBytes;
    }
}
