package com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for an audit log entry (delete or restore action).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDeletionAuditResponse {

    private Long id;
    private String action;  // DELETE or RESTORE
    private LocalDateTime timestamp;
    private UUID adminId;
    private String adminEmail; // Resolved from adminId for display
    private String resourceType;
    private UUID resourceId;
    private String resourceSnapshot;
    private String cascadedDeletions;
}

