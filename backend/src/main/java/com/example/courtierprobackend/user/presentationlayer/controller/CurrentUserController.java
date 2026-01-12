package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.common.exceptions.UnauthorizedException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.mapper.UserMapper;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controller for current user operations.
 * Provides endpoints for the authenticated user to get and update their own profile.
 */
@Slf4j
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private static final Set<String> ALLOWED_LANGUAGES = Set.of("en", "fr");

    private final UserAccountRepository userAccountRepository;
    private final UserMapper userMapper;
    private final Auth0ManagementClient auth0ManagementClient;

    /**
     * Returns the current authenticated user's profile.
     * The response includes the internal UUID which frontend should use for API calls.
     */
    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Get internal UUID from UserContextFilter
        Object internalIdObj = request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR);
        
        if (internalIdObj instanceof UUID internalId) {
            UserAccount account = userAccountRepository.findById(internalId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            return ResponseEntity.ok(userMapper.toResponse(account));
        }

        // Fallback: lookup by Auth0 ID if internal ID not in context
        if (jwt != null) {
            String auth0Id = jwt.getClaimAsString("sub");
            UserAccount account = userAccountRepository.findByAuth0UserId(auth0Id)
                    .orElseThrow(() -> new NotFoundException("User not provisioned. Please contact administrator."));
            return ResponseEntity.ok(userMapper.toResponse(account));
        }

        throw new UnauthorizedException("Authentication required");
    }

    /**
     * Updates the current authenticated user's profile.
     * Currently supports updating: preferredLanguage
     * Also syncs language change to Auth0 for persisting across logins.
     */
    @PatchMapping
    public ResponseEntity<UserResponse> updateCurrentUser(
            HttpServletRequest request,
            @RequestBody Map<String, Object> updates
    ) {
        Object internalIdObj = request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR);

        if (!(internalIdObj instanceof UUID internalId)) {
            throw new UnauthorizedException("Authentication required");
        }

        UserAccount account = userAccountRepository.findById(internalId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String languageUpdated = null;

        // Handle preferredLanguage update
        if (updates.containsKey("preferredLanguage")) {
            String newLanguage = (String) updates.get("preferredLanguage");
            if (newLanguage == null || !ALLOWED_LANGUAGES.contains(newLanguage.toLowerCase())) {
                throw new BadRequestException("Invalid language. Allowed values: en, fr");
            }
            account.setPreferredLanguage(newLanguage.toLowerCase());
            languageUpdated = newLanguage.toLowerCase();
        }

        UserAccount savedAccount = userAccountRepository.save(account);

        // Sync language to Auth0 so it persists across logins
        if (languageUpdated != null && savedAccount.getAuth0UserId() != null) {
            try {
                auth0ManagementClient.updateUserLanguage(savedAccount.getAuth0UserId(), languageUpdated);
            } catch (Exception e) {
                // Log but don't fail the request if Auth0 sync fails
                log.warn("Failed to sync language to Auth0 for user {}: {}", savedAccount.getId(), e.getMessage());
            }
        }

        return ResponseEntity.ok(userMapper.toResponse(savedAccount));
    }
}
