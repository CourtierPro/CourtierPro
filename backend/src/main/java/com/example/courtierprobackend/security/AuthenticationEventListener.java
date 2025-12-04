package com.example.courtierprobackend.security;

import com.example.courtierprobackend.audit.businesslayer.LoginAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@AllArgsConstructor
public class AuthenticationEventListener {

    private final LoginAuditService loginAuditService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            // Get JWT from authentication
            Object principal = event.getAuthentication().getPrincipal();

            if (principal instanceof Jwt jwt) {
                String userId = jwt.getSubject();
                
                // Try custom claim first, then standard email claim
                String email = jwt.getClaimAsString("https://courtierpro.dev/email");
                if (email == null || email.isEmpty()) {
                    email = jwt.getClaimAsString("email");
                }
                
                // Skip if email is still null
                if (email == null) {
                    System.err.println("Email claim not found in JWT for user: " + userId);
                    return;
                }

                // Extract role from custom claim
                Object rolesClaim = jwt.getClaim("https://courtierpro.dev/roles");
                String role = "UNKNOWN";

                if (rolesClaim instanceof java.util.List<?> roles && !roles.isEmpty()) {
                    role = roles.get(0).toString();
                }

                // Get request details
                HttpServletRequest request = getCurrentRequest();
                String ipAddress = extractIpAddress(request);
                String userAgent = request != null ? request.getHeader("User-Agent") : null;

                // Record the login event
                loginAuditService.recordLoginEvent(userId, email, role, ipAddress, userAgent);
            }
        } catch (Exception e) {
            // Don't fail authentication if audit logging fails
            System.err.println("Failed to record login audit event: " + e.getMessage());
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) return null;

        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first one
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}
