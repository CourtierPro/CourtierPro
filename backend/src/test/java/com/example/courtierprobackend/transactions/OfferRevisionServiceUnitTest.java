package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.transactions.businesslayer.TransactionServiceImpl;
import com.example.courtierprobackend.transactions.datalayer.Offer;
import com.example.courtierprobackend.transactions.datalayer.OfferRevision;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRevisionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for offer revision-related methods in TransactionServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class OfferRevisionServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private PropertyRepository propertyRepository; // Required for constructor

    @Mock
    private PinnedTransactionRepository pinnedTransactionRepository; // Required for constructor

    @Mock
    private TransactionParticipantRepository participantRepository; // Required for constructor

    @Mock
    private UserAccountRepository userAccountRepository; // Required for constructor

    @Mock
    private EmailService emailService; // Required for constructor

    @Mock
    private NotificationService notificationService; // Required for constructor

    @Mock
    private TimelineService timelineService;

    @Mock
    private ConditionRepository conditionRepository; // Required for constructor

    @Mock
    private PropertyOfferRepository propertyOfferRepository; // Required for constructor

    @Mock
    private OfferDocumentRepository offerDocumentRepository; // Required for constructor

    @Mock
    private OfferRevisionRepository offerRevisionRepository;

    @Mock
    private com.example.courtierprobackend.infrastructure.storage.S3StorageService s3StorageService;

    @Mock
    private DocumentConditionLinkRepository documentConditionLinkRepository;

    @InjectMocks
    private TransactionServiceImpl service;

    private UUID transactionId;
    private UUID brokerId;
    private UUID offerId;
    private Transaction transaction;
    private Offer offer;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        offerId = UUID.randomUUID();

        transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setBrokerId(brokerId);
        transaction.setSide(TransactionSide.SELL_SIDE);
        transaction.setStatus(TransactionStatus.ACTIVE);

        offer = Offer.builder()
                .id(1L)
                .offerId(offerId)
                .transactionId(transactionId)
                .buyerName("John Doe")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("getOfferRevisions")
    class GetOfferRevisionsTests {

        @Test
        @DisplayName("should return revisions for an offer")
        void getOfferRevisions_validOffer_returnsRevisions() {
            OfferRevision revision1 = OfferRevision.builder()
                    .revisionId(UUID.randomUUID())
                    .offerId(offerId)
                    .revisionNumber(1)
                    .previousAmount(BigDecimal.valueOf(450000))
                    .newAmount(BigDecimal.valueOf(500000))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(offer));
            when(offerRevisionRepository.findByOfferIdOrderByRevisionNumberAsc(offerId))
                    .thenReturn(List.of(revision1));

            List<OfferRevisionResponseDTO> result = service.getOfferRevisions(offerId, brokerId, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRevisionNumber()).isEqualTo(1);
            assertThat(result.get(0).getNewAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        }

        @Test
        @DisplayName("should throw NotFoundException when offer not found")
        void getOfferRevisions_offerNotFound_throws() {

            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOfferRevisions(offerId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Offer not found");
        }
    }

    @Nested
    @DisplayName("createRevisionOnUpdate")
    class CreateRevisionOnUpdateTests {

        @Test
        @DisplayName("should create revision when offer amount changes")
        void updateOffer_amountChanged_createsRevision() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(550000))
                    .status(ReceivedOfferStatus.PENDING)
                    .build();

            // Set existing maximum revision number
            when(offerRevisionRepository.findMaxRevisionNumberByOfferId(offerId)).thenReturn(1);

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.updateOffer(transactionId, offerId, request, brokerId);

            // Verify revision repository save was called
            verify(offerRevisionRepository).save(argThat(revision -> 
                revision.getOfferId().equals(offerId) &&
                revision.getRevisionNumber() == 2 &&
                revision.getPreviousAmount().compareTo(BigDecimal.valueOf(500000)) == 0 &&
                revision.getNewAmount().compareTo(BigDecimal.valueOf(550000)) == 0
            ));
        }

        @Test
        @DisplayName("should create revision when offer status changes")
        void updateOffer_statusChanged_createsRevision() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .status(ReceivedOfferStatus.ACCEPTED)
                    .build();

            when(offerRevisionRepository.findMaxRevisionNumberByOfferId(offerId)).thenReturn(0);

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.updateOffer(transactionId, offerId, request, brokerId);

            verify(offerRevisionRepository).save(argThat(revision -> 
                revision.getNewStatus().equals(ReceivedOfferStatus.ACCEPTED.name()) &&
                revision.getPreviousStatus().equals(ReceivedOfferStatus.PENDING.name())
            ));
        }

        @Test
        @DisplayName("should NOT create revision when nothing changes")
        void updateOffer_nothingChanged_doesNotCreateRevision() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .status(ReceivedOfferStatus.PENDING)
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(offer));
            when(offerRepository.save(any(Offer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.updateOffer(transactionId, offerId, request, brokerId);

            verify(offerRevisionRepository, never()).save(any());
        }
    }
}
