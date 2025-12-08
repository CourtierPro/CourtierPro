package com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LogoutAuditEventRepository extends JpaRepository<LogoutAuditEvent, UUID> {

    List<LogoutAuditEvent> findByUserIdOrderByTimestampDesc(String userId);

    List<LogoutAuditEvent> findByReasonOrderByTimestampDesc(LogoutAuditEvent.LogoutReason reason);

    List<LogoutAuditEvent> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);

    List<LogoutAuditEvent> findAllByOrderByTimestampDesc();
}
