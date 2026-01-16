package com.example.courtierprobackend.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Represents the current authenticated user with both Auth0 and internal identities.
 * The internal UUID is resolved by UserContextFilter and stored in request attributes.
 */
public record CurrentUser(@AuthenticationPrincipal Jwt jwt) {

    /**
     * Returns the internal UUID for the current user.
     * This is the ID that should be used for all database operations.
     *
     * @return the internal UUID, or null if user is not provisioned
     */
    public UUID internalId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            Object id = attrs.getRequest().getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR);
            if (id instanceof UUID) {
                return (UUID) id;
            }
        }
        return null;
    }

    /**
     * Returns the internal UUID as a String for compatibility with existing code.
     *
     * @return the internal UUID as String, or null if user is not provisioned
     */
    public String internalIdString() {
        UUID id = internalId();
        return id != null ? id.toString() : null;
    }

    /**
     * Returns the Auth0 user ID (the JWT 'sub' claim).
     * Use this only for Auth0 API calls, never for database storage.
     *
     * @return the Auth0 user ID
     */
    public String auth0Id() {
        return jwt.getClaimAsString("sub");
    }

    /**
     * @deprecated Use {@link #auth0Id()} or {@link #internalId()} instead.
     * This method returns Auth0 ID for backward compatibility.
     */
    @Deprecated
    public String brokerId() {
        // For backward compatibility, return Auth0 ID
        // Controllers should migrate to use internalId() or internalIdString()
        return auth0Id();
    }

    public String email() {
        return jwt.getClaimAsString("email");
    }

    public String name() {
        return jwt.getClaimAsString("name");
    }
}
