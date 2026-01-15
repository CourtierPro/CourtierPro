package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.transactions.businesslayer.TransactionServiceImpl;
import com.example.courtierprobackend.transactions.datalayer.Property;
import com.example.courtierprobackend.transactions.datalayer.PropertyOffer;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.BuyerOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.CounterpartyResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for property offer methods in TransactionServiceImpl.
 * Tests business logic for buy-side transaction property offers.
 */
@ExtendWith(MockitoExtension.class)
class PropertyOfferServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyOfferRepository propertyOfferRepository;

    @Mock
    private OfferDocumentRepository offerDocumentRepository;

    @Mock
    private OfferRevisionRepository offerRevisionRepository;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private ConditionRepository conditionRepository;

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
    private S3StorageService s3StorageService;

    @Mock
    private DocumentConditionLinkRepository documentConditionLinkRepository;

    @InjectMocks
    private TransactionServiceImpl service;

    private UUID transactionId;
    private UUID propertyId;
    private UUID propertyOfferId;
    private UUID brokerId;
    private UUID clientId;
    private Transaction buySideTransaction;
    private Property sampleProperty;
    private PropertyOffer sampleOffer;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        propertyOfferId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        buySideTransaction = new Transaction();
        buySideTransaction.setTransactionId(transactionId);
        buySideTransaction.setBrokerId(brokerId);
        buySideTransaction.setClientId(clientId);
        buySideTransaction.setSide(TransactionSide.BUY_SIDE);
        buySideTransaction.setStatus(TransactionStatus.ACTIVE);

        PropertyAddress address = new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1");
        sampleProperty = Property.builder()
                .id(1L)
                .propertyId(propertyId)
                .transactionId(transactionId)
                .address(address)
                .askingPrice(new BigDecimal("500000"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleOffer = PropertyOffer.builder()
                .id(1L)
                .propertyOfferId(propertyOfferId)
                .propertyId(propertyId)
                .offerRound(1)
                .offerAmount(new BigDecimal("475000"))
                .status(BuyerOfferStatus.OFFER_MADE)
                .expiryDate(LocalDate.now().plusDays(3))
                .notes("Initial offer")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Default stubs for document methods
        lenient().when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(any()))
                .thenReturn(Collections.emptyList());
    }

    // ==================== getPropertyOffers Tests ====================

    @Nested
    @DisplayName("getPropertyOffers")
    class GetPropertyOffersTests {

        @Test
        @DisplayName("should return property offers for broker")
        void getPropertyOffers_asBroker_returnsOffers() {
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId))
                    .thenReturn(List.of(sampleOffer));

            List<PropertyOfferResponseDTO> result = service.getPropertyOffers(propertyId, brokerId, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOfferAmount()).isEqualByComparingTo(new BigDecimal("475000"));
            assertThat(result.get(0).getStatus()).isEqualTo(BuyerOfferStatus.OFFER_MADE);
        }

        @Test
        @DisplayName("should return property offers for client")
        void getPropertyOffers_asClient_returnsOffers() {
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId))
                    .thenReturn(List.of(sampleOffer));

            List<PropertyOfferResponseDTO> result = service.getPropertyOffers(propertyId, clientId, false);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should throw NotFoundException when property not found")
        void getPropertyOffers_propertyNotFound_throws() {
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPropertyOffers(propertyId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Property not found");
        }

        @Test
        @DisplayName("should return empty list when no offers exist")
        void getPropertyOffers_noOffers_returnsEmptyList() {
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId))
                    .thenReturn(Collections.emptyList());

            List<PropertyOfferResponseDTO> result = service.getPropertyOffers(propertyId, brokerId, true);

            assertThat(result).isEmpty();
        }
    }

    // ==================== addPropertyOffer Tests ====================

    @Nested
    @DisplayName("addPropertyOffer")
    class AddPropertyOfferTests {

        @Test
        @DisplayName("should add first property offer with round 1")
        void addPropertyOffer_firstOffer_setsRoundTo1() {
            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("480000"))
                    .status(BuyerOfferStatus.OFFER_MADE)
                    .expiryDate(LocalDate.now().plusDays(5))
                    .notes("First offer")
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findMaxOfferRoundByPropertyId(propertyId))
                    .thenReturn(null);
            when(propertyOfferRepository.save(any(PropertyOffer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(propertyRepository.save(any(Property.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PropertyOfferResponseDTO result = service.addPropertyOffer(propertyId, request, brokerId);

            assertThat(result.getOfferRound()).isEqualTo(1);
            assertThat(result.getOfferAmount()).isEqualByComparingTo(new BigDecimal("480000"));
            verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.PROPERTY_OFFER_MADE), any(), isNull(), any());
            verify(notificationService).createNotification(
                    eq(clientId.toString()),
                    eq("notifications.propertyOfferMade.title"),
                    eq("notifications.propertyOfferMade.message"),
                    anyMap(),
                    eq(transactionId.toString()),
                    eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.OFFER_MADE));
        }

        @Test
        @DisplayName("should increment offer round for subsequent offers")
        void addPropertyOffer_subsequentOffer_incrementsRound() {
            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("490000"))
                    .status(BuyerOfferStatus.OFFER_MADE)
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findMaxOfferRoundByPropertyId(propertyId))
                    .thenReturn(2);
            when(propertyOfferRepository.save(any(PropertyOffer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(propertyRepository.save(any(Property.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PropertyOfferResponseDTO result = service.addPropertyOffer(propertyId, request, brokerId);

            assertThat(result.getOfferRound()).isEqualTo(3);
        }

        @Test
        @DisplayName("should sync offer amount and status to property")
        void addPropertyOffer_syncsToProperty() {
            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("495000"))
                    .status(BuyerOfferStatus.OFFER_MADE)
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findMaxOfferRoundByPropertyId(propertyId))
                    .thenReturn(null);
            when(propertyOfferRepository.save(any(PropertyOffer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(propertyRepository.save(any(Property.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.addPropertyOffer(propertyId, request, brokerId);

            verify(propertyRepository).save(argThat(p ->
                    p.getOfferAmount().compareTo(new BigDecimal("495000")) == 0 &&
                    p.getOfferStatus() != null
            ));
        }

        @Test
        @DisplayName("should throw NotFoundException when property not found")
        void addPropertyOffer_propertyNotFound_throws() {
            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("450000"))
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addPropertyOffer(propertyId, request, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Property not found");
        }
    }

    // ==================== updatePropertyOffer Tests ====================

    @Nested
    @DisplayName("updatePropertyOffer")
    class UpdatePropertyOfferTests {

        @Test
        @DisplayName("should update property offer successfully")
        void updatePropertyOffer_validRequest_updatesOffer() {
            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("485000"))
                    .status(BuyerOfferStatus.ACCEPTED)
                    .counterpartyResponse(CounterpartyResponse.ACCEPTED)
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                    .thenReturn(Optional.of(sampleOffer));
            when(propertyOfferRepository.save(any(PropertyOffer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PropertyOfferResponseDTO result = service.updatePropertyOffer(propertyId, propertyOfferId, request, brokerId);

            assertThat(result.getOfferAmount()).isEqualByComparingTo(new BigDecimal("485000"));
            assertThat(result.getStatus()).isEqualTo(BuyerOfferStatus.ACCEPTED);
            verify(timelineService).addEntry(eq(transactionId), eq(brokerId), eq(TimelineEntryType.PROPERTY_OFFER_UPDATED), any(), isNull(), any());
            verify(notificationService).createNotification(
                    eq(clientId.toString()),
                    eq("notifications.propertyOfferStatusChanged.title"),
                    eq("notifications.propertyOfferStatusChanged.message"),
                    anyMap(),
                    eq(transactionId.toString()),
                    eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.OFFER_STATUS_CHANGED));
        }

        @Test
        @DisplayName("should send OFFER_COUNTERED notification when countered")
        void updatePropertyOffer_countered_sendsNotification() {
            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("490000"))
                    .status(BuyerOfferStatus.COUNTERED)
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                    .thenReturn(Optional.of(sampleOffer));
            when(propertyOfferRepository.save(any(PropertyOffer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.updatePropertyOffer(propertyId, propertyOfferId, request, brokerId);

            verify(notificationService).createNotification(
                    eq(clientId.toString()),
                    anyString(),
                    anyString(),
                    anyMap(),
                    eq(transactionId.toString()),
                    eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.OFFER_COUNTERED));
        }

        @Test
        @DisplayName("should throw NotFoundException when offer not found")
        void updatePropertyOffer_offerNotFound_throws() {
            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("485000"))
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePropertyOffer(propertyId, propertyOfferId, request, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Property offer not found");
        }

        @Test
        @DisplayName("should throw BadRequestException when offer belongs to different property")
        void updatePropertyOffer_wrongProperty_throws() {
            UUID otherPropertyId = UUID.randomUUID();
            sampleOffer.setPropertyId(otherPropertyId);

            PropertyOfferRequestDTO request = PropertyOfferRequestDTO.builder()
                    .offerAmount(new BigDecimal("485000"))
                    .build();

            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(sampleProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyOfferRepository.findByPropertyOfferId(propertyOfferId))
                    .thenReturn(Optional.of(sampleOffer));

            assertThatThrownBy(() -> service.updatePropertyOffer(propertyId, propertyOfferId, request, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong");
        }
    }
}
