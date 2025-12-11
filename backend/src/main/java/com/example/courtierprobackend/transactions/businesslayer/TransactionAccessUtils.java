package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.transactions.datalayer.Transaction;

import java.util.UUID;

public class TransactionAccessUtils {

    /**
     * Verifies that the given user ID matches either the Broker ID or Client ID of the transaction.
     * Throws ForbiddenException if the authenticated user is not authorized to access this transaction.
     *
     * @param tx The transaction to check.
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
     * @param tx The transaction to check.
     * @param brokerId The ID of the broker attempting access.
     * @throws ForbiddenException if user is not the broker.
     */
    public static void verifyBrokerAccess(Transaction tx, UUID brokerId) {
        if (brokerId == null || tx.getBrokerId() == null || !tx.getBrokerId().equals(brokerId)) {
            throw new ForbiddenException("You do not have access to this transaction");
        }
    }
}
