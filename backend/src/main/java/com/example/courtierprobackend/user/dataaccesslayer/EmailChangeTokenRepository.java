package com.example.courtierprobackend.user.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, UUID> {
    Optional<EmailChangeToken> findByTokenAndUsedFalse(String token);
    void deleteByUserId(UUID userId);
}
