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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final AppointmentRepository appointmentRepository;
    private final DocumentRepository documentRepository;
    private final PropertyRepository propertyRepository;
    private final OfferRepository offerRepository;
    private final ConditionRepository conditionRepository;
    private final PropertyOfferRepository propertyOfferRepository;

    public AnalyticsDTO getAnalytics(UUID brokerId) {
        List<Transaction> allTransactions = transactionRepository.findAllByBrokerId(brokerId);
        int total = allTransactions.size();

        // --- Transaction Overview ---
        int active = 0, closed = 0, terminated = 0, buy = 0, sell = 0;
        for (Transaction t : allTransactions) {
            if (t.getStatus() == TransactionStatus.ACTIVE) active++;
            else if (t.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY) closed++;
            else if (t.getStatus() == TransactionStatus.TERMINATED_EARLY) terminated++;
            if (t.getSide() == TransactionSide.BUY_SIDE) buy++;
            else if (t.getSide() == TransactionSide.SELL_SIDE) sell++;
        }

        double successRate = (closed + terminated) > 0
                ? round((double) closed / (closed + terminated) * 100) : 0.0;

        List<Long> durations = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY
                        && t.getOpenedAt() != null && t.getClosedAt() != null)
                .map(t -> ChronoUnit.DAYS.between(t.getOpenedAt(), t.getClosedAt()))
                .toList();
        double avgDuration = durations.isEmpty() ? 0 : round(durations.stream().mapToLong(Long::longValue).average().orElse(0));
        int longestDuration = durations.isEmpty() ? 0 : (int) durations.stream().mapToLong(Long::longValue).max().orElse(0);
        int shortestDuration = durations.isEmpty() ? 0 : (int) durations.stream().mapToLong(Long::longValue).min().orElse(0);

        Map<String, Integer> openedPerMonth = allTransactions.stream()
                .filter(t -> t.getOpenedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getOpenedAt()).toString(),
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> closedPerMonth = allTransactions.stream()
                .filter(t -> t.getClosedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getClosedAt()).toString(),
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> buyerStageDistribution = allTransactions.stream()
                .filter(t -> t.getSide() == TransactionSide.BUY_SIDE && t.getBuyerStage() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getBuyerStage().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> sellerStageDistribution = allTransactions.stream()
                .filter(t -> t.getSide() == TransactionSide.SELL_SIDE && t.getSellerStage() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getSellerStage().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // --- House Visits ---
        List<Transaction> buyTransactions = allTransactions.stream()
                .filter(t -> t.getSide() == TransactionSide.BUY_SIDE).toList();

        // Single batch query → Map<transactionId, count>
        Map<UUID, Integer> hvCountsByTxId = new java.util.HashMap<>();
        if (!buyTransactions.isEmpty()) {
            List<UUID> buyTxIds = buyTransactions.stream().map(Transaction::getTransactionId).toList();
            for (Object[] row : appointmentRepository.countConfirmedHouseVisitsByTransactionIds(buyTxIds)) {
                hvCountsByTxId.put((UUID) row[0], ((Number) row[1]).intValue());
            }
        }

        int totalHouseVisits = hvCountsByTxId.values().stream().mapToInt(Integer::intValue).sum();

        List<Transaction> closedBuyTransactions = buyTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY).toList();
        double avgHouseVisits = 0.0;
        if (!closedBuyTransactions.isEmpty()) {
            int closedHV = closedBuyTransactions.stream()
                    .mapToInt(t -> hvCountsByTxId.getOrDefault(t.getTransactionId(), 0))
                    .sum();
            avgHouseVisits = round((double) closedHV / closedBuyTransactions.size());
        }

        // --- Properties (Buy-Side) ---
        List<Property> allProperties = new ArrayList<>();
        for (Transaction t : buyTransactions) {
            allProperties.addAll(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(t.getTransactionId()));
        }
        int totalProperties = allProperties.size();
        double avgPropertiesPerBuy = buy > 0 ? round((double) totalProperties / buy) : 0;

        long interestedCount = allProperties.stream().filter(p -> p.getStatus() == PropertyStatus.INTERESTED).count();
        long notInterestedCount = allProperties.stream().filter(p -> p.getStatus() == PropertyStatus.NOT_INTERESTED).count();
        double propertyInterestRate = (interestedCount + notInterestedCount) > 0
                ? round((double) interestedCount / (interestedCount + notInterestedCount) * 100) : 0;
        int propertiesNeedingInfo = (int) allProperties.stream().filter(p -> p.getStatus() == PropertyStatus.NEEDS_INFO).count();

        int propertiesWithOffers = 0;
        int propertiesWithoutOffers = 0;
        List<PropertyOffer> allBuyerOffers = new ArrayList<>();
        for (Property p : allProperties) {
            List<PropertyOffer> offers = propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(p.getPropertyId());
            allBuyerOffers.addAll(offers);
            if (offers.isEmpty()) propertiesWithoutOffers++;
            else propertiesWithOffers++;
        }

        // --- Buyer Offers ---
        int totalBuyerOffers = allBuyerOffers.size();
        long acceptedBuyerOffers = allBuyerOffers.stream().filter(o -> o.getStatus() == BuyerOfferStatus.ACCEPTED).count();
        double buyerOfferAcceptanceRate = totalBuyerOffers > 0
                ? round((double) acceptedBuyerOffers / totalBuyerOffers * 100) : 0;

        double avgOfferRounds = 0;
        if (!allProperties.isEmpty()) {
            Map<UUID, Long> roundsByProperty = allBuyerOffers.stream()
                    .collect(Collectors.groupingBy(PropertyOffer::getPropertyId, Collectors.counting()));
            if (!roundsByProperty.isEmpty()) {
                avgOfferRounds = round(roundsByProperty.values().stream().mapToLong(Long::longValue).average().orElse(0));
            }
        }

        double avgBuyerOfferAmount = allBuyerOffers.stream()
                .map(PropertyOffer::getOfferAmount)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);
        avgBuyerOfferAmount = round(avgBuyerOfferAmount);

        int expiredOrWithdrawn = (int) allBuyerOffers.stream()
                .filter(o -> o.getStatus() == BuyerOfferStatus.EXPIRED || o.getStatus() == BuyerOfferStatus.WITHDRAWN)
                .count();

        long counteredBuyerOffers = allBuyerOffers.stream()
                .filter(o -> o.getCounterpartyResponse() == CounterpartyResponse.COUNTERED).count();
        double buyerCounterOfferRate = totalBuyerOffers > 0
                ? round((double) counteredBuyerOffers / totalBuyerOffers * 100) : 0;

        // --- Received Offers (Sell-Side) ---
        List<Transaction> sellTransactions = allTransactions.stream()
                .filter(t -> t.getSide() == TransactionSide.SELL_SIDE).toList();
        List<Offer> allReceivedOffers = new ArrayList<>();
        for (Transaction t : sellTransactions) {
            allReceivedOffers.addAll(offerRepository.findByTransactionIdOrderByCreatedAtDesc(t.getTransactionId()));
        }
        int totalOffers = allReceivedOffers.size();
        long acceptedReceived = allReceivedOffers.stream().filter(o -> o.getStatus() == ReceivedOfferStatus.ACCEPTED).count();
        double receivedOfferAcceptanceRate = totalOffers > 0
                ? round((double) acceptedReceived / totalOffers * 100) : 0;

        double avgReceivedOfferAmount = allReceivedOffers.stream()
                .map(Offer::getOfferAmount)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);
        avgReceivedOfferAmount = round(avgReceivedOfferAmount);

        double highestOffer = allReceivedOffers.stream()
                .map(Offer::getOfferAmount).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        double lowestOffer = allReceivedOffers.stream()
                .map(Offer::getOfferAmount).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).min().orElse(0);

        double avgOffersPerSell = sell > 0 ? round((double) totalOffers / sell) : 0;

        int pendingOrReview = (int) allReceivedOffers.stream()
                .filter(o -> o.getStatus() == ReceivedOfferStatus.PENDING
                        || o.getStatus() == ReceivedOfferStatus.UNDER_REVIEW)
                .count();

        long counteredReceived = allReceivedOffers.stream()
                .filter(o -> o.getStatus() == ReceivedOfferStatus.COUNTERED).count();
        double receivedCounterOfferRate = totalOffers > 0
                ? round((double) counteredReceived / totalOffers * 100) : 0;

        // --- Documents ---
        List<UUID> transactionIds = allTransactions.stream()
                .map(Transaction::getTransactionId).toList();
        int totalDocuments = 0;
        int pendingDocuments = 0;
        int documentsNeedingRevision = 0;
        int completedDocuments = 0;
        for (UUID txnId : transactionIds) {
            List<Document> docs = documentRepository.findByTransactionRef_TransactionId(txnId).stream()
                    .filter(d -> d.getStatus() != DocumentStatusEnum.DRAFT)
                    .toList();
            totalDocuments += docs.size();
            for (Document d : docs) {
                if (d.getStatus() == DocumentStatusEnum.REQUESTED || d.getStatus() == DocumentStatusEnum.NEEDS_REVISION)
                    pendingDocuments++;
                if (d.getStatus() == DocumentStatusEnum.NEEDS_REVISION)
                    documentsNeedingRevision++;
                if (d.getStatus() == DocumentStatusEnum.APPROVED || d.getStatus() == DocumentStatusEnum.SUBMITTED)
                    completedDocuments++;
            }
        }
        double documentCompletionRate = totalDocuments > 0
                ? round((double) completedDocuments / totalDocuments * 100) : 0;
        double avgDocsPerTx = total > 0 ? round((double) totalDocuments / total) : 0;

        // --- Appointments ---
        List<Appointment> allAppointments = appointmentRepository
                .findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId);
        int totalAppointments = allAppointments.size();

        long confirmed = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long declined = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.DECLINED).count();
        long cancelled = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        double confirmationRate = totalAppointments > 0 ? round((double) confirmed / totalAppointments * 100) : 0;
        double declinedRate = totalAppointments > 0 ? round((double) declined / totalAppointments * 100) : 0;
        double cancelledRate = totalAppointments > 0 ? round((double) cancelled / totalAppointments * 100) : 0;

        LocalDateTime now = LocalDateTime.now();
        int upcoming = (int) allAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED && a.getFromDateTime().isAfter(now))
                .count();

        double avgApptPerTx = total > 0 ? round((double) totalAppointments / total) : 0;

        // --- Conditions ---
        List<Condition> allConditions = new ArrayList<>();
        for (UUID txnId : transactionIds) {
            allConditions.addAll(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(txnId));
        }
        int totalConditions = allConditions.size();
        long satisfied = allConditions.stream().filter(c -> c.getStatus() == ConditionStatus.SATISFIED).count();
        double conditionSatisfiedRate = totalConditions > 0
                ? round((double) satisfied / totalConditions * 100) : 0;

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysOut = today.plusDays(7);
        int approaching = (int) allConditions.stream()
                .filter(c -> c.getStatus() == ConditionStatus.PENDING
                        && !c.getDeadlineDate().isBefore(today)
                        && !c.getDeadlineDate().isAfter(sevenDaysOut))
                .count();
        int overdue = (int) allConditions.stream()
                .filter(c -> c.getStatus() == ConditionStatus.PENDING && c.getDeadlineDate().isBefore(today))
                .count();
        double avgCondPerTx = total > 0 ? round((double) totalConditions / total) : 0;

        // --- Client Engagement ---
        Map<UUID, Long> clientTxCounts = allTransactions.stream()
                .filter(t -> t.getClientId() != null && t.getStatus() == TransactionStatus.ACTIVE)
                .collect(Collectors.groupingBy(Transaction::getClientId, Collectors.counting()));
        int totalActiveClients = clientTxCounts.size();
        int multiTxClients = (int) clientTxCounts.values().stream().filter(c -> c > 1).count();

        long byBroker = allAppointments.stream()
                .filter(a -> a.getInitiatedBy() == InitiatorType.BROKER).count();
        long byClient = allAppointments.stream()
                .filter(a -> a.getInitiatedBy() == InitiatorType.CLIENT).count();

        // --- Trends ---
        Map<String, Long> monthCounts = allTransactions.stream()
                .filter(t -> t.getOpenedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getOpenedAt()).toString(),
                        Collectors.counting()
                ));
        String busiestMonth = monthCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("—");

        int idleTransactions = (int) allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE
                        && t.getLastUpdated() != null
                        && t.getLastUpdated().isBefore(now.minusDays(30)))
                .count();

        return new AnalyticsDTO(
                total, active, closed, terminated, buy, sell,
                successRate, avgDuration, longestDuration, shortestDuration,
                openedPerMonth, closedPerMonth,
                buyerStageDistribution, sellerStageDistribution,
                totalHouseVisits, avgHouseVisits,
                totalProperties, avgPropertiesPerBuy, propertyInterestRate,
                propertiesNeedingInfo, propertiesWithOffers, propertiesWithoutOffers,
                totalBuyerOffers, buyerOfferAcceptanceRate, avgOfferRounds,
                avgBuyerOfferAmount, expiredOrWithdrawn, buyerCounterOfferRate,
                totalOffers, receivedOfferAcceptanceRate, avgReceivedOfferAmount,
                highestOffer, lowestOffer, avgOffersPerSell,
                pendingOrReview, receivedCounterOfferRate,
                totalDocuments, pendingDocuments, documentCompletionRate,
                documentsNeedingRevision, avgDocsPerTx,
                totalAppointments, confirmationRate, declinedRate, cancelledRate,
                upcoming, avgApptPerTx,
                totalConditions, conditionSatisfiedRate, approaching, overdue, avgCondPerTx,
                totalActiveClients, multiTxClients,
                (int) byBroker, (int) byClient,
                busiestMonth, idleTransactions
        );
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
