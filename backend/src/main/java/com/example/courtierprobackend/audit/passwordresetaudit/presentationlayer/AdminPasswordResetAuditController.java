package com.example.courtierprobackend.audit.passwordresetaudit.presentationlayer;

import com.example.courtierprobackend.audit.passwordresetaudit.businesslayer.PasswordResetAuditService;
import com.example.courtierprobackend.audit.passwordresetaudit.dataaccesslayer.PasswordResetEvent;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin controller to view password reset audit logs
 */
@RestController
@RequestMapping("/api/admin/password-reset-audit")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminPasswordResetAuditController {

    private final PasswordResetAuditService passwordResetAuditService;

    /**
     * Get all password reset events for audit purposes
     * TODO: Implement pagination, filtering by date range, and rate limiting
     * for better performance and security with large datasets
     */
    @GetMapping
    public List<PasswordResetEventResponse> getAllPasswordResetEvents() {
        List<PasswordResetEvent> events = passwordResetAuditService.getAllPasswordResetEvents();
        return events.stream()
                .map(PasswordResetEventResponse::from)
                .toList();
    }
}
