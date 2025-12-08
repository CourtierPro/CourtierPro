package com.example.courtierprobackend.audit.loginaudit.presentationlayer;

import com.example.courtierprobackend.audit.loginaudit.businesslayer.LoginAuditService;
import com.example.courtierprobackend.audit.loginaudit.dataaccesslayer.LoginAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/login-audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class LoginAuditController {

    private final LoginAuditService loginAuditService;

    @GetMapping
    public List<LoginAuditEvent> getAllLoginEvents() {
        return loginAuditService.getAllLoginEvents();
    }

    @GetMapping("/user/{userId}")
    public List<LoginAuditEvent> getLoginEventsByUser(@PathVariable String userId) {
        return loginAuditService.getLoginEventsByUser(userId);
    }

    @GetMapping("/role/{role}")
    public List<LoginAuditEvent> getLoginEventsByRole(@PathVariable String role) {
        return loginAuditService.getLoginEventsByRole(role);
    }
}
