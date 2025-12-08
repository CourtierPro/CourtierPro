package com.example.courtierprobackend.audit.loginaudit.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LoginAuditEventRepository extends JpaRepository<LoginAuditEvent, UUID> {

    List<LoginAuditEvent> findByUserIdOrderByTimestampDesc(String userId);

    List<LoginAuditEvent> findByRoleOrderByTimestampDesc(String role);

    List<LoginAuditEvent> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);

    List<LoginAuditEvent> findAllByOrderByTimestampDesc();
}
