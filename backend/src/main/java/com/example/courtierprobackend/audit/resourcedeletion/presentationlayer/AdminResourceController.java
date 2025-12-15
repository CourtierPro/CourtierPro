package com.example.courtierprobackend.audit.resourcedeletion.presentationlayer;

import com.example.courtierprobackend.audit.resourcedeletion.businesslayer.AdminResourceService;
import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditLog;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.AdminDeletionAuditResponse;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.DeletionPreviewResponse;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.ResourceListResponse;
import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin-only controller for resource management and soft-deletion.
 */
@RestController
@RequestMapping("/api/admin/resources")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminResourceController {

    private final AdminResourceService adminResourceService;
    private final UserAccountRepository userAccountRepository;

    /**
     * List all resources of a given type.
     *
     * @param type           Resource type (TRANSACTION, DOCUMENT_REQUEST)
     * @param includeDeleted If true, includes soft-deleted resources
     */
    @GetMapping("/{type}")
    public ResponseEntity<ResourceListResponse> listResources(
            @PathVariable String type,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        AdminDeletionAuditLog.ResourceType resourceType = parseResourceType(type);
        ResourceListResponse response = adminResourceService.listResources(resourceType, includeDeleted);
        return ResponseEntity.ok(response);
    }

    /**
     * Preview the impact of deleting a resource.
     * Shows linked resources and S3 files that will be affected.
     */
    @GetMapping("/{type}/{resourceId}/preview")
    public ResponseEntity<DeletionPreviewResponse> previewDeletion(
            @PathVariable String type,
            @PathVariable UUID resourceId) {

        AdminDeletionAuditLog.ResourceType resourceType = parseResourceType(type);
        DeletionPreviewResponse preview = adminResourceService.previewDeletion(resourceType, resourceId);
        return ResponseEntity.ok(preview);
    }

    /**
     * Soft-delete a resource and hard-delete associated S3 files.
     */
    @DeleteMapping("/{type}/{resourceId}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable String type,
            @PathVariable UUID resourceId,
            @RequestParam(defaultValue = "false") boolean confirmed,
            HttpServletRequest request) {

        if (!confirmed) {
            return ResponseEntity.badRequest().build();
        }

        AdminDeletionAuditLog.ResourceType resourceType = parseResourceType(type);
        UUID adminId = UserContextUtils.resolveUserId(request);

        adminResourceService.deleteResource(resourceType, resourceId, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted resource.
     * Note: S3 files cannot be recovered; only metadata is restored.
     */
    @PostMapping("/{type}/{resourceId}/restore")
    public ResponseEntity<Void> restoreResource(
            @PathVariable String type,
            @PathVariable UUID resourceId,
            HttpServletRequest request) {

        AdminDeletionAuditLog.ResourceType resourceType = parseResourceType(type);
        UUID adminId = UserContextUtils.resolveUserId(request);

        adminResourceService.restoreResource(resourceType, resourceId, adminId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get audit history (both delete and restore actions).
     */
    @GetMapping("/audit-history")
    public ResponseEntity<List<AdminDeletionAuditResponse>> getAuditHistory() {
        List<AdminDeletionAuditLog> logs = adminResourceService.getAuditHistory();

        List<AdminDeletionAuditResponse> response = logs.stream()
                .map(log -> {
                    String adminEmail = userAccountRepository.findById(log.getAdminId())
                            .map(u -> u.getEmail())
                            .orElse("Unknown");

                    return AdminDeletionAuditResponse.builder()
                            .id(log.getId())
                            .action(log.getAction().name())
                            .timestamp(log.getTimestamp())
                            .adminId(log.getAdminId())
                            .adminEmail(adminEmail)
                            .resourceType(log.getResourceType().name())
                            .resourceId(log.getResourceId())
                            .resourceSnapshot(log.getResourceSnapshot())
                            .cascadedDeletions(log.getCascadedDeletions())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private AdminDeletionAuditLog.ResourceType parseResourceType(String type) {
        try {
            return AdminDeletionAuditLog.ResourceType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid resource type: " + type +
                    ". Valid types: TRANSACTION, DOCUMENT_REQUEST");
        }
    }
}
