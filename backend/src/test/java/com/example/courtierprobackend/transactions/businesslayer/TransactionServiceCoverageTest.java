package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ConflictException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.infrastructure.storage.ObjectStorageService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.TransactionParticipant;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.UpdateParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TransactionServiceCoverageTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private TimelineService timelineService;
    @Mock private TransactionParticipantRepository participantRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private PinnedTransactionRepository pinnedTransactionRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private ConditionRepository conditionRepository;
    @Mock private PropertyOfferRepository propertyOfferRepository;
    @Mock private OfferDocumentRepository offerDocumentRepository;
    @Mock private OfferRevisionRepository offerRevisionRepository;
    @Mock private ObjectStorageService objectStorageService;
    @Mock private com.example.courtierprobackend.documents.datalayer.DocumentRepository documentRepository;
    @Mock private DocumentConditionLinkRepository documentConditionLinkRepository;
    @Mock private SearchCriteriaRepository searchCriteriaRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private UUID transactionId;
    private UUID brokerId;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setBrokerId(brokerId);
    }

    // --- Update Notes ---

    @Test
    void saveInternalNotes_transactionNotFound_throwsNotFoundException() {
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.saveInternalNotes(transactionId, "notes", brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Transaction not found");
    }

    // --- Terminate Transaction ---

    @Test
    void terminateTransaction_transactionNotFound_throwsNotFoundException() {
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.terminateTransaction(transactionId, "Valid termination reason > 10 chars", brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    void terminateTransaction_optimisticLockingFailure_throwsConflictException() {
        transaction.setStatus(TransactionStatus.ACTIVE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new OptimisticLockingFailureException("concurrent"));

        // Mock broker exists for access check
        UserAccount broker = new UserAccount();
        broker.setId(brokerId);
        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(broker));

        // Mock participant check (broker is participant)
        TransactionParticipant participant = new TransactionParticipant();
        participant.setUserId(brokerId);
        participant.setRole(ParticipantRole.BROKER);
        participant.setPermissions(Collections.singleton(ParticipantPermission.EDIT_STAGE));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));

        assertThatThrownBy(() -> transactionService.terminateTransaction(transactionId, "Valid termination reason > 10 chars", brokerId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("The transaction was updated by another user");
    }

    // --- Update Stage Rollback Validation ---

    @Test
    void updateStage_rollbackReasonTooShort_throwsBadRequest() {
        transaction.setSide(TransactionSide.BUY_SIDE);
        transaction.setBuyerStage(BuyerStage.BUYER_POSSESSION);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        // Mock permission
        TransactionParticipant participant = new TransactionParticipant();
        participant.setUserId(brokerId);
        participant.setPermissions(Collections.singleton(ParticipantPermission.EDIT_STAGE));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("BUYER_PROPERTY_SEARCH");
        dto.setReason("short");

        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reason must be between 10 and 500 characters.");
    }

    @Test
    void updateStage_rollbackReasonTypes_validatesCorrectly() {
        transaction.setSide(TransactionSide.BUY_SIDE);
        transaction.setBuyerStage(BuyerStage.BUYER_POSSESSION);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        TransactionParticipant participant = new TransactionParticipant();
        participant.setUserId(brokerId);
        participant.setPermissions(Collections.singleton(ParticipantPermission.EDIT_STAGE));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));

        // Null reason
        StageUpdateRequestDTO nullReason = new StageUpdateRequestDTO();
        nullReason.setStage("BUYER_PROPERTY_SEARCH");
        nullReason.setReason(null);
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, nullReason, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reason is required for stage rollback.");

        // Blank reason
        StageUpdateRequestDTO blankReason = new StageUpdateRequestDTO();
        blankReason.setStage("BUYER_PROPERTY_SEARCH");
        blankReason.setReason("   ");
        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, blankReason, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reason is required for stage rollback.");
    }

    @Test
    void updateStage_invalidBuyerStage_throwsBadRequest() {
        transaction.setSide(TransactionSide.BUY_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        TransactionParticipant participant = new TransactionParticipant();
        participant.setUserId(brokerId);
        participant.setPermissions(Collections.singleton(ParticipantPermission.EDIT_STAGE));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("INVALID_STAGE");

        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid buyer stage");
    }

    @Test
    void updateStage_invalidSellerStage_throwsBadRequest() {
        transaction.setSide(TransactionSide.SELL_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        TransactionParticipant participant = new TransactionParticipant();
        participant.setUserId(brokerId);
        participant.setPermissions(Collections.singleton(ParticipantPermission.EDIT_STAGE));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("INVALID_STAGE");

        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid seller stage");
    }

    @Test
    void updateStage_sellerRollbackValidatesReason() {
        transaction.setSide(TransactionSide.SELL_SIDE);
        transaction.setSellerStage(SellerStage.SELLER_HANDOVER);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        TransactionParticipant participant = new TransactionParticipant();
        participant.setUserId(brokerId);
        participant.setPermissions(Collections.singleton(ParticipantPermission.EDIT_STAGE));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));

        StageUpdateRequestDTO dto = new StageUpdateRequestDTO();
        dto.setStage("SELLER_PUBLISH_LISTING"); // Rollback without reason

        assertThatThrownBy(() -> transactionService.updateTransactionStage(transactionId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reason is required for stage rollback.");
    }

    // --- Update Participant ---

    @Test
    void updateParticipant_participantNotFound_throwsNotFound() {
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        when(participantRepository.findById(any())).thenReturn(Optional.empty());

        // Mock permission
        TransactionParticipant broker = new TransactionParticipant();
        broker.setUserId(brokerId);
        broker.setRole(ParticipantRole.BROKER);
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(broker));

        UpdateParticipantRequestDTO dto = new UpdateParticipantRequestDTO();
        dto.setName("Name");
        dto.setEmail("email@test.com");
        dto.setRole(ParticipantRole.BUYER);
        UUID partId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionService.updateParticipant(transactionId, partId, dto, brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Participant not found");
    }

    @Test
    void updateParticipant_wrongTransaction_throwsBadRequest() {
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        TransactionParticipant target = new TransactionParticipant();
        target.setTransactionId(UUID.randomUUID()); // Different transaction
        when(participantRepository.findById(any())).thenReturn(Optional.of(target));

        // Mock permission
        TransactionParticipant broker = new TransactionParticipant();
        broker.setUserId(brokerId);
        broker.setRole(ParticipantRole.BROKER);
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(broker));

        UpdateParticipantRequestDTO dto = new UpdateParticipantRequestDTO();
        dto.setName("Name");
        UUID partId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionService.updateParticipant(transactionId, partId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Participant does not belong to this transaction");
    }

    @Test
    void updateParticipant_duplicateEmail_throwsBadRequest() {
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        UUID targetId = UUID.randomUUID();
        TransactionParticipant target = new TransactionParticipant();
        target.setId(targetId);
        target.setTransactionId(transactionId);
        target.setSystem(false);
        when(participantRepository.findById(targetId)).thenReturn(Optional.of(target));

        // Mock existing participants
        TransactionParticipant broker = new TransactionParticipant();
        broker.setUserId(brokerId);
        broker.setRole(ParticipantRole.BROKER);

        TransactionParticipant other = new TransactionParticipant();
        other.setId(UUID.randomUUID());
        other.setEmail("duplicate@test.com");

        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(broker, target, other));

        UpdateParticipantRequestDTO dto = new UpdateParticipantRequestDTO();
        dto.setName("Name");
        dto.setEmail("duplicate@test.com");

        assertThatThrownBy(() -> transactionService.updateParticipant(transactionId, targetId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Un participant avec cet email existe déjà dans la transaction.");
    }

    @Test
    void updateParticipant_systemParticipant_throwsBadRequest() {
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        TransactionParticipant target = new TransactionParticipant();
        target.setTransactionId(transactionId);
        target.setSystem(true);
        when(participantRepository.findById(any())).thenReturn(Optional.of(target));

        // Mock permission
        TransactionParticipant broker = new TransactionParticipant();
        broker.setUserId(brokerId);
        broker.setRole(ParticipantRole.BROKER);
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(broker));

        UpdateParticipantRequestDTO dto = new UpdateParticipantRequestDTO();
        dto.setName("Name");
        UUID partId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionService.updateParticipant(transactionId, partId, dto, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Impossible de modifier ce participant système.");
    }

    // --- House Visit Count ---

    @Test
    void getHouseVisitCount_sellSide_returnsZero() {
        transaction.setSide(TransactionSide.SELL_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

        // Mock permission
        TransactionParticipant participant = new TransactionParticipant();
        participant.setUserId(brokerId);
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(List.of(participant));

        int count = transactionService.getHouseVisitCount(transactionId, brokerId);

        assertThat(count).isZero();
    }
}
