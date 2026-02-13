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
                        "EXISTS (SELECT t FROM Transaction t WHERE t.clientId = u.id AND t.brokerId = :brokerId)")
        List<UserAccount> searchClientsOfBroker(
                        @org.springframework.data.repository.query.Param("brokerId") UUID brokerId,
                        @org.springframework.data.repository.query.Param("query") String query);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u.id FROM UserAccount u WHERE " +
                        "u.id IN (SELECT t.clientId FROM Transaction t WHERE t.brokerId = :brokerId) AND " +
                        "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
        List<UUID> findIdsBySearchQuery(
                        @org.springframework.data.repository.query.Param("brokerId") UUID brokerId,
                        @org.springframework.data.repository.query.Param("query") String query);

        List<UserAccount> findByRoleAndWeeklyDigestEnabledTrueAndActiveTrue(UserRole role);

}
