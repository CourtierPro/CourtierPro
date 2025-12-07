package com.example.courtierprobackend.audit.businesslayer;

import com.example.courtierprobackend.audit.dataaccesslayer.PasswordResetEvent;
import com.example.courtierprobackend.audit.dataaccesslayer.PasswordResetEventRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service to record password reset events for audit purposes.
 */
@Service
@AllArgsConstructor
public class PasswordResetAuditService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetAuditService.class);

    private final PasswordResetEventRepository passwordResetEventRepository;

    /**
     * Record a password reset request event
     */
    @Transactional
    public void recordPasswordResetRequest(
            String userId,
            String email,
            String ipAddress,
            String userAgent
    ) {
        PasswordResetEvent event = PasswordResetEvent.builder()
                .userId(userId)
                .email(email)
                .eventType(PasswordResetEvent.ResetEventType.REQUESTED)
                .timestamp(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        passwordResetEventRepository.save(event);

        logger.info("Password reset REQUESTED for user {} ({})", userId, email);
    }

    /**
     * Record a successful password reset completion event
     */
    @Transactional
    public void recordPasswordResetCompletion(
            String userId,
            String email,
            String ipAddress,
            String userAgent
    ) {
        PasswordResetEvent event = PasswordResetEvent.builder()
                .userId(userId)
                .email(email)
                .eventType(PasswordResetEvent.ResetEventType.COMPLETED)
                .timestamp(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        passwordResetEventRepository.save(event);

        logger.info("Password reset COMPLETED for user {} ({})", userId, email);
    }

    /**
     * Get all password reset events (for admin audit)
     */
    public List<PasswordResetEvent> getAllPasswordResetEvents() {
        return passwordResetEventRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Get password reset events for a specific user
     */
    public List<PasswordResetEvent> getPasswordResetEventsForUser(String userId) {
        return passwordResetEventRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
