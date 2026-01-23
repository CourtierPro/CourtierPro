package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import com.example.courtierprobackend.transactions.util.TransactionAccessUtils;
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

    @Nested
    @DisplayName("verifyBrokerOrCoManagerAccess")
    class VerifyBrokerOrCoManagerAccessTests {
        
        @Test
        @DisplayName("should allow access for primary broker")
        void verifyBrokerOrCoManagerAccess_asPrimaryBroker_succeeds() {
            assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyBrokerOrCoManagerAccess(transaction, brokerId, "broker@example.com", null, null));
        }

        @Test
        @DisplayName("should allow access for co-broker with permission")
        void verifyBrokerOrCoManagerAccess_asCoBrokerWithPermission_succeeds() {
            UUID coBrokerId = UUID.randomUUID();
            String email = "cobroker@example.com";
            var permission = com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS;
            
            var participant = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
            participant.setUserId(coBrokerId);
            participant.setEmail(email);
            participant.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
            participant.setPermissions(java.util.Set.of(permission));

            assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyBrokerOrCoManagerAccess(transaction, coBrokerId, email, java.util.List.of(participant), permission));
        }

        @Test
        @DisplayName("should deny access for co-broker without permission")
        void verifyBrokerOrCoManagerAccess_asCoBrokerWithoutPermission_throws() {
            UUID coBrokerId = UUID.randomUUID();
            String email = "cobroker@example.com";
            var permission = com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS;
            
            var participant = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
            participant.setUserId(coBrokerId);
            participant.setEmail(email);
            participant.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
            participant.setPermissions(java.util.Collections.emptySet()); // No permissions

            assertThatThrownBy(() -> 
                TransactionAccessUtils.verifyBrokerOrCoManagerAccess(transaction, coBrokerId, email, java.util.List.of(participant), permission))
                .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should deny access for non-participant")
        void verifyBrokerOrCoManagerAccess_asNonParticipant_throws() {
             var permission = com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.EDIT_DOCUMENTS;
             assertThatThrownBy(() -> 
                TransactionAccessUtils.verifyBrokerOrCoManagerAccess(transaction, UUID.randomUUID(), "random@example.com", java.util.Collections.emptyList(), permission))
                .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("verifyViewAccess")
    class VerifyViewAccessTests {

        @Test
        @DisplayName("should allow view access for primary broker")
        void verifyViewAccess_primaryBroker_succeeds() {
             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, brokerId, "broker@example.com", null, null));
        }

        @Test
        @DisplayName("should allow view access for client")
        void verifyViewAccess_client_succeeds() {
             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, clientId, "client@example.com", null, null));
        }

        @Test
        @DisplayName("should allow view access for co-broker with permission")
        void verifyViewAccess_coBrokerWithPermission_succeeds() {
            UUID coBrokerId = UUID.randomUUID();
            String email = "cobroker@example.com";
            var permission = com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.VIEW_DOCUMENTS;

            var participant = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
            participant.setUserId(coBrokerId);
            participant.setEmail(email);
            participant.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
            participant.setPermissions(java.util.Set.of(permission));

             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, coBrokerId, email, java.util.List.of(participant), permission));
        }
        
        @Test
        @DisplayName("should deny view access for co-broker without permission")
        void verifyViewAccess_coBrokerWithoutPermission_throws() {
            UUID coBrokerId = UUID.randomUUID();
            String email = "cobroker@example.com";
            var permission = com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission.VIEW_DOCUMENTS;

            var participant = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
            participant.setUserId(coBrokerId);
            participant.setEmail(email);
            participant.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.CO_BROKER);
            participant.setPermissions(java.util.Collections.emptySet());

             assertThatThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, coBrokerId, email, java.util.List.of(participant), permission))
                .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should allow view access for other participants (Notary)")
        void verifyViewAccess_otherParticipant_succeeds() {
            UUID notaryId = UUID.randomUUID();
            String email = "notary@example.com";

            var participant = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
            participant.setUserId(notaryId);
            participant.setEmail(email);
            participant.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.NOTARY); // Not CO_BROKER

             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, notaryId, email, java.util.List.of(participant), null));
        }
        
         @Test
        @DisplayName("should verify participant by email if ID is missing")
        void verifyViewAccess_participantByEmail_succeeds() {
            String email = "notary@example.com";
            UUID userId = UUID.randomUUID(); // User exploring access

            var participant = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
            participant.setUserId(null); // ID missing in participant record
            participant.setEmail(email);
            participant.setRole(com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole.NOTARY);

             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, userId, email, java.util.List.of(participant), null));
        }

        @Test
        @DisplayName("should deny view access for null user ID")
        void verifyViewAccess_nullUserId_throws() {
            assertThatThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, null, "email", null, null))
                .isInstanceOf(ForbiddenException.class);
        }
        
        @Test
        @DisplayName("should deny view access for non-participant")
        void verifyViewAccess_nonParticipant_throws() {
             assertThatThrownBy(() -> 
                TransactionAccessUtils.verifyViewAccess(transaction, UUID.randomUUID(), "random@example.com", java.util.Collections.emptyList(), null))
                .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("verifyTransactionAccess (Participants)")
    class VerifyTransactionAccessParticipantsTests {

        @Test
        @DisplayName("should allow access for broker")
        void verifyTransactionAccess_broker_succeeds() {
             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyTransactionAccess(transaction, brokerId, "broker@example.com", null));
        }
        
        @Test
        @DisplayName("should allow access for client")
        void verifyTransactionAccess_client_succeeds() {
             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyTransactionAccess(transaction, clientId, "client@example.com", null));
        }

        @Test
        @DisplayName("should allow access for participant by email")
        void verifyTransactionAccess_participant_succeeds() {
             String email = "participant@example.com";
             var participant = new com.example.courtierprobackend.transactions.datalayer.TransactionParticipant();
             participant.setEmail(email);

             assertThatNoException().isThrownBy(() -> 
                TransactionAccessUtils.verifyTransactionAccess(transaction, UUID.randomUUID(), email, java.util.List.of(participant)));
        }

        @Test
        @DisplayName("should deny access for non-participant")
        void verifyTransactionAccess_nonParticipant_throws() {
             assertThatThrownBy(() -> 
                TransactionAccessUtils.verifyTransactionAccess(transaction, UUID.randomUUID(), "random@example.com", java.util.Collections.emptyList()))
                .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should deny access for null user ID")
        void verifyTransactionAccess_nullUserId_throws() {
             assertThatThrownBy(() -> 
                TransactionAccessUtils.verifyTransactionAccess(transaction, null, "email", null))
                .isInstanceOf(ForbiddenException.class);
        }
    }
}
