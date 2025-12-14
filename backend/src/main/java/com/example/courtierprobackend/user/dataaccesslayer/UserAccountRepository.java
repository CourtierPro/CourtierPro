package com.example.courtierprobackend.user.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByAuth0UserId(String auth0UserId);
    
    List<UserAccount> findByRole(UserRole role);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM UserAccount u " +
            "JOIN Transaction t ON t.clientId = u.id " +
            "WHERE t.brokerId = :brokerId AND " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<UserAccount> searchClientsOfBroker(@org.springframework.data.repository.query.Param("brokerId") UUID brokerId,
                                            @org.springframework.data.repository.query.Param("query") String query);


}
