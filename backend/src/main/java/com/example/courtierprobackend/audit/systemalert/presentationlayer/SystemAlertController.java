package com.example.courtierprobackend.audit.systemalert.presentationlayer;

import com.example.courtierprobackend.audit.systemalert.businesslayer.SystemAlertService;
import com.example.courtierprobackend.audit.systemalert.dataaccesslayer.SystemAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard/admin/alerts")
@RequiredArgsConstructor
public class SystemAlertController {
    private final SystemAlertService alertService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemAlert>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemAlert> createAlert(@RequestBody CreateAlertRequest request) {
        SystemAlert alert = alertService.createAlert(request.getMessage(), request.getSeverity());
        return ResponseEntity.ok(alert);
    }

    // DTO for alert creation
    @lombok.Data
    public static class CreateAlertRequest {
        private String message;
        private String severity;
    }
}
