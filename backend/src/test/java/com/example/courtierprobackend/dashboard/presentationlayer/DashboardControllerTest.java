package com.example.courtierprobackend.dashboard.presentationlayer;

import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.datalayer.*;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardController.
 * Covers broker stats, expiring offers, and pending documents endpoints.
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private DocumentRequestRepository documentRequestRepository;
    @Mock
    private PropertyOfferRepository propertyOfferRepository;
    @Mock
    private OfferRepository offerRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService timelineService;
    @Mock
    private com.example.courtierprobackend.transactions.businesslayer.TransactionService transactionService;

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(
                transactionRepository,
                userRepository,
                documentRequestRepository,
                propertyOfferRepository,
                offerRepository,
                propertyRepository,
                timelineService,
                transactionService
        );
    }

    // ========== Client Dashboard Tests ==========

    @Test
    void getClientStats_WithActiveTransactions_ReturnsCorrectCount() {
        UUID internalId = UUID.randomUUID();
        Transaction tx1 = Transaction.builder().clientId(internalId).status(TransactionStatus.ACTIVE).build();
        Transaction tx2 = Transaction.builder().clientId(internalId).status(TransactionStatus.ACTIVE).build();
        Transaction tx3 = Transaction.builder().clientId(internalId).status(TransactionStatus.CLOSED_SUCCESSFULLY).build();
        when(transactionRepository.findAllByClientId(internalId)).thenReturn(List.of(tx1, tx2, tx3));

        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        ResponseEntity<DashboardController.ClientDashboardStats> response = controller.getClientStats(null, null, request);

        assertThat(response.getBody().getActiveTransactions()).isEqualTo(2);
    }

    @Test
    void getClientStats_WithHeaderId_UsesHeaderId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UUID clientId = UUID.randomUUID();
        String clientHeader = clientId.toString();
        Transaction tx = Transaction.builder().clientId(clientId).status(TransactionStatus.ACTIVE).build();
        when(transactionRepository.findAllByClientId(clientId)).thenReturn(List.of(tx));

        ResponseEntity<DashboardController.ClientDashboardStats> response = controller.getClientStats(clientHeader, null, request);

        assertThat(response.getBody().getActiveTransactions()).isEqualTo(1);
    }

    @Test
    void getClientStats_WithNoTransactions_ReturnsZero() {
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        when(transactionRepository.findAllByClientId(internalId)).thenReturn(List.of());

        ResponseEntity<DashboardController.ClientDashboardStats> response = controller.getClientStats(null, null, request);

        assertThat(response.getBody().getActiveTransactions()).isZero();
    }

    // ========== Broker Dashboard Tests ==========

    @Test
    void getBrokerStats_WithActiveTransactions_ReturnsCorrectStats() {
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        UUID txId1 = UUID.randomUUID();
        UUID txId2 = UUID.randomUUID();
        Transaction tx1 = Transaction.builder()
                .transactionId(txId1)
                .brokerId(internalId)
                .clientId(UUID.randomUUID())
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        Transaction tx2 = Transaction.builder()
                .transactionId(txId2)
                .brokerId(internalId)
                .clientId(UUID.randomUUID())
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(internalId)).thenReturn(List.of(tx1, tx2));
        when(documentRequestRepository.findAll()).thenReturn(List.of());
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(null, null, request);

        assertThat(response.getBody().getActiveTransactions()).isEqualTo(2);
        assertThat(response.getBody().getActiveClients()).isEqualTo(2);
        assertThat(response.getBody().getTotalCommission()).isEqualTo(10000.0);
        assertThat(response.getBody().getPendingDocumentReviews()).isZero();
        assertThat(response.getBody().getExpiringOffersCount()).isZero();
    }

    @Test
    void getBrokerStats_WithSameClientMultipleTransactions_CountsUniqueClients() {
        UUID internalId = UUID.randomUUID();
        UUID clientUuid = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        
        UUID txId1 = UUID.randomUUID();
        UUID txId2 = UUID.randomUUID();
        Transaction tx1 = Transaction.builder()
                .transactionId(txId1)
                .brokerId(internalId)
                .clientId(clientUuid)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        Transaction tx2 = Transaction.builder()
                .transactionId(txId2)
                .brokerId(internalId)
                .clientId(clientUuid)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(internalId)).thenReturn(List.of(tx1, tx2));
        when(documentRequestRepository.findAll()).thenReturn(List.of());
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(null, null, request);

        assertThat(response.getBody().getActiveClients()).isEqualTo(1);
    }

    @Test
    void getBrokerStats_WithPendingDocuments_ReturnsCorrectCount() {
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        DocumentRequest doc1 = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(txId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .build())
                .status(DocumentStatusEnum.SUBMITTED)
                .build();
        DocumentRequest doc2 = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(txId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .build())
                .status(DocumentStatusEnum.APPROVED)
                .build();
        when(documentRequestRepository.findAll()).thenReturn(List.of(doc1, doc2));
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(null, null, request);

        assertThat(response.getBody().getPendingDocumentReviews()).isEqualTo(1);
    }

    @Test
    void getBrokerStats_WithExpiringOffers_ReturnsCorrectCount() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(UUID.randomUUID())
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
        when(documentRequestRepository.findAll()).thenReturn(List.of());

        Offer expiringOffer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(txId)
                .expiryDate(LocalDate.now().plusDays(3))
                .status(ReceivedOfferStatus.PENDING)
                .build();
        Offer notExpiringOffer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(txId)
                .expiryDate(LocalDate.now().plusDays(30))
                .status(ReceivedOfferStatus.PENDING)
                .build();
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(txId))
                .thenReturn(List.of(expiringOffer, notExpiringOffer));

        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(null, null, request);

        assertThat(response.getBody().getExpiringOffersCount()).isEqualTo(1);
    }

    // ========== Expiring Offers Endpoint Tests ==========

    @Test
    void getExpiringOffers_ReturnsOffersExpiringSoon() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .propertyAddress(new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1"))
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        UserAccount client = new UserAccount("auth0|123", "test@test.com", "John", "Doe", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        Offer expiringOffer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(txId)
                .buyerName("Buyer Smith")
                .offerAmount(new BigDecimal("500000"))
                .expiryDate(LocalDate.now().plusDays(2))
                .status(ReceivedOfferStatus.PENDING)
                .build();
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(txId))
                .thenReturn(List.of(expiringOffer));

        ResponseEntity<List<ExpiringOfferDTO>> response = controller.getExpiringOffers(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        ExpiringOfferDTO dto = response.getBody().get(0);
        assertThat(dto.getDaysUntilExpiry()).isEqualTo(2);
        assertThat(dto.getOfferType()).isEqualTo("SELL_SIDE");
        assertThat(dto.getPropertyAddress()).isEqualTo("123 Main St");
        assertThat(dto.getClientName()).isEqualTo("John Doe");
    }

    @Test
    void getExpiringOffers_SortsByUrgency() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(UUID.randomUUID())
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        Offer offer5Days = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(txId)
                .expiryDate(LocalDate.now().plusDays(5))
                .status(ReceivedOfferStatus.PENDING)
                .build();
        Offer offer1Day = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(txId)
                .expiryDate(LocalDate.now().plusDays(1))
                .status(ReceivedOfferStatus.PENDING)
                .build();
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(txId))
                .thenReturn(List.of(offer5Days, offer1Day));

        ResponseEntity<List<ExpiringOfferDTO>> response = controller.getExpiringOffers(null, null, request);

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getDaysUntilExpiry()).isEqualTo(1);
        assertThat(response.getBody().get(1).getDaysUntilExpiry()).isEqualTo(5);
    }

    @Test
    void getExpiringOffers_ExcludesExpiredOffers() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(UUID.randomUUID())
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        Offer expiredOffer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(txId)
                .expiryDate(LocalDate.now().minusDays(1))
                .status(ReceivedOfferStatus.PENDING)
                .build();
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(txId))
                .thenReturn(List.of(expiredOffer));

        ResponseEntity<List<ExpiringOfferDTO>> response = controller.getExpiringOffers(null, null, request);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getExpiringOffers_IncludesBuySidePropertyOffers() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(UUID.randomUUID())
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.BUY_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        Property property = Property.builder()
                .propertyId(propertyId)
                .transactionId(txId)
                .address(new PropertyAddress("456 Buyer St", "Laval", "QC", "H2B 2B2"))
                .build();
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId)).thenReturn(List.of(property));

        PropertyOffer propertyOffer = PropertyOffer.builder()
                .propertyOfferId(UUID.randomUUID())
                .propertyId(propertyId)
                .offerAmount(new BigDecimal("450000"))
                .expiryDate(LocalDate.now().plusDays(4))
                .status(BuyerOfferStatus.OFFER_MADE)
                .build();
        when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId))
                .thenReturn(List.of(propertyOffer));

        ResponseEntity<List<ExpiringOfferDTO>> response = controller.getExpiringOffers(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        ExpiringOfferDTO dto = response.getBody().get(0);
        assertThat(dto.getOfferType()).isEqualTo("BUY_SIDE");
        assertThat(dto.getPropertyAddress()).isEqualTo("456 Buyer St");
    }

    // ========== Pending Documents Endpoint Tests ==========

    @Test
    void getPendingDocuments_ReturnsSubmittedDocuments() {
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .propertyAddress(new PropertyAddress("789 Doc St", "Quebec", "QC", "G1A 1A1"))
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        UserAccount client = new UserAccount("auth0|456", "client@test.com", "Jane", "Smith", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        DocumentRequest submittedDoc = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(txId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .build())
                .status(DocumentStatusEnum.SUBMITTED)
                .docType(DocumentTypeEnum.ID_VERIFICATION)
                .lastUpdatedAt(LocalDateTime.now().minusHours(2))
                .build();
        when(documentRequestRepository.findAll()).thenReturn(List.of(submittedDoc));

        ResponseEntity<List<PendingDocumentDTO>> response = controller.getPendingDocuments(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        PendingDocumentDTO dto = response.getBody().get(0);
        assertThat(dto.getClientName()).isEqualTo("Jane Smith");
        assertThat(dto.getDocumentType()).isEqualTo("ID_VERIFICATION");
        assertThat(dto.getPropertyAddress()).isEqualTo("789 Doc St");
    }

    @Test
    void getPendingDocuments_ExcludesNonSubmittedDocuments() {
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        DocumentRequest requestedDoc = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(txId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .build())
                .status(DocumentStatusEnum.REQUESTED)
                .build();
        DocumentRequest approvedDoc = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(txId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .build())
                .status(DocumentStatusEnum.APPROVED)
                .build();
        when(documentRequestRepository.findAll()).thenReturn(List.of(requestedDoc, approvedDoc));

        ResponseEntity<List<PendingDocumentDTO>> response = controller.getPendingDocuments(null, null, request);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getPendingDocuments_ExcludesClosedTransactions() {
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID activeTxId = UUID.randomUUID();
        UUID closedTxId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction activeTx = Transaction.builder()
                .transactionId(activeTxId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .build();
        Transaction closedTx = Transaction.builder()
                .transactionId(closedTxId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.CLOSED_SUCCESSFULLY)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(activeTx, closedTx));

        DocumentRequest closedTxDoc = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(closedTxId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .build())
                .status(DocumentStatusEnum.SUBMITTED)
                .build();
        when(documentRequestRepository.findAll()).thenReturn(List.of(closedTxDoc));

        ResponseEntity<List<PendingDocumentDTO>> response = controller.getPendingDocuments(null, null, request);

        assertThat(response.getBody()).isEmpty();
    }

    // ========== Admin Dashboard Tests ==========

    @Test
    void getAdminStats_ReturnsCorrectStats() {
        UserAccount broker1 = new UserAccount("b1", "b1@test.com", "First", "Last", UserRole.BROKER, "en");
        broker1.setActive(true);
        UserAccount broker2 = new UserAccount("b2", "b2@test.com", "First", "Last", UserRole.BROKER, "en");
        broker2.setActive(false);
        UserAccount client = new UserAccount("c1", "c1@test.com", "First", "Last", UserRole.CLIENT, "en");
        client.setActive(true);
        
        when(userRepository.count()).thenReturn(3L);
        when(userRepository.findAll()).thenReturn(List.of(broker1, broker2, client));

        ResponseEntity<DashboardController.AdminDashboardStats> response = controller.getAdminStats();

        assertThat(response.getBody().getTotalUsers()).isEqualTo(3);
        assertThat(response.getBody().getActiveBrokers()).isEqualTo(1);
        assertThat(response.getBody().getSystemHealth()).isEqualTo("99.9%");
    }

    @Test
    void getAdminStats_WithNoBrokers_ReturnsZeroActiveBrokers() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findAll()).thenReturn(List.of());

        ResponseEntity<DashboardController.AdminDashboardStats> response = controller.getAdminStats();

        assertThat(response.getBody().getActiveBrokers()).isZero();
    }

    // ========== Recent Activity Tests (Paginated Endpoint) ==========

    @Test
    void getRecentActivity_WithNoActiveTransactions_ReturnsEmptyContent() {
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction closedTx = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .brokerId(brokerId)
                .status(TransactionStatus.CLOSED_SUCCESSFULLY)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(closedTx));
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        assertThat(response.getBody().get("content")).isEqualTo(List.of());
        assertThat(response.getBody().get("totalElements")).isEqualTo(0L);
        assertThat(response.getBody().get("totalPages")).isEqualTo(1);
    }

    @Test
    void getRecentActivity_WithTimelineEntries_ReturnsActivitiesWithPaginationMetadata() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .sellerStage(SellerStage.SELLER_LISTING_PUBLISHED)
                .propertyAddress(new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1"))
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        UserAccount client = new UserAccount("auth0|123", "client@test.com", "John", "Doe", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.CREATED)
                .note("Transaction created")
                .actorName("John Broker")
                .occurredAt(java.time.Instant.now())
                .docType("ID_VERIFICATION")
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        assertThat(response.getBody().get("totalElements")).isEqualTo(1L);
        assertThat(response.getBody().get("page")).isEqualTo(0);
        assertThat(response.getBody().get("size")).isEqualTo(10);
        assertThat(response.getBody().get("first")).isEqualTo(true);
        assertThat(response.getBody().get("last")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).getType()).isEqualTo("CREATED");
        assertThat(content.get(0).getPropertyAddress()).isEqualTo("123 Main St");
        assertThat(content.get(0).getClientName()).isEqualTo("John Doe");
        assertThat(content.get(0).getSide()).isEqualTo("SELL_SIDE");
        assertThat(content.get(0).getCurrentStage()).isEqualTo("SELLER_LISTING_PUBLISHED");
        assertThat(content.get(0).getDocType()).isEqualTo("ID_VERIFICATION");
    }

    @Test
    void getRecentActivity_SellSideWithNullPropertyAddress_ReturnsEmptyAddress() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .sellerStage(SellerStage.SELLER_LISTING_PUBLISHED)
                .propertyAddress(null)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(null)
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content.get(0).getPropertyAddress()).isEmpty();
        assertThat(content.get(0).getType()).isEmpty();
    }

    @Test
    void getRecentActivity_BuySideWithProperty_ReturnsPropertyAddress() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.BUY_SIDE)
                .buyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        Property property = Property.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(txId)
                .address(new PropertyAddress("456 Buyer Ave", "Laval", "QC", "H7T 1Z1"))
                .build();
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId)).thenReturn(List.of(property));

        UserAccount client = new UserAccount("auth0|456", "buyer@test.com", "Jane", "Smith", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.PROPERTY_ADDED)
                .occurredAt(java.time.Instant.now())
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content.get(0).getPropertyAddress()).isEqualTo("456 Buyer Ave");
        assertThat(content.get(0).getSide()).isEqualTo("BUY_SIDE");
        assertThat(content.get(0).getCurrentStage()).isEqualTo("BUYER_PREQUALIFY_FINANCIALLY");
        assertThat(content.get(0).getClientName()).isEqualTo("Jane Smith");
    }

    @Test
    void getRecentActivity_BuySideWithNoProperty_ReturnsNoPropertySelectedMessage() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.BUY_SIDE)
                .buyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId)).thenReturn(List.of());

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.CREATED)
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content.get(0).getPropertyAddress()).isEqualTo("No property selected");
    }

    @Test
    void getRecentActivity_BuySideWithPropertyNullAddress_ReturnsNoPropertySelectedMessage() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.BUY_SIDE)
                .buyerStage(BuyerStage.BUYER_SUBMIT_OFFER)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        Property property = Property.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(txId)
                .address(null)
                .build();
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId)).thenReturn(List.of(property));

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.PROPERTY_OFFER_MADE)
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content.get(0).getPropertyAddress()).isEqualTo("No property selected");
    }

    @Test
    void getRecentActivity_WithPagination_ReturnsCorrectPageMetadata() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .sellerStage(SellerStage.SELLER_LISTING_PUBLISHED)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        // Create 25 entries simulating page 1 of 3 (10 per page)
        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.STAGE_CHANGE)
                .build();

        // Simulating second page with 25 total elements, 10 per page = 3 pages
        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(1, 10), 25);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 1, 10);

        assertThat(response.getBody().get("page")).isEqualTo(1);
        assertThat(response.getBody().get("size")).isEqualTo(10);
        assertThat(response.getBody().get("totalElements")).isEqualTo(25L);
        assertThat(response.getBody().get("totalPages")).isEqualTo(3);
        assertThat(response.getBody().get("first")).isEqualTo(false);
        assertThat(response.getBody().get("last")).isEqualTo(false);
    }

    @Test
    void getRecentActivity_WithHeaderId_UsesHeaderId() {
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of());
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(brokerId.toString(), null, request, 0, 10);

        assertThat(response.getBody().get("content")).isEqualTo(List.of());
        verify(transactionRepository, atLeastOnce()).findAllByBrokerId(brokerId);
    }

    @Test
    void getRecentActivity_WithNullSideAndStage_ReturnsEmptyStrings() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .status(TransactionStatus.ACTIVE)
                .side(null)
                .sellerStage(null)
                .buyerStage(null)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.CREATED)
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content.get(0).getSide()).isEmpty();
        assertThat(content.get(0).getCurrentStage()).isEmpty();
    }

    @Test
    void getRecentActivity_IncludesTransactionInfo() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .sellerStage(SellerStage.SELLER_ACCEPT_BEST_OFFER)
                .propertyAddress(new PropertyAddress("789 Oak St", "Toronto", "ON", "M5V 1A1"))
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo txInfo = 
            com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo.builder()
                .previousStage("SELLER_LISTING")
                .newStage("SELLER_ACCEPT_BEST_OFFER")
                .build();

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.STAGE_CHANGE)
                .transactionInfo(txInfo)
                .occurredAt(java.time.Instant.now())
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content.get(0).getTransactionInfo()).isNotNull();
        assertThat(content.get(0).getTransactionInfo().getPreviousStage()).isEqualTo("SELLER_LISTING");
        assertThat(content.get(0).getTransactionInfo().getNewStage()).isEqualTo("SELLER_ACCEPT_BEST_OFFER");
    }

    @Test
    void getRecentActivity_WithMultipleTransactions_ReturnsEventsFromAllTransactions() {
        UUID brokerId = UUID.randomUUID();
        UUID txId1 = UUID.randomUUID();
        UUID txId2 = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx1 = Transaction.builder()
                .transactionId(txId1)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .sellerStage(SellerStage.SELLER_LISTING_PUBLISHED)
                .propertyAddress(new PropertyAddress("100 Seller St", "Montreal", "QC", "H1A 1A1"))
                .build();
        Transaction tx2 = Transaction.builder()
                .transactionId(txId2)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.BUY_SIDE)
                .buyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx1, tx2));
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId2)).thenReturn(List.of());

        UserAccount client = new UserAccount("auth0|multi", "multi@test.com", "Multi", "Client", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry1 = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId1)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.CREATED)
                .occurredAt(java.time.Instant.now().minusSeconds(60))
                .build();
        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry2 = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId2)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.CREATED)
                .occurredAt(java.time.Instant.now())
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry2, entry1), 
                org.springframework.data.domain.PageRequest.of(0, 10), 2);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content).hasSize(2);
        // First entry should be from buy-side (tx2) - most recent
        assertThat(content.get(0).getSide()).isEqualTo("BUY_SIDE");
        assertThat(content.get(0).getPropertyAddress()).isEqualTo("No property selected");
        // Second entry from sell-side (tx1)
        assertThat(content.get(1).getSide()).isEqualTo("SELL_SIDE");
        assertThat(content.get(1).getPropertyAddress()).isEqualTo("100 Seller St");
    }

    @Test
    void getRecentActivity_WithClientNotFound_ReturnsEmptyClientName() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(TransactionSide.SELL_SIDE)
                .sellerStage(SellerStage.SELLER_LISTING_PUBLISHED)
                .propertyAddress(new PropertyAddress("123 Test St", "Ottawa", "ON", "K1A 1A1"))
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
        when(userRepository.findById(clientId)).thenReturn(Optional.empty());

        com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO entry = 
            com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO.builder()
                .id(UUID.randomUUID())
                .transactionId(txId)
                .type(com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.DOCUMENT_REQUESTED)
                .docType("PROOF_OF_PURCHASE")
                .build();

        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(entry), 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(timelineService.getRecentEntriesForTransactionsPaged(any(), any())).thenReturn(page);

        ResponseEntity<java.util.Map<String, Object>> response = controller.getRecentActivity(null, null, request, 0, 10);

        @SuppressWarnings("unchecked")
        List<RecentActivityDTO> content = (List<RecentActivityDTO>) response.getBody().get("content");
        assertThat(content.get(0).getClientName()).isEmpty();
    }

    // ========== Pinned Transactions Tests ==========

    @Test
    void getPinnedTransactions_WithNoPins_ReturnsEmptyList() {
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        when(transactionService.getPinnedTransactionIds(brokerId)).thenReturn(java.util.Set.of());
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of());

        ResponseEntity<List<PinnedTransactionDTO>> response = controller.getPinnedTransactions(null, null, request);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getPinnedTransactions_WithPinnedBuySideTransaction_ReturnsCorrectStage() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE)
                .buyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY)
                .propertyAddress(new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1"))
                .build();
        
        when(transactionService.getPinnedTransactionIds(brokerId)).thenReturn(java.util.Set.of(txId));
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
        
        UserAccount client = new UserAccount("auth0|123", "client@test.com", "John", "Doe", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        ResponseEntity<List<PinnedTransactionDTO>> response = controller.getPinnedTransactions(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getSide()).isEqualTo("BUY_SIDE");
        assertThat(response.getBody().get(0).getCurrentStage()).isEqualTo("BUYER_PREQUALIFY_FINANCIALLY");
        assertThat(response.getBody().get(0).getClientName()).isEqualTo("John Doe");
    }

    @Test
    void getPinnedTransactions_WithPinnedSellSideTransaction_ReturnsCorrectStage() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.SELL_SIDE)
                .sellerStage(SellerStage.SELLER_ACCEPT_BEST_OFFER)
                .propertyAddress(new PropertyAddress("456 Oak Ave", "Toronto", "ON", "M5V 1A1"))
                .build();
        
        when(transactionService.getPinnedTransactionIds(brokerId)).thenReturn(java.util.Set.of(txId));
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
        when(userRepository.findById(clientId)).thenReturn(Optional.empty());

        ResponseEntity<List<PinnedTransactionDTO>> response = controller.getPinnedTransactions(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getSide()).isEqualTo("SELL_SIDE");
        assertThat(response.getBody().get(0).getCurrentStage()).isEqualTo("SELLER_ACCEPT_BEST_OFFER");
        assertThat(response.getBody().get(0).getClientName()).isEmpty();
    }

    @Test
    void getPinnedTransactions_WithNullSideAndStatus_ReturnsEmptyStrings() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .side(null)
                .status(null)
                .buyerStage(null)
                .sellerStage(null)
                .propertyAddress(null)
                .build();
        
        when(transactionService.getPinnedTransactionIds(brokerId)).thenReturn(java.util.Set.of(txId));
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        ResponseEntity<List<PinnedTransactionDTO>> response = controller.getPinnedTransactions(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getSide()).isEmpty();
        assertThat(response.getBody().get(0).getStatus()).isEmpty();
        assertThat(response.getBody().get(0).getPropertyAddress()).isEmpty();
    }

    @Test
    void getPinnedTransactions_WithMoreThanSixPins_ReturnsOnlySix() {
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        List<UUID> txIds = new java.util.ArrayList<>();
        List<Transaction> transactions = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID txId = UUID.randomUUID();
            txIds.add(txId);
            transactions.add(Transaction.builder()
                    .transactionId(txId)
                    .brokerId(brokerId)
                    .status(TransactionStatus.ACTIVE)
                    .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE)
                    .build());
        }
        
        when(transactionService.getPinnedTransactionIds(brokerId)).thenReturn(new java.util.HashSet<>(txIds));
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(transactions);

        ResponseEntity<List<PinnedTransactionDTO>> response = controller.getPinnedTransactions(null, null, request);

        assertThat(response.getBody()).hasSize(6);
    }

    @Test
    void getPinnedTransactions_WithHeaderId_UsesHeaderId() {
        UUID brokerId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(transactionService.getPinnedTransactionIds(brokerId)).thenReturn(java.util.Set.of());
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of());

        ResponseEntity<List<PinnedTransactionDTO>> response = controller.getPinnedTransactions(brokerId.toString(), null, request);

        assertThat(response.getBody()).isEmpty();
        verify(transactionService).getPinnedTransactionIds(brokerId);
    }

    // ========== Additional Expiring Offers Edge Case Tests ==========

    @Test
    void getExpiringOffers_WithBuySidePropertyOfferNullAddress_ReturnsEmptyAddress() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);
        LocalDate today = LocalDate.now();

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        Property property = Property.builder()
                .propertyId(propertyId)
                .transactionId(txId)
                .address(null) // null address
                .build();
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId)).thenReturn(List.of(property));

        PropertyOffer offer = PropertyOffer.builder()
                .propertyOfferId(UUID.randomUUID())
                .propertyId(propertyId)
                .expiryDate(today.plusDays(3))
                .offerAmount(BigDecimal.valueOf(500000))
                .status(null) // null status
                .build();
        when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId)).thenReturn(List.of(offer));
        when(userRepository.findById(clientId)).thenReturn(Optional.empty());

        ResponseEntity<List<ExpiringOfferDTO>> response = controller.getExpiringOffers(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPropertyAddress()).isEmpty();
        assertThat(response.getBody().get(0).getStatus()).isEmpty();
        assertThat(response.getBody().get(0).getClientName()).isEmpty();
    }

    @Test
    void getExpiringOffers_WithSellSideOfferNullStatus_ReturnsEmptyStatus() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);
        LocalDate today = LocalDate.now();

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.SELL_SIDE)
                .propertyAddress(null)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        Offer offer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(txId)
                .expiryDate(today.plusDays(2))
                .offerAmount(BigDecimal.valueOf(450000))
                .status(null)
                .build();
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(txId)).thenReturn(List.of(offer));
        when(userRepository.findById(clientId)).thenReturn(Optional.empty());

        ResponseEntity<List<ExpiringOfferDTO>> response = controller.getExpiringOffers(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStatus()).isEmpty();
        assertThat(response.getBody().get(0).getPropertyAddress()).isEmpty();
    }

    @Test
    void getExpiringOffers_WithMultipleBuySideAndSellSide_ReturnsSortedByUrgency() {
        UUID brokerId = UUID.randomUUID();
        UUID buyTxId = UUID.randomUUID();
        UUID sellTxId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);
        LocalDate today = LocalDate.now();

        Transaction buyTx = Transaction.builder()
                .transactionId(buyTxId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE)
                .build();
        Transaction sellTx = Transaction.builder()
                .transactionId(sellTxId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.SELL_SIDE)
                .propertyAddress(new PropertyAddress("789 Pine St", "Vancouver", "BC", "V6B 2W2"))
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(buyTx, sellTx));

        Property property = Property.builder()
                .propertyId(propertyId)
                .transactionId(buyTxId)
                .address(new PropertyAddress("123 Elm St", "Calgary", "AB", "T2P 1A1"))
                .build();
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(buyTxId)).thenReturn(List.of(property));

        PropertyOffer buyOffer = PropertyOffer.builder()
                .propertyOfferId(UUID.randomUUID())
                .propertyId(propertyId)
                .expiryDate(today.plusDays(5))
                .offerAmount(BigDecimal.valueOf(600000))
                .status(BuyerOfferStatus.OFFER_MADE)
                .build();
        when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(propertyId)).thenReturn(List.of(buyOffer));

        Offer sellOffer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(sellTxId)
                .expiryDate(today.plusDays(1))
                .offerAmount(BigDecimal.valueOf(550000))
                .status(ReceivedOfferStatus.ACCEPTED)
                .build();
        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(sellTxId)).thenReturn(List.of(sellOffer));

        UserAccount client = new UserAccount("auth0|client", "client@test.com", "Jane", "Client", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        ResponseEntity<List<ExpiringOfferDTO>> response = controller.getExpiringOffers(null, null, request);

        assertThat(response.getBody()).hasSize(2);
        // Should be sorted by urgency - 1 day expiry before 5 day expiry
        assertThat(response.getBody().get(0).getDaysUntilExpiry()).isEqualTo(1);
        assertThat(response.getBody().get(1).getDaysUntilExpiry()).isEqualTo(5);
    }

    // ========== Additional Pending Documents Edge Cases ==========

    @Test
    void getPendingDocuments_WithNullTransactionRef_FiltersOut() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        DocumentRequest docWithNullRef = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(null)
                .status(DocumentStatusEnum.SUBMITTED)
                .build();
        DocumentRequest validDoc = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(txId)
                        .clientId(clientId)
                        .side(TransactionSide.BUY_SIDE)
                        .build())
                .status(DocumentStatusEnum.SUBMITTED)
                .docType(DocumentTypeEnum.ID_VERIFICATION)
                .customTitle("Custom Title")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        when(documentRequestRepository.findAll()).thenReturn(List.of(docWithNullRef, validDoc));

        UserAccount client = new UserAccount("auth0|c", "c@test.com", "Test", "Client", UserRole.CLIENT, "en");
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        ResponseEntity<List<PendingDocumentDTO>> response = controller.getPendingDocuments(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCustomTitle()).isEqualTo("Custom Title");
    }

    @Test
    void getPendingDocuments_WithNullPropertyAddress_ReturnsEmptyAddress() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .propertyAddress(null)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));

        DocumentRequest doc = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(TransactionRef.builder()
                        .transactionId(txId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .build())
                .status(DocumentStatusEnum.SUBMITTED)
                .docType(null)
                .build();
        when(documentRequestRepository.findAll()).thenReturn(List.of(doc));
        when(userRepository.findById(clientId)).thenReturn(Optional.empty());

        ResponseEntity<List<PendingDocumentDTO>> response = controller.getPendingDocuments(null, null, request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPropertyAddress()).isEmpty();
        assertThat(response.getBody().get(0).getDocumentType()).isEqualTo("OTHER");
    }

    // ========== Broker Stats Edge Cases ==========

    @Test
    void getBrokerStats_WithNullTransactionRef_FiltersOutFromPendingCount() {
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(brokerId);

        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .brokerId(brokerId)
                .clientId(clientId)
                .status(TransactionStatus.ACTIVE)
                .side(com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE)
                .build();
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId)).thenReturn(List.of());

        DocumentRequest docWithNullRef = DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(null)
                .status(DocumentStatusEnum.SUBMITTED)
                .build();
        when(documentRequestRepository.findAll()).thenReturn(List.of(docWithNullRef));

        ResponseEntity<DashboardController.BrokerDashboardStats> response = controller.getBrokerStats(null, null, request);

        assertThat(response.getBody().getPendingDocumentReviews()).isZero();
    }

    // ========== Helper Methods ==========

    private MockHttpServletRequest createRequestWithInternalId(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        return request;
    }
}

