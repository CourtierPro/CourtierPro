package com.example.courtierprobackend.transactions.util;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.transactions.datalayer.Transaction;

import java.util.UUID;

public class TransactionAccessUtils {

    /**
     * Verifies that the given user ID matches either the Broker ID or Client ID of
     * the transaction.
     * Throws ForbiddenException if the authenticated user is not authorized to
     * access this transaction.
     *
     * @param tx     The transaction to check.
     * @param userId The ID of the user attempting access.
     * @throws ForbiddenException if user is neither the broker nor the client.
     */
    public static void verifyTransactionAccess(Transaction tx, UUID userId) {
        if (userId == null) {
            throw new ForbiddenException("You do not have access to this transaction");
        }

        boolean isBroker = tx.getBrokerId() != null && tx.getBrokerId().equals(userId);
        boolean isClient = tx.getClientId() != null && tx.getClientId().equals(userId);

        if (!isBroker && !isClient) {
            throw new ForbiddenException("You do not have access to this transaction");
        }
    }

    /**
     * Verifies that the given user ID matches the Broker ID of the transaction.
     *
     * @param tx       The transaction to check.
     * @param brokerId The ID of the broker attempting access.
     * @throws ForbiddenException if user is not the broker.
     */
    public static void verifyBrokerAccess(Transaction tx, UUID brokerId) {
        if (brokerId == null || tx.getBrokerId() == null || !tx.getBrokerId().equals(brokerId)) {
            throw new ForbiddenException("You do not have access to this transaction");
        }
    }

    /**
     * Verifies that the given user ID matches the Client ID of the transaction.
     *
     * @param tx       The transaction to check.
     * @param clientId The ID of the client attempting access.
     * @throws ForbiddenException if user is not the client.
     */
    public static void verifyClientAccess(Transaction tx, UUID clientId) {
        if (clientId == null || tx.getClientId() == null || !tx.getClientId().equals(clientId)) {
            throw new ForbiddenException("You do not have access to this transaction");
        }
    }

    /**
     * Verifies that the user is either the Primary Broker OR a Co-Broker with the
     * required permission.
     */
    public static void verifyBrokerOrCoManagerAccess(Transaction tx, UUID userId, String userEmail,
            java.util.List<com.example.courtierprobackend.transactions.datalayer.TransactionParticipant> participants,
            com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission requiredPermission) {
        // 1. Check if Primary Broker (full access)
        if (tx.getBrokerId() != null && tx.getBrokerId().equals(userId)) {
            return;
        }

        // 2. Check if Co-Broker with Permission
        if (participants != null && userEmail != null) {
            String normalizedUserEmail = userEmail.trim();
            for (com.example.courtierprobackend.transactions.datalayer.TransactionParticipant p : participants) {
                if (p.getRole() == com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER
                        && p.getEmail() != null
                        && normalizedUserEmail.equalsIgnoreCase(p.getEmail().trim())) {

                    if (p.getPermissions() != null && p.getPermissions().contains(requiredPermission)) {
                        return;
                    }
                }
            }
        }

        throw new ForbiddenException("You do not have access to this transaction");
    }

    /**
     * Verifies that the user has VIEW access to a specific resource (documents,
     * properties, etc.).
     * - Primary Broker: Allowed
     * - Client: Allowed
     * - Co-Broker: Must have requiredPermission
     * - Other Participants: Allowed (default view access)
     */
    public static void verifyViewAccess(Transaction tx, UUID userId, String userEmail,
            java.util.List<com.example.courtierprobackend.transactions.datalayer.TransactionParticipant> participants,
            com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission requiredPermission) {

        if (userId == null) {
            throw new ForbiddenException("You do not have access to this transaction");
        }

        // 1. Primary Broker & Client always have access
        if ((tx.getBrokerId() != null && tx.getBrokerId().equals(userId)) ||
                (tx.getClientId() != null && tx.getClientId().equals(userId))) {
            return;
        }

        // 2. Check Participants
        if (participants != null && userEmail != null) {
            String normalizedUserEmail = userEmail.trim();
            for (com.example.courtierprobackend.transactions.datalayer.TransactionParticipant p : participants) {
                if (p.getEmail() != null && normalizedUserEmail.equalsIgnoreCase(p.getEmail().trim())) {
                    // For Co-Brokers, enforce granular permission
                    if (p.getRole() == com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER) {
                        if (p.getPermissions() != null && p.getPermissions().contains(requiredPermission)) {
                            return;
                        } else {
                            // Co-Broker exists but lacks permission
                            throw new ForbiddenException("You do not have permission to view this resource.");
                        }
                    }
                    // For other participants (Notary, Lawyer, etc.), allow view access by default
                    return;
                }
            }
        }

        throw new ForbiddenException("You do not have access to this transaction");
    }

    /**
     * Verifies that the user is the Broker, Client, or a defined Participant.
     * WARNING: This grants generic read access. Use verifyViewAccess for granular
     * checks.
     */
    public static void verifyTransactionAccess(Transaction tx, UUID userId, String userEmail,
            java.util.List<com.example.courtierprobackend.transactions.datalayer.TransactionParticipant> participants) {
        if (userId == null) {
            throw new ForbiddenException("You do not have access to this transaction");
        }

        boolean isBroker = tx.getBrokerId() != null && tx.getBrokerId().equals(userId);
        boolean isClient = tx.getClientId() != null && tx.getClientId().equals(userId);

        if (isBroker || isClient) {
            return;
        }

        if (participants != null && userEmail != null) {
            boolean isParticipant = participants.stream()
                    .anyMatch(p -> userEmail.equalsIgnoreCase(p.getEmail()));
            if (isParticipant)
                return;
        }

        throw new ForbiddenException("You do not have access to this transaction");
    }
}
