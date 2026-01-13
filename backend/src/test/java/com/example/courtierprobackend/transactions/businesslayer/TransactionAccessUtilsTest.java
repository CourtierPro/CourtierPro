package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TransactionAccessUtilsTest {

    @Test
    void verifyTransactionAccess_NullUserId_ThrowsForbidden() {
        Transaction tx = new Transaction();
        assertThatThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(tx, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void verifyTransactionAccess_UserIsBroker_AllowsAccess() {
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setBrokerId(brokerId);
        assertThatNoException().isThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(tx, brokerId));
    }

    @Test
    void verifyTransactionAccess_UserIsClient_AllowsAccess() {
        UUID clientId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setClientId(clientId);
        assertThatNoException().isThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(tx, clientId));
    }

    @Test
    void verifyTransactionAccess_UserIsNeither_ThrowsForbidden() {
        Transaction tx = new Transaction();
        tx.setBrokerId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        UUID otherId = UUID.randomUUID();
        assertThatThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(tx, otherId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void verifyBrokerAccess_NullBrokerId_ThrowsForbidden() {
        Transaction tx = new Transaction();
        assertThatThrownBy(() -> TransactionAccessUtils.verifyBrokerAccess(tx, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void verifyBrokerAccess_BrokerIdDoesNotMatch_ThrowsForbidden() {
        Transaction tx = new Transaction();
        tx.setBrokerId(UUID.randomUUID());
        UUID otherId = UUID.randomUUID();
        assertThatThrownBy(() -> TransactionAccessUtils.verifyBrokerAccess(tx, otherId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void verifyBrokerAccess_BrokerIdMatches_AllowsAccess() {
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setBrokerId(brokerId);
        assertThatNoException().isThrownBy(() -> TransactionAccessUtils.verifyBrokerAccess(tx, brokerId));
    }
}
