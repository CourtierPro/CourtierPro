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
            for (com.example.courtierprobackend.transactions.datalayer.TransactionParticipant p : participants) {
                if (p.getRole() == com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER
                        &&
                        userEmail.equalsIgnoreCase(p.getEmail())) {

                    if (p.getPermissions() != null && p.getPermissions().contains(requiredPermission)) {
                        return;
                    }
                }
            }
        }

        throw new ForbiddenException("You do not have access to this transaction");
    }

    /**
     * Verifies that the user is the Broker, Client, or a defined Participant.
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
