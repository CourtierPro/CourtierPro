package com.example.courtierprobackend.notifications.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BroadcastAuditRepository extends JpaRepository<BroadcastAudit, UUID> {
}
