package com.example.courtierprobackend.security;

import com.example.courtierprobackend.audit.businesslayer.LoginAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

@Component
@AllArgsConstructor
public class AuthenticationEventListener {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final LoginAuditService loginAuditService;

    // Trusted proxy IPs (can be extended later or externalized to config)
    private static final Set<String> TRUSTED_PROXIES =
            Set.of("127.0.0.1", "::1");

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Object principal = event.getAuthentication().getPrincipal();

            if (!(principal instanceof Jwt jwt)) {
                return;
            }

            String userId = jwt.getSubject();

            // Email: custom claim → fallback
            String email = jwt.getClaimAsString("https://courtierpro.dev/email");
            if (email == null || email.isEmpty()) {
                email = jwt.getClaimAsString("email");
            }

            if (email == null) {
                logger.warn("Email claim not found in JWT for user {}", userId);
                return;
            }

            // Role extraction
            Object rolesClaim = jwt.getClaim("https://courtierpro.dev/roles");
            String role = "UNKNOWN";

            if (rolesClaim instanceof java.util.List<?> roles && !roles.isEmpty()) {
                role = roles.get(0).toString();
            }

            HttpServletRequest request = getCurrentRequest();
            String ipAddress = extractIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            loginAuditService.recordLoginEvent(
                    userId,
                    email,
                    role,
                    ipAddress,
                    userAgent
            );

        } catch (Exception e) {
            // IMPORTANT: audit must never break authentication
            logger.error("Failed to record login audit event", e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * Safely extracts IP address.
     * X-Forwarded-For is trusted ONLY if request came through a trusted proxy.
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String remoteAddr = request.getRemoteAddr();

        if (TRUSTED_PROXIES.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // XFF may contain multiple IPs, take the first
                return xff.split(",")[0].trim();
            }
        }

        return remoteAddr;
    }
}
