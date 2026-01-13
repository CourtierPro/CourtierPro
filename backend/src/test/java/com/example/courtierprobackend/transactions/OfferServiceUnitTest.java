package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.transactions.businesslayer.TransactionServiceImpl;
import com.example.courtierprobackend.transactions.datalayer.Offer;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for offer-related methods in TransactionServiceImpl.
 * Tests business logic for seller-side transaction offers.
 */
@ExtendWith(MockitoExtension.class)
class OfferServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PinnedTransactionRepository pinnedTransactionRepository;

    @Mock
    private TransactionParticipantRepository participantRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TimelineService timelineService;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private PropertyOfferRepository propertyOfferRepository;

    @Mock
    private OfferDocumentRepository offerDocumentRepository;

    @Mock
    private OfferRevisionRepository offerRevisionRepository;

    @Mock
    private com.example.courtierprobackend.infrastructure.storage.S3StorageService s3StorageService;

    @InjectMocks
    private TransactionServiceImpl service;

    private UUID transactionId;
    private UUID brokerId;
    private UUID clientId;
    private UUID offerId;
    private Transaction sellSideTransaction;
    private Transaction buySideTransaction;
    private Offer sampleOffer;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        offerId = UUID.randomUUID();

        sellSideTransaction = new Transaction();
        sellSideTransaction.setTransactionId(transactionId);
        sellSideTransaction.setBrokerId(brokerId);
        sellSideTransaction.setClientId(clientId);
        sellSideTransaction.setSide(TransactionSide.SELL_SIDE);
        sellSideTransaction.setStatus(TransactionStatus.ACTIVE);

        buySideTransaction = new Transaction();
        buySideTransaction.setTransactionId(transactionId);
        buySideTransaction.setBrokerId(brokerId);
        buySideTransaction.setClientId(clientId);
        buySideTransaction.setSide(TransactionSide.BUY_SIDE);
        buySideTransaction.setStatus(TransactionStatus.ACTIVE);

        sampleOffer = Offer.builder()
                .id(1L)
                .offerId(offerId)
                .transactionId(transactionId)
                .buyerName("John Doe")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .notes("Test notes")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Default stub: offerDocumentRepository returns empty list
        lenient().when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(any()))
                .thenReturn(java.util.Collections.emptyList());
        // Default stub: offerRevisionRepository returns null for max revision
        lenient().when(offerRevisionRepository.findMaxRevisionNumberByOfferId(any()))
                .thenReturn(null);
    }

    // ==================== getOffers Tests ====================

    @Nested
    @DisplayName("getOffers")
    class GetOffersTests {

        @Test
        @DisplayName("should return offers for sell-side transaction")
        void getOffers_sellSide_returnsOffers() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of(sampleOffer));

            List<OfferResponseDTO> result = service.getOffers(transactionId, brokerId, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBuyerName()).isEqualTo("John Doe");
            assertThat(result.get(0).getNotes()).isEqualTo("Test notes");
        }

        @Test
        @DisplayName("should show notes for non-broker")
        void getOffers_asClient_showsNotes() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of(sampleOffer));

            List<OfferResponseDTO> result = service.getOffers(transactionId, clientId, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotes()).isEqualTo(sampleOffer.getNotes());
        }

        @Test
        @DisplayName("should return empty list for buy-side transaction")
        void getOffers_buySide_returnsEmptyList() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            List<OfferResponseDTO> result = service.getOffers(transactionId, brokerId, true);

            assertThat(result).isEmpty();
            verify(offerRepository, never()).findByTransactionIdOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void getOffers_transactionNotFound_throws() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOffers(transactionId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Transaction not found");
        }
    }

    // ==================== addOffer Tests ====================

    @Nested
    @DisplayName("addOffer")
    class AddOfferTests {

        @Test
        @DisplayName("should add offer to sell-side transaction")
        void addOffer_sellSide_addsOffer() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("Jane Smith")
                    .offerAmount(BigDecimal.valueOf(450000))
                    .status(ReceivedOfferStatus.PENDING)
                    .notes("First offer")
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.save(any(Offer.class)))
                    .thenAnswer(inv -> {
                        Offer saved = inv.getArgument(0);
                        saved.setId(1L);
                        return saved;
                    });

            OfferResponseDTO result = service.addOffer(transactionId, request, brokerId);

            assertThat(result.getBuyerName()).isEqualTo("Jane Smith");
            assertThat(result.getOfferAmount()).isEqualByComparingTo(BigDecimal.valueOf(450000));
            verify(timelineService).addEntry(any(), eq(brokerId), any(), any(), isNull(), any());
            verify(notificationService).createNotification(
                    eq(clientId.toString()),
                    eq("notifications.offerReceived.title"),
                    eq("notifications.offerReceived.message"),
                    any(java.util.Map.class),
                    eq(transactionId.toString()),
                    any()
            );
        }

        @Test
        @DisplayName("should throw BadRequestException for buy-side transaction")
        void addOffer_buySide_throws() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("Jane Smith")
                    .offerAmount(BigDecimal.valueOf(400000))
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.addOffer(transactionId, request, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("seller-side");
        }

        @Test
        @DisplayName("should set default status if not provided")
        void addOffer_noStatus_setsDefault() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("Jane Smith")
                    .offerAmount(BigDecimal.valueOf(425000))
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.save(any(Offer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OfferResponseDTO result = service.addOffer(transactionId, request, brokerId);

            assertThat(result.getStatus()).isEqualTo(ReceivedOfferStatus.PENDING);
        }
    }

    // ==================== updateOffer Tests ====================

    @Nested
    @DisplayName("updateOffer")
    class UpdateOfferTests {

        @Test
        @DisplayName("should update offer successfully")
        void updateOffer_validRequest_updatesOffer() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("John Doe Updated")
                    .offerAmount(BigDecimal.valueOf(520000))
                    .status(ReceivedOfferStatus.ACCEPTED)
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(sampleOffer));
            when(offerRepository.save(any(Offer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OfferResponseDTO result = service.updateOffer(transactionId, offerId, request, brokerId);

            assertThat(result.getBuyerName()).isEqualTo("John Doe Updated");
            assertThat(result.getStatus()).isEqualTo(ReceivedOfferStatus.ACCEPTED);
            // Status change should trigger timeline entry
            verify(timelineService).addEntry(any(), eq(brokerId), any(), any(), isNull(), any());
        }

        @Test
        @DisplayName("should throw NotFoundException when offer not found")
        void updateOffer_offerNotFound_throws() {
            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOffer(transactionId, offerId, request, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Offer not found");
        }

        @Test
        @DisplayName("should throw BadRequestException when offer belongs to different transaction")
        void updateOffer_wrongTransaction_throws() {
            UUID otherTransactionId = UUID.randomUUID();
            sampleOffer.setTransactionId(otherTransactionId);

            OfferRequestDTO request = OfferRequestDTO.builder()
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(sampleOffer));

            assertThatThrownBy(() -> service.updateOffer(transactionId, offerId, request, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // ==================== removeOffer Tests ====================

    @Nested
    @DisplayName("removeOffer")
    class RemoveOfferTests {

        @Test
        @DisplayName("should remove offer successfully")
        void removeOffer_validRequest_removesOffer() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(sampleOffer));

            service.removeOffer(transactionId, offerId, brokerId);

            verify(offerRepository).delete(sampleOffer);
            verify(timelineService).addEntry(any(), eq(brokerId), any(), any(), isNull(), any());
        }

        @Test
        @DisplayName("should throw NotFoundException when offer not found")
        void removeOffer_offerNotFound_throws() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeOffer(transactionId, offerId, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Offer not found");
        }
    }

    // ==================== getOfferById Tests ====================

    @Nested
    @DisplayName("getOfferById")
    class GetOfferByIdTests {

        @Test
        @DisplayName("should return offer with notes for broker")
        void getOfferById_asBroker_returnsOfferWithNotes() {
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(sampleOffer));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));

            OfferResponseDTO result = service.getOfferById(offerId, brokerId, true);

            assertThat(result.getBuyerName()).isEqualTo("John Doe");
            assertThat(result.getNotes()).isEqualTo("Test notes");
        }

        @Test
        @DisplayName("should return offer with notes for client")
        void getOfferById_asClient_showsNotes() {
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.of(sampleOffer));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));

            OfferResponseDTO result = service.getOfferById(offerId, clientId, false);

            assertThat(result.getBuyerName()).isEqualTo("John Doe");
            assertThat(result.getNotes()).isEqualTo(sampleOffer.getNotes());
        }

        @Test
        @DisplayName("should throw NotFoundException when offer not found")
        void getOfferById_offerNotFound_throws() {
            when(offerRepository.findByOfferId(offerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOfferById(offerId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Offer not found");
        }
    }
}
