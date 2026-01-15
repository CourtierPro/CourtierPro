package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TransactionAccessUtils utility class.
 * Tests access verification logic for transactions.
 */
class TransactionAccessUtilsTest {

    private Transaction transaction;
    private UUID brokerId;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        
        transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setBrokerId(brokerId);
        transaction.setClientId(clientId);
    }

    @Nested
    @DisplayName("verifyTransactionAccess")
    class VerifyTransactionAccessTests {

        @Test
        @DisplayName("should allow access for broker")
        void verifyTransactionAccess_asBroker_succeeds() {
            assertThatNoException()
                    .isThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(transaction, brokerId));
        }

        @Test
        @DisplayName("should allow access for client")
        void verifyTransactionAccess_asClient_succeeds() {
            assertThatNoException()
                    .isThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(transaction, clientId));
        }

        @Test
        @DisplayName("should deny access for unauthorized user")
        void verifyTransactionAccess_asUnauthorized_throws() {
            UUID unauthorizedUser = UUID.randomUUID();
            
            assertThatThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(transaction, unauthorizedUser))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("should deny access for null user ID")
        void verifyTransactionAccess_withNullUserId_throws() {
            assertThatThrownBy(() -> TransactionAccessUtils.verifyTransactionAccess(transaction, null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }
    }

    @Nested
    @DisplayName("verifyBrokerAccess")
    class VerifyBrokerAccessTests {

        @Test
        @DisplayName("should allow access for broker")
        void verifyBrokerAccess_asBroker_succeeds() {
            assertThatNoException()
                    .isThrownBy(() -> TransactionAccessUtils.verifyBrokerAccess(transaction, brokerId));
        }

        @Test
        @DisplayName("should deny access for client")
        void verifyBrokerAccess_asClient_throws() {
            assertThatThrownBy(() -> TransactionAccessUtils.verifyBrokerAccess(transaction, clientId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("should deny access for null broker ID")
        void verifyBrokerAccess_withNullBrokerId_throws() {
            assertThatThrownBy(() -> TransactionAccessUtils.verifyBrokerAccess(transaction, null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("should deny access when transaction has no broker")
        void verifyBrokerAccess_transactionNoBroker_throws() {
            transaction.setBrokerId(null);
            
            assertThatThrownBy(() -> TransactionAccessUtils.verifyBrokerAccess(transaction, brokerId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }
    }

    @Nested
    @DisplayName("verifyClientAccess")
    class VerifyClientAccessTests {

        @Test
        @DisplayName("should allow access for client")
        void verifyClientAccess_asClient_succeeds() {
            assertThatNoException()
                    .isThrownBy(() -> TransactionAccessUtils.verifyClientAccess(transaction, clientId));
        }

        @Test
        @DisplayName("should deny access for broker")
        void verifyClientAccess_asBroker_throws() {
            assertThatThrownBy(() -> TransactionAccessUtils.verifyClientAccess(transaction, brokerId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("should deny access for null client ID")
        void verifyClientAccess_withNullClientId_throws() {
            assertThatThrownBy(() -> TransactionAccessUtils.verifyClientAccess(transaction, null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("should deny access when transaction has no client")
        void verifyClientAccess_transactionNoClient_throws() {
            transaction.setClientId(null);
            
            assertThatThrownBy(() -> TransactionAccessUtils.verifyClientAccess(transaction, clientId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }

        @Test
        @DisplayName("should deny access for unauthorized user")
        void verifyClientAccess_asUnauthorized_throws() {
            UUID unauthorizedUser = UUID.randomUUID();
            
            assertThatThrownBy(() -> TransactionAccessUtils.verifyClientAccess(transaction, unauthorizedUser))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access");
        }
    }
}
