package com.example.courtierprobackend.audit.logoutaudit.businesslayer;

import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEvent;
import com.example.courtierprobackend.audit.logoutaudit.dataaccesslayer.LogoutAuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutAuditService {

    private final LogoutAuditEventRepository repository;

    public void recordLogoutEvent(
            String userId,
            String email,
            LogoutAuditEvent.LogoutReason reason,
            Instant timestamp,
            String ipAddress,
            String userAgent
    ) {
        try {
            LogoutAuditEvent event = LogoutAuditEvent.builder()
                    .userId(userId)
                    .email(email)
                    .reason(reason)
                    .timestamp(timestamp)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            repository.save(event);
            log.info("Logout event recorded for user: {} with reason: {}", email, reason);
        } catch (Exception e) {
            log.error("Failed to record logout event for user: {}", email, e);
            // Don't throw exception - logout should succeed even if logging fails
        }
    }

    public List<LogoutAuditEvent> getAllLogoutEvents() {
        return repository.findAllByOrderByTimestampDesc();
    }

    public List<LogoutAuditEvent> getLogoutEventsByUser(String userId) {
        return repository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<LogoutAuditEvent> getLogoutEventsByReason(LogoutAuditEvent.LogoutReason reason) {
        return repository.findByReasonOrderByTimestampDesc(reason);
    }
}
