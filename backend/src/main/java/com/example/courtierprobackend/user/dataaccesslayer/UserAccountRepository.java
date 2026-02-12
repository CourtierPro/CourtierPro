package com.example.courtierprobackend.user.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

        Optional<UserAccount> findByEmail(String email);

        Optional<UserAccount> findByAuth0UserId(String auth0UserId);

        List<UserAccount> findByRole(UserRole role);

        List<UserAccount> findByActiveTrue();

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM UserAccount u " +
                        "WHERE (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
                        "EXISTS (SELECT t FROM Transaction t WHERE (t.clientId = u.id OR t.brokerId = u.id) AND (t.brokerId = :brokerId OR t.clientId = :brokerId))")
        List<UserAccount> searchClientsOfBroker(
                        @org.springframework.data.repository.query.Param("brokerId") UUID brokerId,
                        @org.springframework.data.repository.query.Param("query") String query);

}
