package com.example.courtierprobackend.audit.resourcedeletion.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AdminDeletionAuditRepository extends JpaRepository<AdminDeletionAuditLog, Long> {

    List<AdminDeletionAuditLog> findByResourceTypeOrderByTimestampDesc(AdminDeletionAuditLog.ResourceType type);

    List<AdminDeletionAuditLog> findByAdminIdOrderByTimestampDesc(UUID adminId);

    List<AdminDeletionAuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime from, LocalDateTime to);

    List<AdminDeletionAuditLog> findAllByOrderByTimestampDesc();
    
    List<AdminDeletionAuditLog> findByActionOrderByTimestampDesc(AdminDeletionAuditLog.ActionType action);
}

