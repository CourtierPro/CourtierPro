package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.businesslayer.TransactionServiceImpl;
import com.example.courtierprobackend.transactions.datalayer.*;
import com.example.courtierprobackend.transactions.datalayer.dto.*;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.infrastructure.storage.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for condition linking in TransactionServiceImpl.
 * Tests the addOffer, updateOffer, addPropertyOffer, updatePropertyOffer methods.
 */
@ExtendWith(MockitoExtension.class)
class ConditionLinkingServiceUnitTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private PinnedTransactionRepository pinnedTransactionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private TimelineService timelineService;
    @Mock private TransactionParticipantRepository participantRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private ConditionRepository conditionRepository;
    @Mock private PropertyOfferRepository propertyOfferRepository;
    @Mock private OfferDocumentRepository offerDocumentRepository;
    @Mock private OfferRevisionRepository offerRevisionRepository;
    @Mock private ObjectStorageService objectStorageService;
    @Mock private DocumentRepository documentRequestRepository;
    @Mock private DocumentConditionLinkRepository documentConditionLinkRepository;
    @Mock private SearchCriteriaRepository searchCriteriaRepository;
    @Mock private com.example.courtierprobackend.appointments.datalayer.AppointmentRepository appointmentRepository;
    @Mock private com.example.courtierprobackend.transactions.datalayer.repositories.VisitorRepository visitorRepository;

    private TransactionServiceImpl transactionService;

    private UUID transactionId;
    private UUID brokerId;
    private UUID clientId;
    private UUID conditionId1;
    private UUID conditionId2;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                pinnedTransactionRepository,
                userAccountRepository,
                emailService,
                notificationService,
                timelineService,
                participantRepository,
                propertyRepository,
                offerRepository,
                conditionRepository,
                propertyOfferRepository,
                offerDocumentRepository,
                offerRevisionRepository,
                objectStorageService,
                documentRequestRepository,
                documentConditionLinkRepository,
                searchCriteriaRepository,
                appointmentRepository,
                visitorRepository
        );

        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        conditionId1 = UUID.randomUUID();
        conditionId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("addOffer should save condition links when conditionIds provided")
    void addOffer_withConditionIds_savesLinks() {
        // Arrange
        Transaction tx = createTestTransaction(TransactionSide.SELL_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        
        Offer savedOffer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(transactionId)
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(offerRepository.save(any(Offer.class))).thenReturn(savedOffer);
        when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(documentConditionLinkRepository.findByOfferId(any())).thenReturn(List.of());

        OfferRequestDTO dto = OfferRequestDTO.builder()
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .conditionIds(Arrays.asList(conditionId1, conditionId2))
                .build();

        // Act
        transactionService.addOffer(transactionId, dto, brokerId);

        // Assert
        ArgumentCaptor<DocumentConditionLink> linkCaptor = ArgumentCaptor.forClass(DocumentConditionLink.class);
        verify(documentConditionLinkRepository, times(2)).save(linkCaptor.capture());
        
        List<DocumentConditionLink> savedLinks = linkCaptor.getAllValues();
        assertThat(savedLinks).hasSize(2);
        assertThat(savedLinks).extracting("conditionId")
                .containsExactlyInAnyOrder(conditionId1, conditionId2);
        assertThat(savedLinks).allMatch(link -> link.getOfferId().equals(savedOffer.getOfferId()));
    }

    @Test
    @DisplayName("addOffer should not save condition links when conditionIds is null")
    void addOffer_noConditionIds_doesNotSaveLinks() {
        // Arrange
        Transaction tx = createTestTransaction(TransactionSide.SELL_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        
        Offer savedOffer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(transactionId)
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(offerRepository.save(any(Offer.class))).thenReturn(savedOffer);
        when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(documentConditionLinkRepository.findByOfferId(any())).thenReturn(List.of());

        OfferRequestDTO dto = OfferRequestDTO.builder()
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .conditionIds(null)
                .build();

        // Act
        transactionService.addOffer(transactionId, dto, brokerId);

        // Assert
        verify(documentConditionLinkRepository, never()).save(any(DocumentConditionLink.class));
    }

    @Test
    @DisplayName("updateOffer should replace condition links when conditionIds provided")
    void updateOffer_withConditionIds_replacesLinks() {
        // Arrange
        Transaction tx = createTestTransaction(TransactionSide.SELL_SIDE);
        UUID offerId = UUID.randomUUID();
        
        Offer existingOffer = Offer.builder()
                .offerId(offerId)
                .transactionId(transactionId)
                .buyerName("Old Buyer")
                .offerAmount(BigDecimal.valueOf(400000))
                .status(ReceivedOfferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(offerRepository.findByOfferId(offerId)).thenReturn(Optional.of(existingOffer));
        when(offerRepository.save(any(Offer.class))).thenReturn(existingOffer);
        when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(documentConditionLinkRepository.findByOfferId(any())).thenReturn(List.of());

        OfferRequestDTO dto = OfferRequestDTO.builder()
                .buyerName("Updated Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.UNDER_REVIEW)
                .conditionIds(Arrays.asList(conditionId1))
                .build();

        // Act
        transactionService.updateOffer(transactionId, offerId, dto, brokerId);

        // Assert
        verify(documentConditionLinkRepository).deleteByOfferId(offerId);
        verify(documentConditionLinkRepository).save(any(DocumentConditionLink.class));
    }

    @Test
    @DisplayName("addPropertyOffer should save condition links when conditionIds provided")
    void addPropertyOffer_withConditionIds_savesLinks() {
        // Arrange
        UUID propertyId = UUID.randomUUID();
        Transaction tx = createTestTransaction(TransactionSide.BUY_SIDE);
        
        Property property = Property.builder()
                .propertyId(propertyId)
                .transactionId(transactionId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        PropertyOffer savedOffer = PropertyOffer.builder()
                .propertyOfferId(UUID.randomUUID())
                .propertyId(propertyId)
                .offerRound(1)
                .offerAmount(BigDecimal.valueOf(500000))
                .status(BuyerOfferStatus.OFFER_MADE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(propertyRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(property));
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(propertyOfferRepository.findMaxOfferRoundByPropertyId(propertyId)).thenReturn(null);
        when(propertyOfferRepository.save(any(PropertyOffer.class))).thenReturn(savedOffer);
        when(propertyRepository.save(any(Property.class))).thenReturn(property);
        when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(documentConditionLinkRepository.findByPropertyOfferId(any())).thenReturn(List.of());

        PropertyOfferRequestDTO dto = PropertyOfferRequestDTO.builder()
                .offerAmount(BigDecimal.valueOf(500000))
                .status(BuyerOfferStatus.OFFER_MADE)
                .conditionIds(Arrays.asList(conditionId1, conditionId2))
                .build();

        // Act
        transactionService.addPropertyOffer(propertyId, dto, brokerId);

        // Assert
        ArgumentCaptor<DocumentConditionLink> linkCaptor = ArgumentCaptor.forClass(DocumentConditionLink.class);
        verify(documentConditionLinkRepository, times(2)).save(linkCaptor.capture());
        
        List<DocumentConditionLink> savedLinks = linkCaptor.getAllValues();
        assertThat(savedLinks).hasSize(2);
        assertThat(savedLinks).extracting("conditionId")
                .containsExactlyInAnyOrder(conditionId1, conditionId2);
        assertThat(savedLinks).allMatch(link -> link.getPropertyOfferId().equals(savedOffer.getPropertyOfferId()));
    }

    @Test
    @DisplayName("toOfferResponseDTO should include linked conditions")
    void toOfferResponseDTO_includesLinkedConditions() {
        // Arrange
        Transaction tx = createTestTransaction(TransactionSide.SELL_SIDE);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        
        UUID offerId = UUID.randomUUID();
        Offer savedOffer = Offer.builder()
                .offerId(offerId)
                .transactionId(transactionId)
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(offerRepository.save(any(Offer.class))).thenReturn(savedOffer);
        when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        
        // Mock condition links
        DocumentConditionLink link = DocumentConditionLink.builder()
                .conditionId(conditionId1)
                .offerId(offerId)
                .build();
        when(documentConditionLinkRepository.findByOfferId(offerId)).thenReturn(List.of(link));
        
        // Mock condition lookup
        Condition condition = Condition.builder()
                .conditionId(conditionId1)
                .transactionId(transactionId)
                .type(ConditionType.FINANCING)
                .description("Test financing condition")
                .deadlineDate(java.time.LocalDate.now().plusDays(30))
                .status(ConditionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(conditionRepository.findByConditionId(conditionId1)).thenReturn(Optional.of(condition));

        OfferRequestDTO dto = OfferRequestDTO.builder()
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .build();

        // Act
        OfferResponseDTO response = transactionService.addOffer(transactionId, dto, brokerId);

        // Assert
        assertThat(response.getConditions()).isNotNull();
        assertThat(response.getConditions()).hasSize(1);
        assertThat(response.getConditions().get(0).getConditionId()).isEqualTo(conditionId1);
        assertThat(response.getConditions().get(0).getType()).isEqualTo(ConditionType.FINANCING);
    }

    private Transaction createTestTransaction(TransactionSide side) {
        return Transaction.builder()
                .transactionId(transactionId)
                .brokerId(brokerId)
                .clientId(clientId)
                .side(side)
                .status(TransactionStatus.ACTIVE)
                .build();
    }
}
