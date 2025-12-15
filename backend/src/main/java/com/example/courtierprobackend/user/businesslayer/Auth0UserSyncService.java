package com.example.courtierprobackend.user.businesslayer;

import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.Auth0User;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.Auth0Role;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service to synchronize Auth0 users with the local database.
 * Runs on every startup to ensure local DB matches Auth0.
 */
@Service
@RequiredArgsConstructor
public class Auth0UserSyncService {

    private static final Logger log = LoggerFactory.getLogger(Auth0UserSyncService.class);
    
    // Delay between Auth0 API calls to avoid rate limiting (150ms = ~6 requests/sec, well under Auth0's limit)
    private static final long RATE_LIMIT_DELAY_MS = 150;

    private final Auth0ManagementClient auth0Client;
    private final UserAccountRepository userRepository;

    /**
     * Fetches all users from Auth0 and syncs them to the local database.
     * - Creates new users that don't exist locally
     * - Updates existing users with latest Auth0 data
     * - Deactivates users that were deleted from Auth0
     */
    public void syncUsersFromAuth0() {
        log.info("Starting Auth0 user synchronization...");

        try {
            List<Auth0User> auth0Users = auth0Client.listAllUsers();
            log.info("Fetched {} users from Auth0", auth0Users.size());

            // Track which Auth0 user IDs we've seen
            Set<String> auth0UserIds = new HashSet<>();

            int created = 0;
            int updated = 0;
            int skipped = 0;

            for (Auth0User auth0User : auth0Users) {
                try {
                    auth0UserIds.add(auth0User.userId());
                    
                    // Rate limit: wait before fetching roles to avoid 429 errors
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                    
                    // Fetch roles for this user
                    List<Auth0Role> roles = auth0Client.getUserRoles(auth0User.userId());
                    UserRole userRole = auth0Client.mapRoleToUserRole(roles);

                    // Check if user exists by auth0UserId first
                    Optional<UserAccount> existingByAuth0Id = userRepository.findByAuth0UserId(auth0User.userId());
                    
                    // Also check by email (in case old data exists with different auth0 ID)
                    Optional<UserAccount> existingByEmail = auth0User.email() != null 
                            ? userRepository.findByEmail(auth0User.email()) 
                            : Optional.empty();

                    if (existingByAuth0Id.isPresent()) {
                        // User exists with matching auth0UserId - update if needed
                        UserAccount user = existingByAuth0Id.get();
                        boolean changed = updateUserIfNeeded(user, auth0User, userRole);
                        
                        // Reactivate if they were previously deactivated
                        if (!user.isActive()) {
                            user.setActive(true);
                            changed = true;
                            log.info("Reactivated user: {}", auth0User.email());
                        }
                        
                        if (changed) {
                            userRepository.save(user);
                            updated++;
                        } else {
                            skipped++;
                        }
                    } else if (existingByEmail.isPresent()) {
                        // User exists with matching email but different auth0UserId
                        // Update the auth0UserId to the real one from Auth0
                        UserAccount user = existingByEmail.get();
                        log.info("Updating auth0UserId for existing user: {} ({} -> {})", 
                                auth0User.email(), user.getAuth0UserId(), auth0User.userId());
                        user.setAuth0UserId(auth0User.userId());
                        updateUserIfNeeded(user, auth0User, userRole);
                        
                        // Reactivate if needed
                        if (!user.isActive()) {
                            user.setActive(true);
                        }
                        
                        userRepository.save(user);
                        updated++;
                    } else {
                        // Create new user
                        UserAccount newUser = new UserAccount(
                                auth0User.userId(),
                                auth0User.email() != null ? auth0User.email() : "unknown@example.com",
                                auth0User.givenName() != null ? auth0User.givenName() : "Unknown",
                                auth0User.familyName() != null ? auth0User.familyName() : "User",
                                userRole,
                                auth0User.getPreferredLanguage()
                        );
                        userRepository.save(newUser);
                        created++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to sync user {}: {}", auth0User.userId(), e.getMessage());
                }
            }

            // Delete users that no longer exist in Auth0
            int deleted = deleteMissingUsers(auth0UserIds);

            log.info("Auth0 sync complete: {} created, {} updated, {} unchanged, {} deleted",
                    created, updated, skipped, deleted);

        } catch (Exception e) {
            log.error("Failed to sync users from Auth0: {}", e.getMessage(), e);
        }
    }

    /**
     * Deletes local users whose auth0UserId is not in the provided set.
     * This handles users that were deleted from Auth0.
     */
    private int deleteMissingUsers(Set<String> activeAuth0UserIds) {
        List<UserAccount> allLocalUsers = userRepository.findAll();
        int deleted = 0;
        
        for (UserAccount user : allLocalUsers) {
            // Skip users with fake auth0 IDs (old seeded data) - just delete them
            String auth0Id = user.getAuth0UserId();
            if (auth0Id != null && (
                    auth0Id.matches("auth0\\|client\\d+") ||
                    auth0Id.matches("auth0\\|broker\\d+"))) {
                userRepository.delete(user);
                deleted++;
                log.info("Deleted fake seeded user: {} ({})", user.getEmail(), auth0Id);
                continue;
            }
            
            // If user's auth0 ID is not in the active set, delete them
            if (auth0Id != null && !activeAuth0UserIds.contains(auth0Id)) {
                try {
                    userRepository.delete(user);
                    deleted++;
                    log.info("Deleted user missing from Auth0: {} ({})", user.getEmail(), auth0Id);
                } catch (Exception e) {
                    log.warn("Could not delete user {} ({}): {}", user.getEmail(), auth0Id, e.getMessage());
                }
            }
        }
        
        return deleted;
    }

    /**
     * Updates user fields if they've changed.
     * Returns true if any changes were made.
     */
    private boolean updateUserIfNeeded(UserAccount user, Auth0User auth0User, UserRole userRole) {
        boolean changed = false;

        if (!userRole.equals(user.getRole())) {
            user.setRole(userRole);
            changed = true;
        }
        if (auth0User.email() != null && !auth0User.email().equals(user.getEmail())) {
            user.setEmail(auth0User.email());
            changed = true;
        }
        if (auth0User.givenName() != null && !auth0User.givenName().equals(user.getFirstName())) {
            user.setFirstName(auth0User.givenName());
            changed = true;
        }
        if (auth0User.familyName() != null && !auth0User.familyName().equals(user.getLastName())) {
            user.setLastName(auth0User.familyName());
            changed = true;
        }

        return changed;
    }
}
