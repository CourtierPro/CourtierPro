package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.common.exceptions.UnauthorizedException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.mapper.UserMapper;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller for current user operations.
 * Provides endpoints for the authenticated user to get their own profile.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final UserAccountRepository userAccountRepository;
    private final UserMapper userMapper;

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
}
