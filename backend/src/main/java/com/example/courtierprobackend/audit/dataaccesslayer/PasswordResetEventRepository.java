package com.example.courtierprobackend.audit.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PasswordResetEventRepository extends JpaRepository<PasswordResetEvent, UUID> {
    
    /**
     * Find all password reset events ordered by timestamp descending (newest first)
     */
    List<PasswordResetEvent> findAllByOrderByTimestampDesc();
    
    /**
     * Find all password reset events for a specific user
     */
    List<PasswordResetEvent> findByUserIdOrderByTimestampDesc(String userId);
}
