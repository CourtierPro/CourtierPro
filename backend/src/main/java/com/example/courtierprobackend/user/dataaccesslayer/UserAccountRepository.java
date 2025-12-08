package com.example.courtierprobackend.user.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByAuth0UserId(String auth0UserId);
    
    List<UserAccount> findByRole(UserRole role);
}
