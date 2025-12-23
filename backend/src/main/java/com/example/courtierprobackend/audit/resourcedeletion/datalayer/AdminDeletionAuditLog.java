package com.example.courtierprobackend.audit.resourcedeletion.datalayer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log for admin resource operations (delete and restore).
 * Records who performed what action, when, and what was affected.
 */
@Entity
@Table(name = "admin_deletion_audit_logs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDeletionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType action;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "resource_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    /**
     * JSON snapshot of the resource's key metadata.
     * For documents: type, filename, size, hash (non-sensitive only).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_snapshot", columnDefinition = "jsonb")
    private String resourceSnapshot;

    /**
     * JSON list of cascaded operations (linked resources also affected).
     * Example: ["TimelineEntry:uuid", "SubmittedDocument:uuid"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cascaded_deletions", columnDefinition = "jsonb")
    private String cascadedDeletions;

    public enum ActionType {
        DELETE,
        RESTORE
    }

    public enum ResourceType {
        TRANSACTION,
        DOCUMENT_REQUEST,
        SUBMITTED_DOCUMENT,
        TIMELINE_ENTRY,
        APPOINTMENT
    }
}
