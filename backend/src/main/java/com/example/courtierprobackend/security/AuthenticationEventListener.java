package com.example.courtierprobackend.security;

import com.example.courtierprobackend.audit.loginaudit.businesslayer.LoginAuditService;
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

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthenticationEventListener {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final LoginAuditService loginAuditService;
    
    // Track which tokens have already been audited to avoid duplicate entries
    // Key is subject + issued-at timestamp to uniquely identify a login session
    private final Set<String> auditedTokens = ConcurrentHashMap.newKeySet();
    
    // Limit cache size to prevent memory issues (tokens expire anyway)
    private static final int MAX_CACHE_SIZE = 10000;

    // Trusted proxy IPs (can be extended later or externalized to config)
    private static final Set<String> TRUSTED_PROXIES =
            Set.of("127.0.0.1", "::1");

    public AuthenticationEventListener(LoginAuditService loginAuditService) {
        this.loginAuditService = loginAuditService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Object principal = event.getAuthentication().getPrincipal();

            if (!(principal instanceof Jwt jwt)) {
                return;
            }

            String userId = jwt.getSubject();
            Instant issuedAt = jwt.getIssuedAt();
            
            // Create unique key for this token session
            String tokenKey = userId + ":" + (issuedAt != null ? issuedAt.toEpochMilli() : "unknown");
            
            // Prevent unbounded cache growth - evict oldest entries before adding new one
            if (auditedTokens.size() >= MAX_CACHE_SIZE) {
                // Remove approximately 10% of entries to avoid frequent evictions
                int toRemove = MAX_CACHE_SIZE / 10;
                var iterator = auditedTokens.iterator();
                while (iterator.hasNext() && toRemove > 0) {
                    iterator.next();
                    iterator.remove();
                    toRemove--;
                }
                logger.debug("Evicted entries from login audit token cache due to size limit");
            }
            
            // Only record audit if we haven't seen this token before
            if (!auditedTokens.add(tokenKey)) {
                // Token already audited, skip duplicate
                return;
            }

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

