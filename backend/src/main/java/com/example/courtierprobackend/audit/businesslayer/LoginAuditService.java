package com.example.courtierprobackend.audit.businesslayer;

import com.example.courtierprobackend.audit.dataaccesslayer.LoginAuditEvent;
import com.example.courtierprobackend.audit.dataaccesslayer.LoginAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditEventRepository repository;

    public void recordLoginEvent(String userId, String email, String role, String ipAddress, String userAgent) {
        LoginAuditEvent event = LoginAuditEvent.builder()
                .userId(userId)
                .email(email)
                .role(role)
                .timestamp(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        repository.save(event);
    }

    public List<LoginAuditEvent> getAllLoginEvents() {
        return repository.findAllByOrderByTimestampDesc();
    }

    public List<LoginAuditEvent> getLoginEventsByUser(String userId) {
        return repository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<LoginAuditEvent> getLoginEventsByRole(String role) {
        return repository.findByRoleOrderByTimestampDesc(role);
    }
}
