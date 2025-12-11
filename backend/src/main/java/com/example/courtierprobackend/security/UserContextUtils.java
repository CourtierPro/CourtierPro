package com.example.courtierprobackend.security;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class UserContextUtils {

    /**
     * Resolves the internal user UUID from the request attributes (set by UserContextFilter).
     * Also supports an optional override ID (typically from a header) for development/testing flexibility.
     *
     * @param request    The HTTP request containing the internalUserId attribute.
     * @param overrideId Optional ID string (e.g. from x-broker-id header). If present, it takes precedence.
     * @return The resolved UUID.
     * @throws ForbiddenException if the user ID cannot be resolved.
     */
    public static UUID resolveUserId(HttpServletRequest request, String overrideId) {
        // DEV mode: header override
        if (StringUtils.hasText(overrideId)) {
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
}
