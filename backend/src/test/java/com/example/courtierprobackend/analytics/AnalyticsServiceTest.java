package com.example.courtierprobackend.analytics;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID brokerId;

    @BeforeEach
    void setUp() {
        brokerId = UUID.randomUUID();
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
            when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(Collections.emptyList());
            when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                    .thenReturn(Collections.emptyList());

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalTransactions()).isZero();
            assertThat(result.activeTransactions()).isZero();
            assertThat(result.closedTransactions()).isZero();
            assertThat(result.terminatedTransactions()).isZero();
            assertThat(result.buyTransactions()).isZero();
            assertThat(result.sellTransactions()).isZero();
            assertThat(result.successRate()).isZero();
            assertThat(result.avgTransactionDurationDays()).isZero();
            assertThat(result.longestDurationDays()).isZero();
            assertThat(result.shortestDurationDays()).isZero();
            assertThat(result.transactionsOpenedPerMonth()).isEmpty();
            assertThat(result.transactionsClosedPerMonth()).isEmpty();
            assertThat(result.totalHouseVisits()).isZero();
            assertThat(result.avgHouseVisitsPerClosedTransaction()).isZero();
            assertThat(result.totalProperties()).isZero();
            assertThat(result.totalBuyerOffers()).isZero();
            assertThat(result.totalOffers()).isZero();
            assertThat(result.totalDocuments()).isZero();
            assertThat(result.totalAppointments()).isZero();
            assertThat(result.totalConditions()).isZero();
            assertThat(result.totalActiveClients()).isZero();
            assertThat(result.busiestMonth()).isEqualTo("—");
            assertThat(result.idleTransactions()).isZero();
        }
    }

    @Nested
    class TransactionOverviewTest {
        @Test
        void countsStatusesAndSidesCorrectly() {
            Transaction active1 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction active2 = buildSellTransaction(TransactionStatus.ACTIVE, SellerStage.SELLER_PUBLISH_LISTING);
            Transaction closed = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            Transaction terminated = buildSellTransaction(TransactionStatus.TERMINATED_EARLY, SellerStage.SELLER_INITIAL_CONSULTATION);

            setupDefaultMocks(List.of(active1, active2, closed, terminated));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalTransactions()).isEqualTo(4);
            assertThat(result.activeTransactions()).isEqualTo(2);
            assertThat(result.closedTransactions()).isEqualTo(1);
            assertThat(result.terminatedTransactions()).isEqualTo(1);
            assertThat(result.buyTransactions()).isEqualTo(2);
            assertThat(result.sellTransactions()).isEqualTo(2);
        }

        @Test
        void successRate_calculatedFromClosedAndTerminated() {
            Transaction closed1 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            Transaction closed2 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            Transaction terminated = buildSellTransaction(TransactionStatus.TERMINATED_EARLY, SellerStage.SELLER_INITIAL_CONSULTATION);

            setupDefaultMocks(List.of(closed1, closed2, terminated));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.successRate()).isCloseTo(66.7, within(0.1));
        }

        @Test
        void duration_calculatedOnlyForClosedWithDates() {
            LocalDateTime opened1 = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime closed1 = LocalDateTime.of(2025, 1, 11, 0, 0);
            LocalDateTime opened2 = LocalDateTime.of(2025, 2, 1, 0, 0);
            LocalDateTime closed2 = LocalDateTime.of(2025, 2, 21, 0, 0);

            Transaction tx1 = buildTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, TransactionSide.BUY_SIDE, opened1, closed1);
            tx1.setBuyerStage(BuyerStage.BUYER_POSSESSION);
            Transaction tx2 = buildTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, TransactionSide.SELL_SIDE, opened2, closed2);
            tx2.setSellerStage(SellerStage.SELLER_HANDOVER);
            Transaction active = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);

            setupDefaultMocks(List.of(tx1, tx2, active));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.avgTransactionDurationDays()).isEqualTo(15.0);
            assertThat(result.longestDurationDays()).isEqualTo(20);
            assertThat(result.shortestDurationDays()).isEqualTo(10);
        }

        @Test
        void monthlyGrouping_groupsByYearMonth() {
            LocalDateTime jan = LocalDateTime.of(2025, 1, 15, 10, 0);
            LocalDateTime janClosed = LocalDateTime.of(2025, 1, 30, 10, 0);
            LocalDateTime feb = LocalDateTime.of(2025, 2, 10, 10, 0);

            Transaction tx1 = buildTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, TransactionSide.BUY_SIDE, jan, janClosed);
            tx1.setBuyerStage(BuyerStage.BUYER_POSSESSION);
            Transaction tx2 = buildTransaction(TransactionStatus.ACTIVE, TransactionSide.SELL_SIDE, feb, null);
            tx2.setSellerStage(SellerStage.SELLER_PUBLISH_LISTING);

            setupDefaultMocks(List.of(tx1, tx2));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.transactionsOpenedPerMonth()).containsEntry("2025-01", 1);
            assertThat(result.transactionsOpenedPerMonth()).containsEntry("2025-02", 1);
            assertThat(result.transactionsClosedPerMonth()).containsEntry("2025-01", 1);
        }

        @Test
        void stageDistribution_groupsByStage() {
            Transaction buy1 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction buy2 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction buy3 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_OFFER_AND_NEGOTIATION);
            Transaction sell1 = buildSellTransaction(TransactionStatus.ACTIVE, SellerStage.SELLER_PUBLISH_LISTING);

            setupDefaultMocks(List.of(buy1, buy2, buy3, sell1));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.buyerStageDistribution()).containsEntry("BUYER_PROPERTY_SEARCH", 2);
            assertThat(result.buyerStageDistribution()).containsEntry("BUYER_OFFER_AND_NEGOTIATION", 1);
            assertThat(result.sellerStageDistribution()).containsEntry("SELLER_PUBLISH_LISTING", 1);
        }
    }

    @Nested
    class HouseVisitsTest {
        @Test
        void totalHouseVisits_summedAcrossBuyTransactions() {
            Transaction buy1 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction buy2 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction sell = buildSellTransaction(TransactionStatus.ACTIVE, SellerStage.SELLER_PUBLISH_LISTING);

            setupDefaultMocks(List.of(buy1, buy2, sell));
            when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(buy1.getTransactionId())).thenReturn(3);
            when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(buy2.getTransactionId())).thenReturn(5);

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalHouseVisits()).isEqualTo(8);
        }

        @Test
        void avgHouseVisits_onlyClosedBuyTransactions() {
            Transaction closed1 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            Transaction closed2 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            Transaction active = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);

            setupDefaultMocks(List.of(closed1, closed2, active));
            when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(closed1.getTransactionId())).thenReturn(4);
            when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(closed2.getTransactionId())).thenReturn(6);
            when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(active.getTransactionId())).thenReturn(2);

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.avgHouseVisitsPerClosedTransaction()).isEqualTo(5.0);
        }
    }

    @Nested
    class PropertiesTest {
        @Test
        void propertyStats_calculatedCorrectly() {
            Transaction buy = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            setupDefaultMocks(List.of(buy));

            Property pInterested = buildProperty(PropertyStatus.INTERESTED);
            pInterested.setTransactionId(buy.getTransactionId());
            Property pNotInterested = buildProperty(PropertyStatus.NOT_INTERESTED);
            pNotInterested.setTransactionId(buy.getTransactionId());
            Property pNeedsInfo = buildProperty(PropertyStatus.NEEDS_INFO);
            pNeedsInfo.setTransactionId(buy.getTransactionId());

            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(buy.getTransactionId()))
                    .thenReturn(List.of(pInterested, pNotInterested, pNeedsInfo));

            PropertyOffer offer = buildPropertyOffer(pInterested.getPropertyId(),
                    BuyerOfferStatus.OFFER_MADE, BigDecimal.valueOf(300000), null);
            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(pInterested.getPropertyId()))
                    .thenReturn(List.of(offer));
            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(pNotInterested.getPropertyId()))
                    .thenReturn(Collections.emptyList());
            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(pNeedsInfo.getPropertyId()))
                    .thenReturn(Collections.emptyList());

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalProperties()).isEqualTo(3);
            assertThat(result.avgPropertiesPerBuyTransaction()).isEqualTo(3.0);
            assertThat(result.propertiesNeedingInfo()).isEqualTo(1);
            assertThat(result.propertiesWithOffers()).isEqualTo(1);
            assertThat(result.propertiesWithoutOffers()).isEqualTo(2);
        }
    }

    @Nested
    class BuyerOffersTest {
        @Test
        void buyerOfferStats_calculatedCorrectly() {
            Transaction buy = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_OFFER_AND_NEGOTIATION);
            setupDefaultMocks(List.of(buy));

            Property p = buildProperty(PropertyStatus.INTERESTED);
            p.setTransactionId(buy.getTransactionId());
            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(buy.getTransactionId()))
                    .thenReturn(List.of(p));

            PropertyOffer accepted = buildPropertyOffer(p.getPropertyId(), BuyerOfferStatus.ACCEPTED,
                    BigDecimal.valueOf(400000), CounterpartyResponse.COUNTERED);
            PropertyOffer expired = buildPropertyOffer(p.getPropertyId(), BuyerOfferStatus.EXPIRED,
                    BigDecimal.valueOf(350000), null);
            PropertyOffer withdrawn = buildPropertyOffer(p.getPropertyId(), BuyerOfferStatus.WITHDRAWN,
                    BigDecimal.valueOf(300000), null);

            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(p.getPropertyId()))
                    .thenReturn(List.of(accepted, expired, withdrawn));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalBuyerOffers()).isEqualTo(3);
            assertThat(result.buyerOfferAcceptanceRate()).isCloseTo(33.3, within(0.1));
            assertThat(result.avgOfferRounds()).isEqualTo(3.0);
            assertThat(result.avgBuyerOfferAmount()).isCloseTo(350000, within(1.0));
            assertThat(result.expiredOrWithdrawnOffers()).isEqualTo(2);
            assertThat(result.buyerCounterOfferRate()).isCloseTo(33.3, within(0.1));
        }
    }

    @Nested
    class ReceivedOffersTest {
        @Test
        void receivedOfferStats_calculatedCorrectly() {
            Transaction sell = buildSellTransaction(TransactionStatus.ACTIVE, SellerStage.SELLER_OFFER_AND_NEGOTIATION);
            setupDefaultMocks(List.of(sell));

            Offer accepted = buildOffer(sell.getTransactionId(), ReceivedOfferStatus.ACCEPTED, BigDecimal.valueOf(500000));
            Offer pending = buildOffer(sell.getTransactionId(), ReceivedOfferStatus.PENDING, BigDecimal.valueOf(400000));
            Offer countered = buildOffer(sell.getTransactionId(), ReceivedOfferStatus.COUNTERED, BigDecimal.valueOf(450000));
            Offer underReview = buildOffer(sell.getTransactionId(), ReceivedOfferStatus.UNDER_REVIEW, BigDecimal.valueOf(420000));

            when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(sell.getTransactionId()))
                    .thenReturn(List.of(accepted, pending, countered, underReview));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalOffers()).isEqualTo(4);
            assertThat(result.receivedOfferAcceptanceRate()).isEqualTo(25.0);
            assertThat(result.avgReceivedOfferAmount()).isCloseTo(442500, within(1.0));
            assertThat(result.highestOfferAmount()).isEqualTo(500000.0);
            assertThat(result.lowestOfferAmount()).isEqualTo(400000.0);
            assertThat(result.avgOffersPerSellTransaction()).isEqualTo(4.0);
            assertThat(result.pendingOrReviewOffers()).isEqualTo(2);
            assertThat(result.receivedCounterOfferRate()).isEqualTo(25.0);
        }
    }

    @Nested
    class DocumentsTest {
        @Test
        void documentStats_excludesDrafts() {
            Transaction tx = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            setupDefaultMocks(List.of(tx));

            Document draft = buildDocument(DocumentStatusEnum.DRAFT);
            Document approved = buildDocument(DocumentStatusEnum.APPROVED);
            Document submitted = buildDocument(DocumentStatusEnum.SUBMITTED);
            Document requested = buildDocument(DocumentStatusEnum.REQUESTED);
            Document needsRevision = buildDocument(DocumentStatusEnum.NEEDS_REVISION);

            when(documentRepository.findByTransactionRef_TransactionId(tx.getTransactionId()))
                    .thenReturn(List.of(draft, approved, submitted, requested, needsRevision));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalDocuments()).isEqualTo(4);
            assertThat(result.pendingDocuments()).isEqualTo(2);
            assertThat(result.documentsNeedingRevision()).isEqualTo(1);
            assertThat(result.documentCompletionRate()).isEqualTo(50.0);
            assertThat(result.avgDocumentsPerTransaction()).isEqualTo(4.0);
        }
    }

    @Nested
    class AppointmentsTest {
        @Test
        void appointmentStats_calculatedCorrectly() {
            Transaction tx = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
            setupPropertyMocks(List.of(tx));
            setupDocumentMocks(List.of(tx));
            setupConditionMocks(List.of(tx));
            setupOfferMocks(List.of(tx));
            when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(any())).thenReturn(0);

            Appointment confirmed = buildAppointment(AppointmentStatus.CONFIRMED, InitiatorType.BROKER, LocalDateTime.now().plusDays(5));
            Appointment declined = buildAppointment(AppointmentStatus.DECLINED, InitiatorType.CLIENT, LocalDateTime.now().minusDays(2));
            Appointment cancelled = buildAppointment(AppointmentStatus.CANCELLED, InitiatorType.BROKER, LocalDateTime.now().minusDays(1));
            Appointment proposed = buildAppointment(AppointmentStatus.PROPOSED, InitiatorType.CLIENT, LocalDateTime.now().minusDays(3));

            when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                    .thenReturn(List.of(confirmed, declined, cancelled, proposed));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalAppointments()).isEqualTo(4);
            assertThat(result.appointmentConfirmationRate()).isEqualTo(25.0);
            assertThat(result.declinedAppointmentRate()).isEqualTo(25.0);
            assertThat(result.cancelledAppointmentRate()).isEqualTo(25.0);
            assertThat(result.upcomingAppointments()).isEqualTo(1);
            assertThat(result.avgAppointmentsPerTransaction()).isEqualTo(4.0);
        }

        @Test
        void appointmentInitiators_countedCorrectly() {
            Transaction tx = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(List.of(tx));
            setupPropertyMocks(List.of(tx));
            setupDocumentMocks(List.of(tx));
            setupConditionMocks(List.of(tx));
            setupOfferMocks(List.of(tx));
            when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(any())).thenReturn(0);

            Appointment byBroker1 = buildAppointment(AppointmentStatus.CONFIRMED, InitiatorType.BROKER, LocalDateTime.now().minusDays(1));
            Appointment byBroker2 = buildAppointment(AppointmentStatus.PROPOSED, InitiatorType.BROKER, LocalDateTime.now().minusDays(2));
            Appointment byClient = buildAppointment(AppointmentStatus.CONFIRMED, InitiatorType.CLIENT, LocalDateTime.now().minusDays(3));

            when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                    .thenReturn(List.of(byBroker1, byBroker2, byClient));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.appointmentsByBroker()).isEqualTo(2);
            assertThat(result.appointmentsByClient()).isEqualTo(1);
        }
    }

    @Nested
    class ConditionsTest {
        @Test
        void conditionStats_calculatedCorrectly() {
            Transaction tx = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_FINANCING_AND_CONDITIONS);
            setupDefaultMocks(List.of(tx));

            Condition satisfied = buildCondition(tx.getTransactionId(), ConditionStatus.SATISFIED, LocalDate.now().minusDays(5));
            Condition pending = buildCondition(tx.getTransactionId(), ConditionStatus.PENDING, LocalDate.now().plusDays(3));
            Condition overdue = buildCondition(tx.getTransactionId(), ConditionStatus.PENDING, LocalDate.now().minusDays(1));
            Condition failed = buildCondition(tx.getTransactionId(), ConditionStatus.FAILED, LocalDate.now().minusDays(10));
            Condition futureOk = buildCondition(tx.getTransactionId(), ConditionStatus.PENDING, LocalDate.now().plusDays(30));

            when(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(tx.getTransactionId()))
                    .thenReturn(List.of(satisfied, pending, overdue, failed, futureOk));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalConditions()).isEqualTo(5);
            assertThat(result.conditionSatisfiedRate()).isEqualTo(20.0);
            assertThat(result.conditionsApproachingDeadline()).isEqualTo(1);
            assertThat(result.overdueConditions()).isEqualTo(1);
            assertThat(result.avgConditionsPerTransaction()).isEqualTo(5.0);
        }
    }

    @Nested
    class ClientEngagementTest {
        @Test
        void clientEngagement_calculatedCorrectly() {
            UUID client1 = UUID.randomUUID();
            UUID client2 = UUID.randomUUID();

            Transaction tx1 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            tx1.setClientId(client1);
            Transaction tx2 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_OFFER_AND_NEGOTIATION);
            tx2.setClientId(client1);
            Transaction tx3 = buildSellTransaction(TransactionStatus.ACTIVE, SellerStage.SELLER_PUBLISH_LISTING);
            tx3.setClientId(client2);
            Transaction closed = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            closed.setClientId(client2);

            setupDefaultMocks(List.of(tx1, tx2, tx3, closed));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.totalActiveClients()).isEqualTo(2);
            assertThat(result.clientsWithMultipleTransactions()).isEqualTo(1);
        }
    }

    @Nested
    class TrendsTest {
        @Test
        void busiestMonth_correctlyIdentified() {
            LocalDateTime jan1 = LocalDateTime.of(2025, 1, 5, 10, 0);
            LocalDateTime jan2 = LocalDateTime.of(2025, 1, 15, 10, 0);
            LocalDateTime feb1 = LocalDateTime.of(2025, 2, 10, 10, 0);

            Transaction tx1 = buildTransaction(TransactionStatus.ACTIVE, TransactionSide.BUY_SIDE, jan1, null);
            tx1.setBuyerStage(BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction tx2 = buildTransaction(TransactionStatus.ACTIVE, TransactionSide.BUY_SIDE, jan2, null);
            tx2.setBuyerStage(BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction tx3 = buildTransaction(TransactionStatus.ACTIVE, TransactionSide.SELL_SIDE, feb1, null);
            tx3.setSellerStage(SellerStage.SELLER_PUBLISH_LISTING);

            setupDefaultMocks(List.of(tx1, tx2, tx3));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.busiestMonth()).isEqualTo("2025-01");
        }

        @Test
        void idleTransactions_identifiesStaleActive() {
            Transaction idle = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            idle.setLastUpdated(LocalDateTime.now().minusDays(45));

            Transaction recent = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            recent.setLastUpdated(LocalDateTime.now().minusDays(5));

            Transaction closedOld = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            closedOld.setLastUpdated(LocalDateTime.now().minusDays(60));

            setupDefaultMocks(List.of(idle, recent, closedOld));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId);

            assertThat(result.idleTransactions()).isEqualTo(1);
        }
    }

    private void setupDefaultMocks(List<Transaction> transactions) {
        when(transactionRepository.findAllByBrokerId(brokerId)).thenReturn(transactions);
        when(appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.countConfirmedHouseVisitsByTransactionId(any())).thenReturn(0);
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
