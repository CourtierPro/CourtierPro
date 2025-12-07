package com.example.courtierprobackend.audit.presentationlayer.controller;

import com.example.courtierprobackend.audit.businesslayer.PasswordResetAuditService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller to receive Auth0 Log Stream events.
 * 
 * This endpoint receives events from Auth0 when:
 * - A user requests a password reset (event type: "fp" - forgot password)
 * - A user completes a password change (event type: "scp" - successful change password)
 * 
 * To set up in Auth0:
 * 1. Go to Monitoring > Streams > Create Stream > Custom Webhook
 * 2. Configure endpoint: https://your-backend.com/api/webhooks/auth0-events
 * 3. Filter by event types: fp (forgot password), scp (successful change password)
 * 4. Save the webhook
 */
@RestController
@RequestMapping("/api/webhooks")
@AllArgsConstructor
public class Auth0WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(Auth0WebhookController.class);
    
    private final PasswordResetAuditService passwordResetAuditService;

    /**
     * Receive Auth0 log stream events
     * 
     * Auth0 Event Types:
     * - "fp": Forgot Password request
     * - "scp": Successful Change Password
     * - "fcp": Failed Change Password
     */
    @PostMapping("/auth0-events")
    public ResponseEntity<Void> handleAuth0Event(
            @RequestBody Auth0LogEvent event,
            HttpServletRequest request
    ) {
        try {
            logger.info("Received Auth0 event: type={}, user={}", event.getType(), event.getUserId());

            // Extract IP from event payload (user's actual IP), with fallbacks to headers
            String ipAddress = extractIpAddress(event, request);
            // Use user agent from Auth0 event if available, otherwise from request header
            String userAgent = (event.getUserAgent() != null && !event.getUserAgent().isEmpty()) 
                    ? event.getUserAgent() 
                    : request.getHeader("User-Agent");

            // Handle different event types
            switch (event.getType()) {
                case "fp": // Forgot Password - user requested reset
                    if (event.getUserId() != null && event.getUserEmail() != null) {
                        passwordResetAuditService.recordPasswordResetRequest(
                                event.getUserId(),
                                event.getUserEmail(),
                                ipAddress,
                                userAgent
                        );
                    }
                    break;

                case "scp": // Successful Change Password - user completed reset
                    if (event.getUserId() != null && event.getUserEmail() != null) {
                        passwordResetAuditService.recordPasswordResetCompletion(
                                event.getUserId(),
                                event.getUserEmail(),
                                ipAddress,
                                userAgent
                        );
                    }
                    break;

                case "fcp": // Failed Change Password - log but don't record as audit
                    logger.warn("Failed password change attempt for user: {}", event.getUserId());
                    break;

                default:
                    logger.debug("Unhandled Auth0 event type: {}", event.getType());
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error processing Auth0 webhook event", e);
            // Return 200 anyway to prevent Auth0 from retrying
            return ResponseEntity.ok().build();
        }
    }

    private String extractIpAddress(Auth0LogEvent event, HttpServletRequest request) {
        // Use IP from Auth0 event payload (actual user's IP)
        if (event.getIp() != null && !event.getIp().isEmpty()) {
            return event.getIp();
        }
        
        // Fallback to X-Forwarded-For header
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        
        // Final fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * DTO for Auth0 log stream events
     */
    @Data
    public static class Auth0LogEvent {
        
        @JsonProperty("log_id")
        private String logId;
        
        @JsonProperty("type")
        private String type;  // Event type code (fp, scp, etc.)
        
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("user_name")
        private String userName;
        
        @JsonProperty("connection")
        private String connection;
        
        @JsonProperty("client_id")
        private String clientId;
        
        @JsonProperty("client_name")
        private String clientName;
        
        @JsonProperty("ip")
        private String ip;
        
        @JsonProperty("user_agent")
        private String userAgent;
        
        @JsonProperty("description")
        private String description;
        
        // Email might be in different fields depending on event
        @JsonProperty("email")
        private String email;
        
        // Fallback method to get email with proper validation
        public String getUserEmail() {
            if (email != null && !email.isEmpty() && isValidEmail(email)) {
                return email;
            }
            if (userName != null && isValidEmail(userName)) {
                return userName;
            }
            return null;
        }
        
        private boolean isValidEmail(String email) {
            if (email == null || email.isEmpty()) {
                return false;
            }
            // RFC 5322 compliant email regex pattern
            String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
            return email.matches(emailRegex);
        }
    }
}
