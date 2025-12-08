package com.example.courtierprobackend.audit.presentationlayer;

import com.example.courtierprobackend.audit.businesslayer.LogoutAuditService;
import com.example.courtierprobackend.audit.dataaccesslayer.LogoutAuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LogoutAuditController {

    private final LogoutAuditService logoutAuditService;

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> recordLogout(
            @RequestBody LogoutRequestDto request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        // Extract user info from JWT
        String userId = jwt.getSubject(); // Auth0 'sub' claim
        
        // Email: custom claim → fallback (same pattern as LoginAuditEvent)
        String email = jwt.getClaimAsString("https://courtierpro.dev/email");
        if (email == null || email.isEmpty()) {
            email = jwt.getClaimAsString("email");
        }
        
        // If still no email, use userId as fallback (shouldn't happen in production)
        if (email == null || email.isEmpty()) {
            email = userId;
        }

        // Extract request metadata
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Parse the reason
        LogoutAuditEvent.LogoutReason reason = parseLogoutReason(request.reason());

        // Parse timestamp
        Instant timestamp = request.timestamp() != null
                ? Instant.parse(request.timestamp())
                : Instant.now();

        // Record the logout event
        logoutAuditService.recordLogoutEvent(userId, email, reason, timestamp, ipAddress, userAgent);

        return ResponseEntity.ok(Map.of("message", "Logout event recorded successfully"));
    }

    @GetMapping("/logout-audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LogoutAuditEvent> getAllLogoutEvents() {
        return logoutAuditService.getAllLogoutEvents();
    }

    @GetMapping("/logout-audit/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LogoutAuditEvent> getLogoutEventsByUser(@PathVariable String userId) {
        return logoutAuditService.getLogoutEventsByUser(userId);
    }

    @GetMapping("/logout-audit/reason/{reason}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LogoutAuditEvent> getLogoutEventsByReason(@PathVariable String reason) {
        LogoutAuditEvent.LogoutReason logoutReason = LogoutAuditEvent.LogoutReason.valueOf(reason.toUpperCase());
        return logoutAuditService.getLogoutEventsByReason(logoutReason);
    }

    private LogoutAuditEvent.LogoutReason parseLogoutReason(String reason) {
        if (reason == null) {
            return LogoutAuditEvent.LogoutReason.MANUAL;
        }

        return switch (reason.toLowerCase()) {
            case "session_timeout" -> LogoutAuditEvent.LogoutReason.SESSION_TIMEOUT;
            case "forced" -> LogoutAuditEvent.LogoutReason.FORCED;
            default -> LogoutAuditEvent.LogoutReason.MANUAL;
        };
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Get first IP if multiple
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    // DTO for request body
    public record LogoutRequestDto(String reason, String timestamp) {}
}
