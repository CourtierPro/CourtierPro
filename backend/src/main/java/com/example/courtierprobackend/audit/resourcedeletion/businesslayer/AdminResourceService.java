package com.example.courtierprobackend.audit.resourcedeletion.businesslayer;

import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditLog;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.DeletionPreviewResponse;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.ResourceListResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service for admin resource management and soft-deletion with S3 cleanup.
 */
public interface AdminResourceService {

    /**
     * List all resources of a given type (excluding soft-deleted by default).
     */
    ResourceListResponse listResources(AdminDeletionAuditLog.ResourceType type, boolean includeDeleted);

    /**
     * Preview what will be affected by deleting a resource.
     * Shows the resource details and cascaded deletions without executing.
     */
    DeletionPreviewResponse previewDeletion(AdminDeletionAuditLog.ResourceType type, UUID resourceId);

    /**
     * Soft-delete a resource and hard-delete associated S3 files.
     * Creates an immutable audit log entry.
     *
     * @param type      The resource type
     * @param resourceId The resource UUID
     * @param adminId   The admin performing the deletion
     */
    void deleteResource(AdminDeletionAuditLog.ResourceType type, UUID resourceId, UUID adminId);

    /**
     * Restore a soft-deleted resource (if S3 files are not recoverable,
     * only metadata is restored).
     */
    void restoreResource(AdminDeletionAuditLog.ResourceType type, UUID resourceId, UUID adminId);

    /**
     * Get deletion audit history.
     */
    List<AdminDeletionAuditLog> getAuditHistory();
}
