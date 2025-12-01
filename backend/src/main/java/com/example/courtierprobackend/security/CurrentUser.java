package com.example.courtierprobackend.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(@AuthenticationPrincipal Jwt jwt) {

    public String brokerId() {
        // default Auth0 user identifier
        return jwt.getClaimAsString("sub");
    }

    public String email() {
        return jwt.getClaimAsString("email");
    }

    public String name() {
        return jwt.getClaimAsString("name");
    }
}
