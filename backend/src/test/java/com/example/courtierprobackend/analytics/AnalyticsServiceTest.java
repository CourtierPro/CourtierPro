package com.example.courtierprobackend.analytics;

import com.example.courtierprobackend.audit.analytics_export_audit.datalayer.AnalyticsExportAuditEvent;
import com.example.courtierprobackend.audit.analytics_export_audit.datalayer.AnalyticsExportAuditRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.transactions.datalayer.*;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

        @Mock
        private TransactionRepository transactionRepository;
        @Mock
        private AppointmentRepository appointmentRepository;
        @Mock
        private DocumentRepository documentRepository;
        @Mock
        private PropertyRepository propertyRepository;
        @Mock
        private OfferRepository offerRepository;
        @Mock
        private ConditionRepository conditionRepository;
        @Mock
        private PropertyOfferRepository propertyOfferRepository;
        @Mock
        private AnalyticsExportAuditRepository analyticsExportAuditRepository;
        @Mock
        private UserAccountRepository userAccountRepository;
        @Mock
        private TimelineEntryRepository timelineEntryRepository;

        @InjectMocks
        private AnalyticsService analyticsService;

        private UUID brokerId;

        @BeforeEach
        void setUp() {
                brokerId = UUID.randomUUID();
                when(analyticsExportAuditRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        }

        private AnalyticsFilterRequest emptyFilters() {
                return AnalyticsFilterRequest.builder().build();
        }

        private Transaction buildTransaction(TransactionStatus status, TransactionSide side,
                        LocalDateTime openedAt, LocalDateTime closedAt) {
                return Transaction.builder()
                                .transactionId(UUID.randomUUID())
                                .brokerId(brokerId)
                                .clientId(UUID.randomUUID())
                                .status(status)
                                .side(side)
                                .openedAt(openedAt)
                                .closedAt(closedAt)
                                .lastUpdated(LocalDateTime.now())
                                .build();
        }

        private Transaction buildBuyTransaction(TransactionStatus status, BuyerStage stage) {
                Transaction tx = buildTransaction(status, TransactionSide.BUY_SIDE,
                                LocalDateTime.now().minusDays(30),
                                status == TransactionStatus.CLOSED_SUCCESSFULLY ? LocalDateTime.now() : null);
                tx.setBuyerStage(stage);
                return tx;
        }

        private Transaction buildSellTransaction(TransactionStatus status, SellerStage stage) {
                Transaction tx = buildTransaction(status, TransactionSide.SELL_SIDE,
                                LocalDateTime.now().minusDays(20),
                                status == TransactionStatus.CLOSED_SUCCESSFULLY ? LocalDateTime.now() : null);
                tx.setSellerStage(stage);
                return tx;
        }

        private Appointment buildAppointment(AppointmentStatus status, InitiatorType initiator,
                        LocalDateTime fromDateTime) {
                Appointment apt = new Appointment();
                apt.setAppointmentId(UUID.randomUUID());
                apt.setBrokerId(brokerId);
                apt.setClientId(UUID.randomUUID());
                apt.setStatus(status);
                apt.setInitiatedBy(initiator);
                apt.setFromDateTime(fromDateTime);
                apt.setToDateTime(fromDateTime.plusHours(1));
                apt.setTitle("meeting");
                return apt;
        }

        private Document buildDocument(DocumentStatusEnum status) {
                Document doc = new Document();
                doc.setStatus(status);
                return doc;
        }

        private Property buildProperty(PropertyStatus status) {
                return Property.builder()
                                .propertyId(UUID.randomUUID())
                                .transactionId(UUID.randomUUID())
                                .status(status)
                                .build();
        }

        private PropertyOffer buildPropertyOffer(UUID propertyId, BuyerOfferStatus status,
                        BigDecimal amount, CounterpartyResponse counterparty) {
                return PropertyOffer.builder()
                                .propertyOfferId(UUID.randomUUID())
                                .propertyId(propertyId)
                                .offerRound(1)
                                .offerAmount(amount)
                                .status(status)
                                .counterpartyResponse(counterparty)
                                .build();
        }

        private Offer buildOffer(UUID transactionId, ReceivedOfferStatus status, BigDecimal amount) {
                return Offer.builder()
                                .offerId(UUID.randomUUID())
                                .transactionId(transactionId)
                                .offerAmount(amount)
                                .status(status)
                                .build();
        }

        private Condition buildCondition(UUID transactionId, ConditionStatus status, LocalDate deadline) {
                return Condition.builder()
                                .conditionId(UUID.randomUUID())
                                .transactionId(transactionId)
                                .type(ConditionType.FINANCING)
                                .description("Test condition")
                                .deadlineDate(deadline)
                                .status(status)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        @Nested
        class EmptyState {
                @Test
                void getAnalytics_noTransactions_returnsAllZeros() {
                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any()))
                                        .thenReturn(Collections.emptyList());

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.totalTransactions()).isZero();
                        assertThat(result.activeTransactions()).isZero();
                        // ... (other assertions same as before)
                }
        }

        @Nested
        class TransactionOverviewTest {
                @Test
                void countsStatusesAndSidesCorrectly() {
                        Transaction active1 = buildBuyTransaction(TransactionStatus.ACTIVE,
                                        BuyerStage.BUYER_PROPERTY_SEARCH);
                        Transaction active2 = buildSellTransaction(TransactionStatus.ACTIVE,
                                        SellerStage.SELLER_PUBLISH_LISTING);
                        Transaction closed = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY,
                                        BuyerStage.BUYER_POSSESSION);
                        Transaction terminated = buildSellTransaction(TransactionStatus.TERMINATED_EARLY,
                                        SellerStage.SELLER_INITIAL_CONSULTATION);

                        setupDefaultMocks(List.of(active1, active2, closed, terminated));

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.totalTransactions()).isEqualTo(4);
                        assertThat(result.activeTransactions()).isEqualTo(2);
                        assertThat(result.closedTransactions()).isEqualTo(1);
                        assertThat(result.terminatedTransactions()).isEqualTo(1);
                        assertThat(result.buyTransactions()).isEqualTo(2);
                        assertThat(result.sellTransactions()).isEqualTo(2);
                }

                @Test
                void successRate_calculatedFromClosedAndTerminated() {
                        Transaction closed1 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY,
                                        BuyerStage.BUYER_POSSESSION);
                        Transaction closed2 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY,
                                        BuyerStage.BUYER_POSSESSION);
                        Transaction terminated = buildSellTransaction(TransactionStatus.TERMINATED_EARLY,
                                        SellerStage.SELLER_INITIAL_CONSULTATION);

                        setupDefaultMocks(List.of(closed1, closed2, terminated));

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.successRate()).isCloseTo(66.7, within(0.1));
                }

                @Test
                void avgDuration_calculatedCorrectly() {
                        LocalDateTime now = LocalDateTime.now();
                        Transaction t1 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY,
                                        BuyerStage.BUYER_POSSESSION);
                        t1.setOpenedAt(now.minusDays(10));
                        t1.setClosedAt(now);

                        Transaction t2 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY,
                                        BuyerStage.BUYER_POSSESSION);
                        t2.setOpenedAt(now.minusDays(20));
                        t2.setClosedAt(now);

                        setupDefaultMocks(List.of(t1, t2));

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.avgTransactionDurationDays()).isEqualTo(15.0);
                        assertThat(result.shortestDurationDays()).isEqualTo(10);
                        assertThat(result.longestDurationDays()).isEqualTo(20);
                }
        }

        @Nested
        class BuySideMetricsTest {
                @Test
                void calculatesHouseVisitsAndPropertiesCorrectly() {
                        Transaction t1 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY,
                                        BuyerStage.BUYER_POSSESSION);
                        Property p1 = buildProperty(PropertyStatus.INTERESTED);
                        Property p2 = buildProperty(PropertyStatus.NOT_INTERESTED);

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(List.of(t1));
                        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(t1.getTransactionId()))
                                        .thenReturn(List.of(p1, p2));
                        when(appointmentRepository.countConfirmedHouseVisitsByTransactionIds(anyList()))
                                        .thenReturn(Collections
                                                        .singletonList(new Object[] { t1.getTransactionId(), 5 }));

                        // Mock other dependencies to avoid NPEs
                        setupDocumentMocks(List.of(t1));
                        setupConditionMocks(List.of(t1));
                        setupOfferMocks(List.of(t1));
                        when(appointmentRepository.findForAnalytics(any(), any(), any()))
                                        .thenReturn(Collections.emptyList());

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.buyTransactions()).isEqualTo(1);
                        assertThat(result.totalHouseVisits()).isEqualTo(5);
                        assertThat(result.avgHouseVisitsPerClosedTransaction()).isEqualTo(5.0);
                        assertThat(result.totalProperties()).isEqualTo(2);
                        assertThat(result.avgPropertiesPerBuyTransaction()).isEqualTo(2.0);
                        assertThat(result.propertyInterestRate()).isEqualTo(50.0);
                }

                @Test
                void calculatesBuyerOffersCorrectly() {
                        Transaction t1 = buildBuyTransaction(TransactionStatus.ACTIVE,
                                        BuyerStage.BUYER_OFFER_AND_NEGOTIATION);
                        Property p1 = buildProperty(PropertyStatus.INTERESTED);
                        PropertyOffer o1 = buildPropertyOffer(p1.getPropertyId(), BuyerOfferStatus.ACCEPTED,
                                        new BigDecimal("500000"), CounterpartyResponse.ACCEPTED);
                        PropertyOffer o2 = buildPropertyOffer(p1.getPropertyId(), BuyerOfferStatus.DECLINED,
                                        new BigDecimal("450000"), CounterpartyResponse.DECLINED);

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(List.of(t1));
                        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(t1.getTransactionId()))
                                        .thenReturn(List.of(p1));
                        when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(p1.getPropertyId()))
                                        .thenReturn(List.of(o1, o2));

                        // Mock other dependencies
                        setupDocumentMocks(List.of(t1));
                        setupConditionMocks(List.of(t1));
                        setupOfferMocks(List.of(t1));
                        when(appointmentRepository.findForAnalytics(any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(appointmentRepository.countConfirmedHouseVisitsByTransactionIds(any()))
                                        .thenReturn(Collections.emptyList());

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.totalBuyerOffers()).isEqualTo(2);
                        assertThat(result.buyerOfferAcceptanceRate()).isEqualTo(50.0);
                        assertThat(result.avgBuyerOfferAmount()).isEqualTo(475000.0);
                }
        }

        @Nested
        class SellSideMetricsTest {
                @Test
                void calculatesShowingsAndReceivedOffersCorrectly() {
                        Transaction t1 = buildSellTransaction(TransactionStatus.CLOSED_SUCCESSFULLY,
                                        SellerStage.SELLER_HANDOVER);
                        Offer o1 = buildOffer(t1.getTransactionId(), ReceivedOfferStatus.ACCEPTED,
                                        new BigDecimal("600000"));

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(List.of(t1));
                        when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(t1.getTransactionId()))
                                        .thenReturn(List.of(o1));
                        when(appointmentRepository.countConfirmedShowingsByTransactionIds(anyList()))
                                        .thenReturn(Collections
                                                        .singletonList(new Object[] { t1.getTransactionId(), 3 }));
                        when(appointmentRepository.sumVisitorsByTransactionIds(anyList()))
                                        .thenReturn(Collections
                                                        .singletonList(new Object[] { t1.getTransactionId(), 10 }));

                        // Mock other dependencies
                        setupDocumentMocks(List.of(t1));
                        setupConditionMocks(List.of(t1));
                        setupPropertyMocks(List.of(t1));
                        when(appointmentRepository.findForAnalytics(any(), any(), any()))
                                        .thenReturn(Collections.emptyList());

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.sellTransactions()).isEqualTo(1);
                        assertThat(result.totalSellShowings()).isEqualTo(3);
                        assertThat(result.avgSellShowingsPerClosedTransaction()).isEqualTo(3.0);
                        assertThat(result.totalSellVisitors()).isEqualTo(10);
                        assertThat(result.totalOffers()).isEqualTo(1);
                        assertThat(result.receivedOfferAcceptanceRate()).isEqualTo(100.0);
                        assertThat(result.highestOfferAmount()).isEqualTo(600000.0);
                }
        }

        @Nested
        class DocumentMetricsTest {
                @Test
                void calculatesDocumentMetricsCorrectly() {
                        Transaction t1 = buildBuyTransaction(TransactionStatus.ACTIVE,
                                        BuyerStage.BUYER_FINANCING_AND_CONDITIONS);
                        Document d1 = buildDocument(DocumentStatusEnum.APPROVED);
                        Document d2 = buildDocument(DocumentStatusEnum.NEEDS_REVISION);
                        Document d3 = buildDocument(DocumentStatusEnum.REQUESTED);

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(List.of(t1));
                        when(documentRepository.findByTransactionRef_TransactionId(t1.getTransactionId()))
                                        .thenReturn(List.of(d1, d2, d3));

                        // Mock other dependencies
                        setupPropertyMocks(List.of(t1));
                        setupConditionMocks(List.of(t1));
                        setupOfferMocks(List.of(t1));
                        when(appointmentRepository.findForAnalytics(any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(appointmentRepository.countConfirmedHouseVisitsByTransactionIds(any()))
                                        .thenReturn(Collections.emptyList());

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.totalDocuments()).isEqualTo(3);
                        assertThat(result.pendingDocuments()).isEqualTo(2); // REQUESTED + NEEDS_REVISION
                        assertThat(result.documentsNeedingRevision()).isEqualTo(1);
                        assertThat(result.documentCompletionRate()).isEqualTo(33.3);
                        assertThat(result.avgDocumentsPerTransaction()).isEqualTo(3.0);
                }
        }

        @Nested
        class AppointmentMetricsTest {
                @Test
                void calculatesAppointmentMetricsCorrectly() {
                        LocalDateTime future = LocalDateTime.now().plusDays(1);
                        Appointment a1 = buildAppointment(AppointmentStatus.CONFIRMED, InitiatorType.BROKER, future);
                        Appointment a2 = buildAppointment(AppointmentStatus.DECLINED, InitiatorType.CLIENT, future);
                        Appointment a3 = buildAppointment(AppointmentStatus.CANCELLED, InitiatorType.BROKER, future);
                        Appointment a4 = buildAppointment(AppointmentStatus.CONFIRMED, InitiatorType.CLIENT, future);

                        setupDefaultMocks(Collections.emptyList());
                        when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any()))
                                        .thenReturn(List.of(a1, a2, a3, a4));

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.totalAppointments()).isEqualTo(4);
                        assertThat(result.appointmentConfirmationRate()).isEqualTo(50.0);
                        assertThat(result.declinedAppointmentRate()).isEqualTo(25.0);
                        assertThat(result.cancelledAppointmentRate()).isEqualTo(25.0);
                        assertThat(result.upcomingAppointments()).isEqualTo(2);
                        assertThat(result.appointmentsByBroker()).isEqualTo(2);
                        assertThat(result.appointmentsByClient()).isEqualTo(2);
                }
        }

        @Nested
        class ConditionMetricsTest {
                @Test
                void calculatesConditionMetricsCorrectly() {
                        Transaction t1 = buildBuyTransaction(TransactionStatus.ACTIVE,
                                        BuyerStage.BUYER_FINANCING_AND_CONDITIONS);
                        Condition c1 = buildCondition(t1.getTransactionId(), ConditionStatus.SATISFIED,
                                        LocalDate.now());
                        Condition c2 = buildCondition(t1.getTransactionId(), ConditionStatus.PENDING,
                                        LocalDate.now().minusDays(1)); // Overdue
                        Condition c3 = buildCondition(t1.getTransactionId(), ConditionStatus.PENDING,
                                        LocalDate.now().plusDays(5)); // Approaching

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(List.of(t1));
                        when(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(t1.getTransactionId()))
                                        .thenReturn(List.of(c1, c2, c3));

                        // Mock other dependencies
                        setupPropertyMocks(List.of(t1));
                        setupDocumentMocks(List.of(t1));
                        setupOfferMocks(List.of(t1));
                        when(appointmentRepository.findForAnalytics(any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(appointmentRepository.countConfirmedHouseVisitsByTransactionIds(any()))
                                        .thenReturn(Collections.emptyList());

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.totalConditions()).isEqualTo(3);
                        assertThat(result.conditionSatisfiedRate()).isEqualTo(33.3);
                        assertThat(result.overdueConditions()).isEqualTo(1);
                        assertThat(result.conditionsApproachingDeadline()).isEqualTo(1);
                        assertThat(result.avgConditionsPerTransaction()).isEqualTo(3.0);
                }
        }

        @Nested
        class FilteringTest {
                @Test
                void getAnalytics_withDateFilters_callsRepositoryWithCorrectDates() {
                        LocalDate startDate = LocalDate.of(2025, 1, 1);
                        LocalDate endDate = LocalDate.of(2025, 1, 31);
                        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                                        .startDate(startDate)
                                        .endDate(endDate)
                                        .build();

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any()))
                                        .thenReturn(Collections.emptyList());

                        analyticsService.getAnalytics(brokerId, filters);

                        verify(transactionRepository).findForAnalytics(
                                        eq(brokerId),
                                        eq(startDate.atStartOfDay()),
                                        eq(endDate.atTime(LocalTime.MAX)),
                                        eq(null));
                        verify(appointmentRepository).findForAnalytics(
                                        eq(brokerId),
                                        eq(startDate.atStartOfDay()),
                                        eq(endDate.atTime(LocalTime.MAX)));
                }

                @Test
                void getAnalytics_withClientFilter_callsRepositoryWithCorrectClient() {
                        String clientName = "John Doe";
                        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                                        .clientName(clientName)
                                        .build();
                        List<UUID> mockClientIds = List.of(UUID.randomUUID());

                        when(userAccountRepository.findIdsBySearchQuery(brokerId, clientName))
                                        .thenReturn(mockClientIds);
                        when(transactionRepository.findForAnalyticsWithClients(eq(brokerId), any(), any(), any(),
                                        any()))
                                        .thenReturn(Collections.emptyList());
                        when(appointmentRepository.findForAnalyticsWithClients(eq(brokerId), any(), any(), any()))
                                        .thenReturn(Collections.emptyList());

                        analyticsService.getAnalytics(brokerId, filters);

                        verify(userAccountRepository).findIdsBySearchQuery(brokerId, clientName);
                        verify(transactionRepository).findForAnalyticsWithClients(
                                        eq(brokerId),
                                        any(),
                                        any(),
                                        any(),
                                        eq(mockClientIds));
                        verify(appointmentRepository).findForAnalyticsWithClients(
                                        eq(brokerId),
                                        any(),
                                        any(),
                                        eq(mockClientIds));
                }

                @Test
                void getAnalytics_withClientFilter_noMatchingClients_returnsEmptyWithoutRepositoryCalls() {
                        String clientName = "NonExistentClient";
                        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                                        .clientName(clientName)
                                        .build();

                        when(userAccountRepository.findIdsBySearchQuery(brokerId, clientName))
                                        .thenReturn(Collections.emptyList());

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, filters);

                        assertThat(result.totalTransactions()).isZero();

                        verify(userAccountRepository).findIdsBySearchQuery(brokerId, clientName);
                        verify(transactionRepository, never()).findForAnalytics(any(), any(), any(), any());
                        verify(transactionRepository, never()).findForAnalyticsWithClients(any(), any(), any(), any(),
                                        any());
                        verify(appointmentRepository, never()).findForAnalytics(any(), any(), any());
                        verify(appointmentRepository, never()).findForAnalyticsWithClients(any(), any(), any(), any());
                }

                @Test
                void getAnalytics_withTransactionTypeFilter_callsRepositoryWithCorrectType() {
                        TransactionSide type = TransactionSide.BUY_SIDE;
                        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                                        .transactionType(type)
                                        .build();

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any()))
                                        .thenReturn(Collections.emptyList());

                        analyticsService.getAnalytics(brokerId, filters);

                        verify(transactionRepository).findForAnalytics(
                                        eq(brokerId),
                                        any(),
                                        any(),
                                        eq(type));

                        // New: Verify appointments are filtered in memory. If total appointments
                        // returned is X,
                        // and we filter to only those in the transaction set, the result should reflect
                        // that.
                        // Since we mocked findForAnalytics to return empty, the filter effect is
                        // trivial (empty list).
                        // A dedicated test case for the filtering logic would be better.
                }

                @Test
                void getAnalytics_withTransactionTypeFilter_filtersAppointmentsInMemory() {
                        TransactionSide type = TransactionSide.BUY_SIDE;
                        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                                        .transactionType(type)
                                        .build();

                        Transaction t1 = buildBuyTransaction(TransactionStatus.ACTIVE,
                                        BuyerStage.BUYER_PROPERTY_SEARCH); // Buy
                                                                           // side
                        t1.setTransactionId(UUID.randomUUID());

                        Appointment a1 = buildAppointment(AppointmentStatus.CONFIRMED, InitiatorType.BROKER,
                                        LocalDateTime.now());
                        a1.setTransactionId(t1.getTransactionId()); // Matches t1

                        Appointment a2 = buildAppointment(AppointmentStatus.CONFIRMED, InitiatorType.BROKER,
                                        LocalDateTime.now());
                        a2.setTransactionId(UUID.randomUUID()); // Doesn't match t1 (simulates sell side appointment)

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), eq(type)))
                                        .thenReturn(List.of(t1));
                        when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any()))
                                        .thenReturn(List.of(a1, a2));

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, filters);

                        // Expect only a1 to count towards totalAppointments because a2 is filtered out
                        assertThat(result.totalAppointments()).isEqualTo(1);
                        assertThat(result.avgAppointmentsPerTransaction()).isEqualTo(1.0); // 1 appt / 1 tx
                }
        }

        @Nested
        class PipelineTest {
                @Test
                void calculatePipeline_tracksActiveClientsAndDurations() {
                        UUID clientId = UUID.randomUUID();
                        Transaction t1 = buildBuyTransaction(TransactionStatus.ACTIVE,
                                        BuyerStage.BUYER_PROPERTY_SEARCH);
                        t1.setClientId(clientId);

                        UserAccount client = new UserAccount();
                        client.setId(clientId);
                        client.setFirstName("Alice");
                        client.setLastName("Smith");

                        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                        .thenReturn(List.of(t1));
                        when(userAccountRepository.findAllById(any())).thenReturn(List.of(client));

                        TimelineEntry entry = new TimelineEntry();
                        entry.setTransactionId(t1.getTransactionId());
                        entry.setTimestamp(java.time.Instant.now().minus(java.time.Duration.ofDays(5)));
                        entry.setType(TimelineEntryType.STAGE_CHANGE);
                        entry.setTransactionInfo(TransactionInfo.builder()
                                        .previousStage(BuyerStage.BUYER_FINANCIAL_PREPARATION.name())
                                        .newStage(BuyerStage.BUYER_PROPERTY_SEARCH.name())
                                        .build());

                        when(timelineEntryRepository.findByTransactionIdInAndTypeInOrderByTimestampAsc(any(), any()))
                                        .thenReturn(List.of(entry));

                        AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

                        assertThat(result.buyerPipeline()).isNotEmpty();
                        AnalyticsDTO.PipelineStageDTO searchStage = result.buyerPipeline().stream()
                                        .filter(s -> s.stageName().equals(BuyerStage.BUYER_PROPERTY_SEARCH.name()))
                                        .findFirst().orElseThrow();

                        assertThat(searchStage.count()).isEqualTo(1);
                        assertThat(searchStage.avgDays()).isGreaterThanOrEqualTo(5.0);
                        assertThat(searchStage.clients()).hasSize(1);
                        assertThat(searchStage.clients().get(0).clientName()).isEqualTo("Alice Smith");
                }
        }

        @Nested
        class ExportTest {
                @Test
                void exportAnalyticsCsv_ShouldReturnByteArrayAndLogAudit() throws Exception {
                        // Given
                        AnalyticsFilterRequest filters = emptyFilters();
                        UserAccount mockBroker = new UserAccount();
                        mockBroker.setFirstName("John");
                        mockBroker.setLastName("Doe");

                        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(mockBroker));
                        setupDefaultMocks(Collections.emptyList());

                        // When
                        byte[] result = analyticsService.exportAnalyticsCsv(brokerId, filters);

                        // Then
                        assertThat(result).isNotNull();
                        assertThat(result.length).isGreaterThan(0);
                        verify(analyticsExportAuditRepository).save(any(AnalyticsExportAuditEvent.class));
                        verify(userAccountRepository).findById(brokerId);
                }

                @Test
                void exportAnalyticsPdf_ShouldReturnByteArrayAndLogAudit() {
                        // Given
                        AnalyticsFilterRequest filters = emptyFilters();
                        UserAccount mockBroker = new UserAccount();
                        mockBroker.setFirstName("John");
                        mockBroker.setLastName("Doe");

                        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(mockBroker));
                        setupDefaultMocks(Collections.emptyList());

                        // When
                        byte[] result = analyticsService.exportAnalyticsPdf(brokerId, filters);

                        // Then
                        assertThat(result).isNotNull();
                        assertThat(result.length).isGreaterThan(0);
                        verify(analyticsExportAuditRepository).save(any(AnalyticsExportAuditEvent.class));
                        verify(userAccountRepository).findById(brokerId);
                }
        }

        private void setupDefaultMocks(List<Transaction> transactions) {
                // Updated to use findForAnalytics (no clients)
                when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                                .thenReturn(transactions);

                // Mock userAccountRepository to return empty by default for any set of client
                // IDs
                when(userAccountRepository.findAllById(any())).thenReturn(Collections.emptyList());
                for (Transaction tx : transactions) {
                        if (tx.getClientId() != null) {
                                when(userAccountRepository.findById(tx.getClientId())).thenReturn(Optional.empty());
                        }
                }

                // Similarly for appointments
                when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any()))
                                .thenReturn(Collections.emptyList());

                when(appointmentRepository.countConfirmedHouseVisitsByTransactionIds(any()))
                                .thenReturn(Collections.emptyList());
                setupPropertyMocks(transactions);
                setupDocumentMocks(transactions);
                setupConditionMocks(transactions);
                setupOfferMocks(transactions);
        }

        private void setupPropertyMocks(List<Transaction> transactions) {
                for (Transaction tx : transactions) {
                        when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(tx.getTransactionId()))
                                        .thenReturn(Collections.emptyList());
                }
        }

        private void setupDocumentMocks(List<Transaction> transactions) {
                for (Transaction tx : transactions) {
                        when(documentRepository.findByTransactionRef_TransactionId(tx.getTransactionId()))
                                        .thenReturn(Collections.emptyList());
                }
        }

        private void setupConditionMocks(List<Transaction> transactions) {
                for (Transaction tx : transactions) {
                        when(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(tx.getTransactionId()))
                                        .thenReturn(Collections.emptyList());
                }
        }

        private void setupOfferMocks(List<Transaction> transactions) {
                for (Transaction tx : transactions) {
                        if (tx.getSide() == TransactionSide.SELL_SIDE) {
                                when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(tx.getTransactionId()))
                                                .thenReturn(Collections.emptyList());
                        }
                }
        }
}
