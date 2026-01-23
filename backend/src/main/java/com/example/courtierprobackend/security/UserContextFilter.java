package com.example.courtierprobackend.security;

import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter that translates Auth0 user ID from JWT to internal UUID.
 * Runs after Spring Security authentication and attaches the internal UUID
 * to request attributes for use by controllers and services.
 */
@Component
public class UserContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(UserContextFilter.class);

    public static final String INTERNAL_USER_ID_ATTR = "internalUserId";
    public static final String AUTH0_USER_ID_ATTR = "auth0UserId";
    public static final String USER_ROLE_ATTR = "userRole";

    private final UserAccountRepository userAccountRepository;

    public UserContextFilter(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String auth0UserId = jwt.getClaimAsString("sub");

            if (auth0UserId != null) {
                // Store Auth0 ID for reference
                request.setAttribute(AUTH0_USER_ID_ATTR, auth0UserId);

                // Lookup internal UUID
                Optional<UserAccount> userOpt = userAccountRepository.findByAuth0UserId(auth0UserId);

                if (userOpt.isPresent()) {
                    UserAccount user = userOpt.get();
                    // Allow inactive users to access /api/me/confirm-email
                    String path = request.getRequestURI();
                    boolean isEmailConfirm = path != null && path.startsWith("/api/me/confirm-email");
                    if (!user.isActive() && !isEmailConfirm) {
                        logger.warn("Blocked request for inactive user: {}", auth0UserId);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your account is inactive. Please contact support or check your email for confirmation.");
                        return;
                    }
                    UUID internalId = user.getId();
                    request.setAttribute(INTERNAL_USER_ID_ATTR, internalId);
                    request.setAttribute(USER_ROLE_ATTR, user.getRole());
                    logger.debug("Resolved internal user ID {} for Auth0 ID {}", internalId, auth0UserId);
                } else {
                    logger.warn("No UserAccount found for Auth0 ID: {}. User may not be provisioned.", auth0UserId);
                    // User not found - could be a first-time login scenario
                    // For now, let the request proceed without internal ID
                    // Controllers should handle missing internal ID appropriately
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

