package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.common.exceptions.UnauthorizedException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.businesslayer.EmailChangeService;
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
    private final EmailChangeService emailChangeService;

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
            @RequestBody com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest
    ) {
        Object internalIdObj = request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR);
        if (!(internalIdObj instanceof UUID internalId)) {
            throw new UnauthorizedException("Authentication required");
        }
        UserAccount account = userAccountRepository.findById(internalId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String languageUpdated = null;
        boolean emailChanged = false;
        String oldEmail = account.getEmail();


        // Handle email update (confirmation flow required)
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(account.getEmail())) {
            // Start email change flow: send confirmation, do not update email yet
            emailChangeService.initiateEmailChange(account, updateRequest.getEmail());
            account.setActive(false); // Optionally deactivate until confirmed
            userAccountRepository.save(account); // Persist the deactivation
            emailChanged = true;
        }

        // Handle notification preferences
        if (updateRequest.getEmailNotificationsEnabled() != null) {
            account.setEmailNotificationsEnabled(updateRequest.getEmailNotificationsEnabled());
        }
        if (updateRequest.getInAppNotificationsEnabled() != null) {
            account.setInAppNotificationsEnabled(updateRequest.getInAppNotificationsEnabled());
        }

        // Handle preferredLanguage update
        if (updateRequest.getPreferredLanguage() == null) {
            throw new BadRequestException("Invalid language. Allowed values: en, fr");
        } else {
            String newLanguage = updateRequest.getPreferredLanguage();
            if (!ALLOWED_LANGUAGES.contains(newLanguage.toLowerCase())) {
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
                log.warn("Failed to sync language to Auth0 for user {}: {}", savedAccount.getId(), e.getMessage());
            }
        }

        // If email changed, trigger confirmation email (placeholder)
        if (emailChanged) {
            // TODO: Send confirmation email to new address
            // emailService.sendEmailChangeConfirmation(savedAccount, oldEmail, updateRequest.getEmail());
        }

        return ResponseEntity.ok(userMapper.toResponse(savedAccount));
    }
    /**
     * Returns whether the current user has MFA enabled in Auth0.
     */
    @GetMapping("/mfa-status")
    public ResponseEntity<Map<String, Object>> getMfaStatus(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String auth0Id = null;
        Object internalIdObj = request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR);
        if (internalIdObj instanceof UUID internalId) {
            UserAccount account = userAccountRepository.findById(internalId)
                    .orElse(null);
            if (account != null) {
                auth0Id = account.getAuth0UserId();
            }
        }
        if (auth0Id == null && jwt != null) {
            auth0Id = jwt.getClaimAsString("sub");
        }
        if (auth0Id == null) {
            throw new UnauthorizedException("Authentication required");
        }

        boolean mfaEnabled = false;
        try {
            mfaEnabled = auth0ManagementClient.isMfaEnabled(auth0Id);
        } catch (Exception e) {
            log.warn("Failed to check MFA status for user {}: {}", auth0Id, e.getMessage());
        }
        return ResponseEntity.ok(Map.of("mfaEnabled", mfaEnabled));
    }

    /**
     * Endpoint to confirm email change via token (GET /api/me/confirm-email?token=...)
     */
    @GetMapping("/confirm-email")
    public ResponseEntity<String> confirmEmailChange(@RequestParam("token") String token) {
        boolean success = emailChangeService.confirmEmailChange(token);
        if (success) {
            return ResponseEntity.ok("Email address confirmed and updated.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }
    }
}
