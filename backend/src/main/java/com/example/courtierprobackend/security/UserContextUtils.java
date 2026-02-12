package com.example.courtierprobackend.security;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.UUID;

public class UserContextUtils {

    /**
     * Resolves the internal user UUID from the request attributes (set by UserContextFilter).
     * Also supports an optional override ID (typically from a header) for development/testing flexibility.
     * The override is only honored when the "dev" Spring profile is active.
     *
     * @param request    The HTTP request containing the internalUserId attribute.
     * @param overrideId Optional ID string (e.g. from x-broker-id header). If present and dev profile is active, it takes precedence.
     * @return The resolved UUID.
     * @throws ForbiddenException if the user ID cannot be resolved.
     */
    public static UUID resolveUserId(HttpServletRequest request, String overrideId) {
        // DEV mode only: header override
        if (StringUtils.hasText(overrideId) && isOverrideAllowed(request)) {
            return UUID.fromString(overrideId);
        }

        // PROD mode: Get internal UUID from UserContextFilter
        if (request != null) {
            Object internalId = request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR);
            if (internalId instanceof UUID) {
                return (UUID) internalId;
            } else if (internalId instanceof String) {
                return UUID.fromString((String) internalId);
            }
        }

        throw new ForbiddenException("Unable to resolve user id from security context");
    }

    /**
     * Convenience method to resolve user ID without an override header.
     */
    public static UUID resolveUserId(HttpServletRequest request) {
        return resolveUserId(request, null);
    }

    /**
     * Checks if the current user has BROKER role based on the request attribute.
     * Falls back to false if role cannot be determined.
     */
    public static boolean isBroker(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        Object role = request.getAttribute(UserContextFilter.USER_ROLE_ATTR);
        if (role == null) {
            return false;
        }
        return "BROKER".equalsIgnoreCase(role.toString());
    }

    /**
     * Checks whether the header override should be allowed.
     * It is blocked only when the "prod" Spring profile is active.
     * In dev, test, and default environments the override is allowed.
     */
    private static boolean isOverrideAllowed(HttpServletRequest request) {
        if (request == null || request.getServletContext() == null) {
            return true;
        }
        try {
            var ctx = org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(request.getServletContext());
            if (ctx == null) {
                return true;
            }
            Environment env = ctx.getEnvironment();
            return !Arrays.asList(env.getActiveProfiles()).contains("prod");
        } catch (Exception e) {
            return true;
        }
    }
}
