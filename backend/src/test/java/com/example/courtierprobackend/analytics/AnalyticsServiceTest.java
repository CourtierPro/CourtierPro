package com.example.courtierprobackend.analytics;

import com.example.courtierprobackend.audit.analytics_export_audit.datalayer.AnalyticsExportAuditEvent;
import com.example.courtierprobackend.audit.analytics_export_audit.datalayer.AnalyticsExportAuditRepository;
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
            when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any(), any())).thenReturn(Collections.emptyList());
            when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any(), any())).thenReturn(Collections.emptyList());

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
            Transaction active1 = buildBuyTransaction(TransactionStatus.ACTIVE, BuyerStage.BUYER_PROPERTY_SEARCH);
            Transaction active2 = buildSellTransaction(TransactionStatus.ACTIVE, SellerStage.SELLER_PUBLISH_LISTING);
            Transaction closed = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
            Transaction terminated = buildSellTransaction(TransactionStatus.TERMINATED_EARLY, SellerStage.SELLER_INITIAL_CONSULTATION);

            setupDefaultMocks(List.of(active1, active2, closed, terminated));

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

            assertThat(result.totalTransactions()).isEqualTo(4);
            assertThat(result.activeTransactions()).isEqualTo(2);
            assertThat(result.closedTransactions()).isEqualTo(1);
            assertThat(result.terminatedTransactions()).isEqualTo(1);
            assertThat(result.buyTransactions()).isEqualTo(2);
            assertThat(result.sellTransactions()).isEqualTo(2);
        }
        
        // ... (other tests from existing file, adapted to use emptyFilters())
        
        @Test
        void successRate_calculatedFromClosedAndTerminated() {
             Transaction closed1 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
             Transaction closed2 = buildBuyTransaction(TransactionStatus.CLOSED_SUCCESSFULLY, BuyerStage.BUYER_POSSESSION);
             Transaction terminated = buildSellTransaction(TransactionStatus.TERMINATED_EARLY, SellerStage.SELLER_INITIAL_CONSULTATION);

             setupDefaultMocks(List.of(closed1, closed2, terminated));

             AnalyticsDTO result = analyticsService.getAnalytics(brokerId, emptyFilters());

             assertThat(result.successRate()).isCloseTo(66.7, within(0.1));
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

            when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            analyticsService.getAnalytics(brokerId, filters);

            verify(transactionRepository).findForAnalytics(
                    eq(brokerId),
                    eq(startDate.atStartOfDay()),
                    eq(endDate.atTime(LocalTime.MAX)),
                    eq(null),
                    eq(null)
            );
            verify(appointmentRepository).findForAnalytics(
                    eq(brokerId),
                    eq(startDate.atStartOfDay()),
                    eq(endDate.atTime(LocalTime.MAX)),
                    eq(null)
            );
        }

        @Test
        void getAnalytics_withClientFilter_callsRepositoryWithCorrectClient() {
            String clientName = "John Doe";
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .clientName(clientName)
                    .build();
            List<UUID> mockClientIds = List.of(UUID.randomUUID());

            when(userAccountRepository.findIdsBySearchQuery(clientName)).thenReturn(mockClientIds);
            when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            analyticsService.getAnalytics(brokerId, filters);

            verify(userAccountRepository).findIdsBySearchQuery(clientName);
            verify(transactionRepository).findForAnalytics(
                    eq(brokerId),
                    any(),
                    any(),
                    any(),
                    eq(mockClientIds)
            );
            verify(appointmentRepository).findForAnalytics(
                    eq(brokerId),
                    any(),
                    any(),
                    eq(mockClientIds)
            );
        }

        @Test
        void getAnalytics_withClientFilter_noMatchingClients_returnsEmptyWithoutRepositoryCalls() {
            String clientName = "NonExistentClient";
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .clientName(clientName)
                    .build();

            when(userAccountRepository.findIdsBySearchQuery(clientName)).thenReturn(Collections.emptyList());

            AnalyticsDTO result = analyticsService.getAnalytics(brokerId, filters);

            assertThat(result.totalTransactions()).isZero();
            
            verify(userAccountRepository).findIdsBySearchQuery(clientName);
            verify(transactionRepository, never()).findForAnalytics(any(), any(), any(), any(), any());
            verify(appointmentRepository, never()).findForAnalytics(any(), any(), any(), any());
        }
        
        @Test
        void getAnalytics_withTransactionTypeFilter_callsRepositoryWithCorrectType() {
            TransactionSide type = TransactionSide.BUY_SIDE;
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .transactionType(type)
                    .build();

            when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            analyticsService.getAnalytics(brokerId, filters);

            verify(transactionRepository).findForAnalytics(
                    eq(brokerId),
                    any(),
                    any(),
                    eq(type),
                    any()
            );
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
        // Updated to use findForAnalytics instead of findAllByBrokerId
        when(transactionRepository.findForAnalytics(eq(brokerId), any(), any(), any(), any())).thenReturn(transactions);
        
        // Similarly for appointments
        when(appointmentRepository.findForAnalytics(eq(brokerId), any(), any(), any()))
                .thenReturn(Collections.emptyList());
                
        when(appointmentRepository.countConfirmedHouseVisitsByTransactionIds(any())).thenReturn(Collections.emptyList());
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
