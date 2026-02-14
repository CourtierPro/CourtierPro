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
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntryRepository;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

        private final TransactionRepository transactionRepository;
        private final AppointmentRepository appointmentRepository;
        private final DocumentRepository documentRepository;
        private final PropertyRepository propertyRepository;
        private final OfferRepository offerRepository;
        private final ConditionRepository conditionRepository;
        private final PropertyOfferRepository propertyOfferRepository;
        private final AnalyticsExportAuditRepository analyticsExportAuditRepository;
        private final com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;
        private final TimelineEntryRepository timelineEntryRepository;

        public AnalyticsDTO getAnalytics(UUID brokerId, AnalyticsFilterRequest filters) {
                LocalDateTime startDateTime = filters.getStartDate() != null ? filters.getStartDate().atStartOfDay()
                                : null;
                LocalDateTime endDateTime = filters.getEndDate() != null ? filters.getEndDate().atTime(LocalTime.MAX)
                                : null;

                List<UUID> clientIds = null;
                boolean clientFilterApplied = false;
                if (filters.getClientName() != null && !filters.getClientName().isBlank()) {
                        clientIds = userAccountRepository.findIdsBySearchQuery(brokerId, filters.getClientName());
                        clientFilterApplied = true;
                }

                List<Transaction> allTransactions;
                List<Appointment> allAppointments;

                if (clientFilterApplied && (clientIds == null || clientIds.isEmpty())) {
                        allTransactions = Collections.emptyList();
                        allAppointments = Collections.emptyList();
                } else if (clientIds != null) {
                        allTransactions = transactionRepository.findForAnalyticsWithClients(
                                        brokerId, startDateTime, endDateTime, filters.getTransactionType(), clientIds);
                        allAppointments = appointmentRepository.findForAnalyticsWithClients(
                                        brokerId, startDateTime, endDateTime, clientIds);
                } else {
                        allTransactions = transactionRepository.findForAnalytics(
                                        brokerId, startDateTime, endDateTime, filters.getTransactionType());
                        allAppointments = appointmentRepository.findForAnalytics(
                                        brokerId, startDateTime, endDateTime);
                }

                Set<UUID> transactionIds = allTransactions.stream()
                                .map(Transaction::getTransactionId)
                                .collect(Collectors.toSet());

                if (filters.getTransactionType() != null) {
                        allAppointments = allAppointments.stream()
                                        .filter(a -> a.getTransactionId() != null
                                                        && transactionIds.contains(a.getTransactionId()))
                                        .collect(Collectors.toList());
                }

                int total = allTransactions.size();

                // --- Transaction Overview ---
                int active = 0, closed = 0, terminated = 0, buy = 0, sell = 0;
                for (Transaction t : allTransactions) {
                        if (t.getStatus() == TransactionStatus.ACTIVE)
                                active++;
                        else if (t.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY)
                                closed++;
                        else if (t.getStatus() == TransactionStatus.TERMINATED_EARLY)
                                terminated++;
                        if (t.getSide() == TransactionSide.BUY_SIDE)
                                buy++;
                        else if (t.getSide() == TransactionSide.SELL_SIDE)
                                sell++;
                }

                double successRate = (closed + terminated) > 0 ? round((double) closed / (closed + terminated) * 100)
                                : 0.0;

                List<Long> durations = allTransactions.stream()
                                .filter(t -> t.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY
                                                && t.getOpenedAt() != null && t.getClosedAt() != null)
                                .map(t -> ChronoUnit.DAYS.between(t.getOpenedAt(), t.getClosedAt()))
                                .toList();
                double avgDuration = durations.isEmpty() ? 0
                                : round(durations.stream().mapToLong(Long::longValue).average().orElse(0));
                int longestDuration = durations.isEmpty() ? 0
                                : (int) durations.stream().mapToLong(Long::longValue).max().orElse(0);
                int shortestDuration = durations.isEmpty() ? 0
                                : (int) durations.stream().mapToLong(Long::longValue).min().orElse(0);

                Map<String, Integer> openedPerMonth = allTransactions.stream()
                                .filter(t -> t.getOpenedAt() != null)
                                .collect(Collectors.groupingBy(
                                                t -> YearMonth.from(t.getOpenedAt()).toString(),
                                                TreeMap::new,
                                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

                Map<String, Integer> closedPerMonth = allTransactions.stream()
                                .filter(t -> t.getClosedAt() != null)
                                .collect(Collectors.groupingBy(
                                                t -> YearMonth.from(t.getClosedAt()).toString(),
                                                TreeMap::new,
                                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

                Map<String, Integer> buyerStageDistribution = allTransactions.stream()
                                .filter(t -> t.getSide() == TransactionSide.BUY_SIDE && t.getBuyerStage() != null)
                                .collect(Collectors.groupingBy(
                                                t -> t.getBuyerStage().name(),
                                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

                Map<String, Integer> sellerStageDistribution = allTransactions.stream()
                                .filter(t -> t.getSide() == TransactionSide.SELL_SIDE && t.getSellerStage() != null)
                                .collect(Collectors.groupingBy(
                                                t -> t.getSellerStage().name(),
                                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

                // --- House Visits (Buy-Side) ---
                List<Transaction> buyTransactions = allTransactions.stream()
                                .filter(t -> t.getSide() == TransactionSide.BUY_SIDE).toList();
                Map<UUID, Integer> hvCountsByTxId = new HashMap<>();
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

                // --- Sell-Side Showings ---
                List<Transaction> sellTransactions = allTransactions.stream()
                                .filter(t -> t.getSide() == TransactionSide.SELL_SIDE).toList();
                Map<UUID, Integer> showingsCountsByTxId = new HashMap<>();
                Map<UUID, Integer> visitorsCountsByTxId = new HashMap<>();
                if (!sellTransactions.isEmpty()) {
                        List<UUID> sellTxIds = sellTransactions.stream().map(Transaction::getTransactionId).toList();
                        for (Object[] row : appointmentRepository.countConfirmedShowingsByTransactionIds(sellTxIds)) {
                                showingsCountsByTxId.put((UUID) row[0], ((Number) row[1]).intValue());
                        }
                        for (Object[] row : appointmentRepository.sumVisitorsByTransactionIds(sellTxIds)) {
                                visitorsCountsByTxId.put((UUID) row[0], ((Number) row[1]).intValue());
                        }
                }
                int totalSellShowings = showingsCountsByTxId.values().stream().mapToInt(Integer::intValue).sum();
                int totalSellVisitors = visitorsCountsByTxId.values().stream().mapToInt(Integer::intValue).sum();
                List<Transaction> closedSellTransactions = sellTransactions.stream()
                                .filter(t -> t.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY).toList();
                double avgSellShowings = 0.0;
                if (!closedSellTransactions.isEmpty()) {
                        int closedShowings = closedSellTransactions.stream()
                                        .mapToInt(t -> showingsCountsByTxId.getOrDefault(t.getTransactionId(), 0))
                                        .sum();
                        avgSellShowings = round((double) closedShowings / closedSellTransactions.size());
                }

                // --- Properties (Buy-Side) ---
                List<Property> allProperties = new ArrayList<>();
                for (Transaction t : buyTransactions) {
                        allProperties.addAll(propertyRepository
                                        .findByTransactionIdOrderByCreatedAtDesc(t.getTransactionId()));
                }
                int totalProperties = allProperties.size();
                double avgPropertiesPerBuyTransaction = buy > 0 ? round((double) totalProperties / buy) : 0;
                long interestedCount = allProperties.stream().filter(p -> p.getStatus() == PropertyStatus.INTERESTED)
                                .count();
                long notInterestedCount = allProperties.stream()
                                .filter(p -> p.getStatus() == PropertyStatus.NOT_INTERESTED).count();
                double propertyInterestRate = (interestedCount + notInterestedCount) > 0
                                ? round((double) interestedCount / (interestedCount + notInterestedCount) * 100)
                                : 0;
                int propertiesNeedingInfo = (int) allProperties.stream()
                                .filter(p -> p.getStatus() == PropertyStatus.NEEDS_INFO).count();
                int propertiesWithOffers = 0, propertiesWithoutOffers = 0;
                List<PropertyOffer> allBuyerOffers = new ArrayList<>();
                for (Property p : allProperties) {
                        List<PropertyOffer> offers = propertyOfferRepository
                                        .findByPropertyIdOrderByOfferRoundDesc(p.getPropertyId());
                        allBuyerOffers.addAll(offers);
                        if (offers.isEmpty())
                                propertiesWithoutOffers++;
                        else
                                propertiesWithOffers++;
                }

                // --- Buyer Offers ---
                int totalBuyerOffers = allBuyerOffers.size();
                long acceptedBuyerOffers = allBuyerOffers.stream()
                                .filter(o -> o.getStatus() == BuyerOfferStatus.ACCEPTED).count();
                double buyerOfferAcceptanceRate = totalBuyerOffers > 0
                                ? round((double) acceptedBuyerOffers / totalBuyerOffers * 100)
                                : 0;
                double avgOfferRounds = 0;
                if (!allProperties.isEmpty()) {
                        Map<UUID, Long> roundsByProperty = allBuyerOffers.stream().collect(
                                        Collectors.groupingBy(PropertyOffer::getPropertyId, Collectors.counting()));
                        if (!roundsByProperty.isEmpty()) {
                                avgOfferRounds = round(roundsByProperty.values().stream().mapToLong(Long::longValue)
                                                .average().orElse(0));
                        }
                }
                double avgBuyerOfferAmount = round(allBuyerOffers.stream().map(PropertyOffer::getOfferAmount)
                                .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0));
                int expiredOrWithdrawnOffers = (int) allBuyerOffers.stream()
                                .filter(o -> o.getStatus() == BuyerOfferStatus.EXPIRED
                                                || o.getStatus() == BuyerOfferStatus.WITHDRAWN)
                                .count();
                long counteredBuyerOffers = allBuyerOffers.stream()
                                .filter(o -> o.getCounterpartyResponse() == CounterpartyResponse.COUNTERED).count();
                double buyerCounterOfferRate = totalBuyerOffers > 0
                                ? round((double) counteredBuyerOffers / totalBuyerOffers * 100)
                                : 0;

                // --- Received Offers (Sell-Side) ---
                List<Offer> allReceivedOffers = new ArrayList<>();
                for (Transaction t : sellTransactions) {
                        allReceivedOffers.addAll(
                                        offerRepository.findByTransactionIdOrderByCreatedAtDesc(t.getTransactionId()));
                }
                int totalOffers = allReceivedOffers.size();
                long acceptedReceived = allReceivedOffers.stream()
                                .filter(o -> o.getStatus() == ReceivedOfferStatus.ACCEPTED).count();
                double receivedOfferAcceptanceRate = totalOffers > 0
                                ? round((double) acceptedReceived / totalOffers * 100)
                                : 0;
                double avgReceivedOfferAmount = round(allReceivedOffers.stream().map(Offer::getOfferAmount)
                                .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0));
                double highestOfferAmount = allReceivedOffers.stream().map(Offer::getOfferAmount)
                                .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).max().orElse(0);
                double lowestOfferAmount = allReceivedOffers.stream().map(Offer::getOfferAmount)
                                .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).min().orElse(0);
                double avgOffersPerSellTransaction = sell > 0 ? round((double) totalOffers / sell) : 0;
                int pendingOrReviewOffers = (int) allReceivedOffers.stream()
                                .filter(o -> o.getStatus() == ReceivedOfferStatus.PENDING
                                                || o.getStatus() == ReceivedOfferStatus.UNDER_REVIEW)
                                .count();
                long counteredReceived = allReceivedOffers.stream()
                                .filter(o -> o.getStatus() == ReceivedOfferStatus.COUNTERED).count();
                double receivedCounterOfferRate = totalOffers > 0
                                ? round((double) counteredReceived / totalOffers * 100)
                                : 0;

                // --- Documents ---
                int totalDocuments = 0, pendingDocuments = 0, documentsNeedingRevision = 0, completedDocuments = 0;
                for (UUID txnId : transactionIds) {
                        List<Document> docs = documentRepository.findByTransactionRef_TransactionId(txnId).stream()
                                        .filter(d -> d.getStatus() != DocumentStatusEnum.DRAFT).toList();
                        totalDocuments += docs.size();
                        for (Document d : docs) {
                                if (d.getStatus() == DocumentStatusEnum.REQUESTED
                                                || d.getStatus() == DocumentStatusEnum.NEEDS_REVISION)
                                        pendingDocuments++;
                                if (d.getStatus() == DocumentStatusEnum.NEEDS_REVISION)
                                        documentsNeedingRevision++;
                                if (d.getStatus() == DocumentStatusEnum.APPROVED
                                                || d.getStatus() == DocumentStatusEnum.SUBMITTED)
                                        completedDocuments++;
                        }
                }
                double documentCompletionRate = totalDocuments > 0
                                ? round((double) completedDocuments / totalDocuments * 100)
                                : 0;
                double avgDocumentsPerTransaction = total > 0 ? round((double) totalDocuments / total) : 0;

                // --- Appointments ---
                int totalAppointments = allAppointments.size();
                long confirmed = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                                .count();
                long declined = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.DECLINED)
                                .count();
                long cancelled = allAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                                .count();
                double appointmentConfirmationRate = totalAppointments > 0
                                ? round((double) confirmed / totalAppointments * 100)
                                : 0;
                double declinedAppointmentRate = totalAppointments > 0
                                ? round((double) declined / totalAppointments * 100)
                                : 0;
                double cancelledAppointmentRate = totalAppointments > 0
                                ? round((double) cancelled / totalAppointments * 100)
                                : 0;
                LocalDateTime now = LocalDateTime.now();
                int upcomingAppointments = (int) allAppointments.stream().filter(
                                a -> a.getStatus() == AppointmentStatus.CONFIRMED && a.getFromDateTime().isAfter(now))
                                .count();
                double avgAppointmentsPerTransaction = total > 0 ? round((double) totalAppointments / total) : 0;

                // --- Conditions ---
                List<Condition> allConditions = new ArrayList<>();
                for (UUID txnId : transactionIds) {
                        allConditions.addAll(conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(txnId));
                }
                int totalConditions = allConditions.size();
                long satisfied = allConditions.stream().filter(c -> c.getStatus() == ConditionStatus.SATISFIED).count();
                double conditionSatisfiedRate = totalConditions > 0 ? round((double) satisfied / totalConditions * 100)
                                : 0;
                LocalDate today = LocalDate.now();
                LocalDate sevenDaysOut = today.plusDays(7);
                int conditionsApproachingDeadline = (int) allConditions.stream()
                                .filter(c -> c.getStatus() == ConditionStatus.PENDING
                                                && !c.getDeadlineDate().isBefore(today)
                                                && !c.getDeadlineDate().isAfter(sevenDaysOut))
                                .count();
                int overdueConditions = (int) allConditions.stream().filter(
                                c -> c.getStatus() == ConditionStatus.PENDING && c.getDeadlineDate().isBefore(today))
                                .count();
                double avgConditionsPerTransaction = total > 0 ? round((double) totalConditions / total) : 0;

                // --- Client Engagement ---
                Map<UUID, Long> clientTxCounts = allTransactions.stream()
                                .filter(t -> t.getClientId() != null && t.getStatus() == TransactionStatus.ACTIVE)
                                .collect(Collectors.groupingBy(Transaction::getClientId, Collectors.counting()));
                int totalActiveClients = clientTxCounts.size();
                int clientsWithMultipleTransactions = (int) clientTxCounts.values().stream().filter(c -> c > 1).count();
                int appointmentsByBroker = (int) allAppointments.stream()
                                .filter(a -> a.getInitiatedBy() == InitiatorType.BROKER).count();
                int appointmentsByClient = (int) allAppointments.stream()
                                .filter(a -> a.getInitiatedBy() == InitiatorType.CLIENT).count();

                // --- Trends ---
                Map<String, Long> monthCounts = allTransactions.stream().filter(t -> t.getOpenedAt() != null)
                                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getOpenedAt()).toString(),
                                                Collectors.counting()));
                String busiestMonth = monthCounts.entrySet().stream().max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey).orElse("—");
                int idleTransactions = (int) allTransactions.stream()
                                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE && t.getLastUpdated() != null
                                                && t.getLastUpdated().isBefore(now.minusDays(30)))
                                .count();

                // --- Client Names for Pipeline ---
                Set<UUID> allClientIds = allTransactions.stream()
                                .map(Transaction::getClientId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                Map<UUID, String> clientNameMap = new HashMap<>();
                if (!allClientIds.isEmpty()) {
                        userAccountRepository.findAllById(allClientIds).forEach(
                                        u -> clientNameMap.put(u.getId(), u.getFirstName() + " " + u.getLastName()));
                }

                // --- Pipeline Visualization (CP-31) ---
                List<AnalyticsDTO.PipelineStageDTO> buyerPipeline = calculatePipeline(
                                allTransactions.stream().filter(t -> t.getSide() == TransactionSide.BUY_SIDE).toList(),
                                BuyerStage.values(), clientNameMap);
                List<AnalyticsDTO.PipelineStageDTO> sellerPipeline = calculatePipeline(
                                allTransactions.stream().filter(t -> t.getSide() == TransactionSide.SELL_SIDE).toList(),
                                SellerStage.values(), clientNameMap);

                return new AnalyticsDTO(
                                total, active, closed, terminated, buy, sell, successRate, avgDuration, longestDuration,
                                shortestDuration,
                                openedPerMonth, closedPerMonth, buyerStageDistribution, sellerStageDistribution,
                                totalHouseVisits, avgHouseVisits, totalSellShowings, avgSellShowings, totalSellVisitors,
                                totalProperties, avgPropertiesPerBuyTransaction, propertyInterestRate,
                                propertiesNeedingInfo, propertiesWithOffers, propertiesWithoutOffers,
                                totalBuyerOffers, buyerOfferAcceptanceRate, avgOfferRounds, avgBuyerOfferAmount,
                                expiredOrWithdrawnOffers, buyerCounterOfferRate,
                                totalOffers, receivedOfferAcceptanceRate, avgReceivedOfferAmount, highestOfferAmount,
                                lowestOfferAmount, avgOffersPerSellTransaction,
                                pendingOrReviewOffers, receivedCounterOfferRate, totalDocuments, pendingDocuments,
                                documentCompletionRate,
                                documentsNeedingRevision, avgDocumentsPerTransaction, totalAppointments,
                                appointmentConfirmationRate, declinedAppointmentRate, cancelledAppointmentRate,
                                upcomingAppointments, avgAppointmentsPerTransaction, totalConditions,
                                conditionSatisfiedRate, conditionsApproachingDeadline, overdueConditions,
                                avgConditionsPerTransaction,
                                totalActiveClients, clientsWithMultipleTransactions, appointmentsByBroker,
                                appointmentsByClient, busiestMonth, idleTransactions,
                                buyerPipeline, sellerPipeline);
        }

        private List<AnalyticsDTO.PipelineStageDTO> calculatePipeline(
                        List<Transaction> transactions,
                        Enum<?>[] stages,
                        Map<UUID, String> clientNames) {
                if (transactions.isEmpty()) {
                        return Arrays.stream(stages)
                                        .map(s -> new AnalyticsDTO.PipelineStageDTO(s.name(), 0, 0.0,
                                                        Collections.emptyList()))
                                        .toList();
                }
                Set<UUID> transactionIds = transactions.stream().map(Transaction::getTransactionId)
                                .collect(Collectors.toSet());
                List<TimelineEntry> history = timelineEntryRepository.findByTransactionIdInAndTypeInOrderByTimestampAsc(
                                transactionIds,
                                List.of(TimelineEntryType.STAGE_CHANGE, TimelineEntryType.STAGE_ROLLBACK));
                Map<UUID, List<TimelineEntry>> historyByTx = history.stream()
                                .collect(Collectors.groupingBy(TimelineEntry::getTransactionId));
                Map<String, List<Double>> durationMap = new HashMap<>();

                // Track current days in stage for the current active clients
                Map<String, List<AnalyticsDTO.ClientStageInfoDTO>> clientsPerStage = new HashMap<>();

                for (Transaction tx : transactions) {
                        List<TimelineEntry> entries = historyByTx.getOrDefault(tx.getTransactionId(),
                                        new ArrayList<>());
                        String currentStage = null;
                        LocalDateTime currentStageStart = tx.getOpenedAt();

                        if (!entries.isEmpty()) {
                                if (entries.get(0).getTransactionInfo() != null) {
                                        currentStage = entries.get(0).getTransactionInfo().getPreviousStage();
                                }
                        } else {
                                if (tx.getSide() == TransactionSide.BUY_SIDE && tx.getBuyerStage() != null)
                                        currentStage = tx.getBuyerStage().name();
                                else if (tx.getSide() == TransactionSide.SELL_SIDE && tx.getSellerStage() != null)
                                        currentStage = tx.getSellerStage().name();
                        }

                        if (currentStage == null && tx.getSide() == TransactionSide.BUY_SIDE)
                                currentStage = BuyerStage.BUYER_FINANCIAL_PREPARATION.name();
                        if (currentStage == null && tx.getSide() == TransactionSide.SELL_SIDE)
                                currentStage = SellerStage.SELLER_INITIAL_CONSULTATION.name();

                        for (TimelineEntry entry : entries) {

                                LocalDateTime entryTime = LocalDateTime.ofInstant(entry.getTimestamp(),
                                                java.time.ZoneId.of("America/Montreal"));
                                if (currentStage != null && currentStageStart != null) {
                                        double days = (double) ChronoUnit.MINUTES.between(currentStageStart, entryTime)
                                                        / (60.0 * 24.0);
                                        durationMap.computeIfAbsent(currentStage, k -> new ArrayList<>()).add(days);
                                }
                                if (entry.getTransactionInfo() != null)
                                        currentStage = entry.getTransactionInfo().getNewStage();
                                currentStageStart = entryTime;
                        }

                        if (tx.getStatus() == TransactionStatus.ACTIVE && currentStage != null
                                        && currentStageStart != null) {
                                double days = (double) ChronoUnit.MINUTES.between(currentStageStart,
                                                LocalDateTime.now(java.time.ZoneId.of("America/Montreal")))
                                                / (60.0 * 24.0);
                                durationMap.computeIfAbsent(currentStage, k -> new ArrayList<>()).add(days);

                                // Collect current clients for active stages
                                String name = clientNames.getOrDefault(tx.getClientId(), "Unknown Client");
                                clientsPerStage.computeIfAbsent(currentStage, k -> new ArrayList<>())
                                                .add(new AnalyticsDTO.ClientStageInfoDTO(name, round(days)));
                        }
                }

                return Arrays.stream(stages).map(stage -> {
                        String stageName = stage.name();
                        List<AnalyticsDTO.ClientStageInfoDTO> stageClients = clientsPerStage.getOrDefault(stageName,
                                        Collections.emptyList());
                        int count = stageClients.size();

                        List<Double> durations = durationMap.getOrDefault(stageName,
                                        Collections.emptyList());
                        double avg = durations.isEmpty() ? 0.0
                                        : round(durations.stream().mapToDouble(d -> d).average().orElse(0.0));
                        return new AnalyticsDTO.PipelineStageDTO(stageName, count, avg, stageClients);
                }).toList();
        }

        public byte[] exportAnalyticsCsv(UUID brokerId, AnalyticsFilterRequest filters) {
                AnalyticsDTO data = getAnalytics(brokerId, filters);
                String brokerName = userAccountRepository.findById(brokerId)
                                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown Broker");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                                CSVFormat.DEFAULT.withHeader("Category", "Metric", "Value"))) {
                        printer.printRecord("Meta", "Broker", brokerName);
                        printer.printRecord("Meta", "Generated Date",
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                        printer.printRecord("Transaction Overview", "Total Transactions", data.totalTransactions());
                        printer.printRecord("Transaction Overview", "Active Transactions", data.activeTransactions());
                        printer.printRecord("Transaction Overview", "Closed Transactions", data.closedTransactions());
                        printer.printRecord("Transaction Overview", "Success Rate (%)", data.successRate());
                        printer.printRecord("Transaction Overview", "Avg Duration (Days)",
                                        data.avgTransactionDurationDays());
                        printer.printRecord("Buy Side", "Total Buy Transactions", data.buyTransactions());
                        printer.printRecord("Buy Side", "Total House Visits", data.totalHouseVisits());
                        printer.printRecord("Buy Side", "Avg Visits/Closed Tx",
                                        data.avgHouseVisitsPerClosedTransaction());
                        printer.printRecord("Sell Side", "Total Sell Transactions", data.sellTransactions());
                        printer.printRecord("Sell Side", "Total Showings", data.totalSellShowings());
                        printer.printRecord("Sell Side", "Avg Showings/Closed Tx",
                                        data.avgSellShowingsPerClosedTransaction());
                        printer.flush();
                        logExportAudit(brokerId, "CSV", filters);
                        return out.toByteArray();
                } catch (IOException e) {
                        log.error("Error generating CSV export", e);
                        throw new RuntimeException("Failed to generate CSV export", e);
                }
        }

        // ── PDF Color Palette ──────────────────────────────────────────
        private static final java.awt.Color NAVY = new java.awt.Color(27, 42, 74); // #1B2A4A
        private static final java.awt.Color ACCENT = new java.awt.Color(59, 130, 246); // #3B82F6
        private static final java.awt.Color WHITE = java.awt.Color.WHITE;
        private static final java.awt.Color ROW_ALT = new java.awt.Color(248, 249, 250); // #F8F9FA
        private static final java.awt.Color BORDER_CLR = new java.awt.Color(222, 226, 230); // #DEE2E6
        private static final java.awt.Color TEXT_DARK = new java.awt.Color(33, 37, 41); // #212529
        private static final java.awt.Color TEXT_MUTED = new java.awt.Color(108, 117, 125); // #6C757D

        // Section accent colors
        private static final java.awt.Color SEC_TRANSACTIONS = new java.awt.Color(59, 130, 246); // Blue
        private static final java.awt.Color SEC_BUY_SIDE = new java.awt.Color(16, 185, 129); // Green
        private static final java.awt.Color SEC_SELL_SIDE = new java.awt.Color(139, 92, 246); // Purple
        private static final java.awt.Color SEC_DOCUMENTS = new java.awt.Color(245, 158, 11); // Amber
        private static final java.awt.Color SEC_APPOINTMENTS = new java.awt.Color(236, 72, 153); // Pink
        private static final java.awt.Color SEC_CONDITIONS = new java.awt.Color(20, 184, 166); // Teal
        private static final java.awt.Color SEC_CLIENTS = new java.awt.Color(249, 115, 22); // Orange
        private static final java.awt.Color SEC_TRENDS = new java.awt.Color(99, 102, 241); // Indigo

        public byte[] exportAnalyticsPdf(UUID brokerId, AnalyticsFilterRequest filters) {
                AnalyticsDTO data = getAnalytics(brokerId, filters);
                String brokerName = userAccountRepository.findById(brokerId)
                                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown Broker");
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        com.lowagie.text.Document document = new com.lowagie.text.Document(
                                        PageSize.A4, 40, 40, 40, 60);
                        PdfWriter writer = PdfWriter.getInstance(document, out);
                        writer.setPageEvent(new PdfFooterEvent());

                        document.open();

                        // ── Header Banner ──
                        addReportHeader(document, brokerName, filters);

                        // ── Section 1: Transaction Overview ──
                        addSectionTitle(document, "Transaction Overview", SEC_TRANSACTIONS);
                        PdfPTable txTable = createStyledTable();
                        addStyledHeader(txTable, "Metric", "Value", SEC_TRANSACTIONS);
                        int row = 0;
                        addStyledRow(txTable, "Total Transactions", String.valueOf(data.totalTransactions()),
                                        row++ % 2 == 1);
                        addStyledRow(txTable, "Active Transactions", String.valueOf(data.activeTransactions()),
                                        row++ % 2 == 1);
                        addStyledRow(txTable, "Closed Transactions", String.valueOf(data.closedTransactions()),
                                        row++ % 2 == 1);
                        addStyledRow(txTable, "Terminated Transactions", String.valueOf(data.terminatedTransactions()),
                                        row++ % 2 == 1);
                        addStyledRow(txTable, "Buy-Side Transactions", String.valueOf(data.buyTransactions()),
                                        row++ % 2 == 1);
                        addStyledRow(txTable, "Sell-Side Transactions", String.valueOf(data.sellTransactions()),
                                        row++ % 2 == 1);
                        addStyledRow(txTable, "Success Rate", data.successRate() + "%", row++ % 2 == 1);
                        addStyledRow(txTable, "Avg Duration", data.avgTransactionDurationDays() + " days",
                                        row++ % 2 == 1);
                        addStyledRow(txTable, "Longest Duration", data.longestDurationDays() + " days", row++ % 2 == 1);
                        addStyledRow(txTable, "Shortest Duration", data.shortestDurationDays() + " days",
                                        row++ % 2 == 1);
                        document.add(txTable);

                        // ── Section 2: Buy-Side Metrics ──
                        addSectionTitle(document, "Buy-Side Metrics", SEC_BUY_SIDE);
                        PdfPTable buyTable = createStyledTable();
                        addStyledHeader(buyTable, "Metric", "Value", SEC_BUY_SIDE);
                        row = 0;
                        addStyledRow(buyTable, "Total House Visits", String.valueOf(data.totalHouseVisits()),
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Avg Visits / Closed Tx",
                                        String.valueOf(data.avgHouseVisitsPerClosedTransaction()), row++ % 2 == 1);
                        addStyledRow(buyTable, "Total Properties", String.valueOf(data.totalProperties()),
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Avg Properties / Buy Tx",
                                        String.valueOf(data.avgPropertiesPerBuyTransaction()), row++ % 2 == 1);
                        addStyledRow(buyTable, "Property Interest Rate", data.propertyInterestRate() + "%",
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Properties Needing Info", String.valueOf(data.propertiesNeedingInfo()),
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Properties With Offers", String.valueOf(data.propertiesWithOffers()),
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Properties Without Offers",
                                        String.valueOf(data.propertiesWithoutOffers()), row++ % 2 == 1);
                        addStyledRow(buyTable, "Total Buyer Offers", String.valueOf(data.totalBuyerOffers()),
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Buyer Offer Acceptance", data.buyerOfferAcceptanceRate() + "%",
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Avg Offer Rounds", String.valueOf(data.avgOfferRounds()),
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Avg Buyer Offer Amount",
                                        "$" + String.format("%,.2f", data.avgBuyerOfferAmount()), row++ % 2 == 1);
                        addStyledRow(buyTable, "Expired / Withdrawn", String.valueOf(data.expiredOrWithdrawnOffers()),
                                        row++ % 2 == 1);
                        addStyledRow(buyTable, "Counter-Offer Rate", data.buyerCounterOfferRate() + "%",
                                        row++ % 2 == 1);
                        document.add(buyTable);

                        // ── Section 3: Sell-Side Metrics ──
                        addSectionTitle(document, "Sell-Side Metrics", SEC_SELL_SIDE);
                        PdfPTable sellTable = createStyledTable();
                        addStyledHeader(sellTable, "Metric", "Value", SEC_SELL_SIDE);
                        row = 0;
                        addStyledRow(sellTable, "Total Showings", String.valueOf(data.totalSellShowings()),
                                        row++ % 2 == 1);
                        addStyledRow(sellTable, "Avg Showings / Closed Tx",
                                        String.valueOf(data.avgSellShowingsPerClosedTransaction()), row++ % 2 == 1);
                        addStyledRow(sellTable, "Total Visitors", String.valueOf(data.totalSellVisitors()),
                                        row++ % 2 == 1);
                        addStyledRow(sellTable, "Total Received Offers", String.valueOf(data.totalOffers()),
                                        row++ % 2 == 1);
                        addStyledRow(sellTable, "Offer Acceptance Rate", data.receivedOfferAcceptanceRate() + "%",
                                        row++ % 2 == 1);
                        addStyledRow(sellTable, "Avg Received Offer",
                                        "$" + String.format("%,.2f", data.avgReceivedOfferAmount()), row++ % 2 == 1);
                        addStyledRow(sellTable, "Highest Offer",
                                        "$" + String.format("%,.2f", data.highestOfferAmount()), row++ % 2 == 1);
                        addStyledRow(sellTable, "Lowest Offer", "$" + String.format("%,.2f", data.lowestOfferAmount()),
                                        row++ % 2 == 1);
                        addStyledRow(sellTable, "Avg Offers / Sell Tx",
                                        String.valueOf(data.avgOffersPerSellTransaction()), row++ % 2 == 1);
                        addStyledRow(sellTable, "Pending / Under Review", String.valueOf(data.pendingOrReviewOffers()),
                                        row++ % 2 == 1);
                        addStyledRow(sellTable, "Counter-Offer Rate", data.receivedCounterOfferRate() + "%",
                                        row++ % 2 == 1);
                        document.add(sellTable);

                        // ── Section 4: Documents ──
                        addSectionTitle(document, "Documents", SEC_DOCUMENTS);
                        PdfPTable docTable = createStyledTable();
                        addStyledHeader(docTable, "Metric", "Value", SEC_DOCUMENTS);
                        row = 0;
                        addStyledRow(docTable, "Total Documents", String.valueOf(data.totalDocuments()),
                                        row++ % 2 == 1);
                        addStyledRow(docTable, "Pending Documents", String.valueOf(data.pendingDocuments()),
                                        row++ % 2 == 1);
                        addStyledRow(docTable, "Completion Rate", data.documentCompletionRate() + "%", row++ % 2 == 1);
                        addStyledRow(docTable, "Needing Revision", String.valueOf(data.documentsNeedingRevision()),
                                        row++ % 2 == 1);
                        addStyledRow(docTable, "Avg Documents / Tx", String.valueOf(data.avgDocumentsPerTransaction()),
                                        row++ % 2 == 1);
                        document.add(docTable);

                        // ── Section 5: Appointments ──
                        addSectionTitle(document, "Appointments", SEC_APPOINTMENTS);
                        PdfPTable apptTable = createStyledTable();
                        addStyledHeader(apptTable, "Metric", "Value", SEC_APPOINTMENTS);
                        row = 0;
                        addStyledRow(apptTable, "Total Appointments", String.valueOf(data.totalAppointments()),
                                        row++ % 2 == 1);
                        addStyledRow(apptTable, "Confirmation Rate", data.appointmentConfirmationRate() + "%",
                                        row++ % 2 == 1);
                        addStyledRow(apptTable, "Declined Rate", data.declinedAppointmentRate() + "%", row++ % 2 == 1);
                        addStyledRow(apptTable, "Cancelled Rate", data.cancelledAppointmentRate() + "%",
                                        row++ % 2 == 1);
                        addStyledRow(apptTable, "Upcoming Appointments", String.valueOf(data.upcomingAppointments()),
                                        row++ % 2 == 1);
                        addStyledRow(apptTable, "Avg Appointments / Tx",
                                        String.valueOf(data.avgAppointmentsPerTransaction()), row++ % 2 == 1);
                        document.add(apptTable);

                        // ── Section 6: Conditions ──
                        addSectionTitle(document, "Conditions", SEC_CONDITIONS);
                        PdfPTable condTable = createStyledTable();
                        addStyledHeader(condTable, "Metric", "Value", SEC_CONDITIONS);
                        row = 0;
                        addStyledRow(condTable, "Total Conditions", String.valueOf(data.totalConditions()),
                                        row++ % 2 == 1);
                        addStyledRow(condTable, "Satisfied Rate", data.conditionSatisfiedRate() + "%", row++ % 2 == 1);
                        addStyledRow(condTable, "Approaching Deadline",
                                        String.valueOf(data.conditionsApproachingDeadline()), row++ % 2 == 1);
                        addStyledRow(condTable, "Overdue", String.valueOf(data.overdueConditions()), row++ % 2 == 1);
                        addStyledRow(condTable, "Avg Conditions / Tx",
                                        String.valueOf(data.avgConditionsPerTransaction()), row++ % 2 == 1);
                        document.add(condTable);

                        // ── Section 7: Client Engagement ──
                        addSectionTitle(document, "Client Engagement", SEC_CLIENTS);
                        PdfPTable clientTable = createStyledTable();
                        addStyledHeader(clientTable, "Metric", "Value", SEC_CLIENTS);
                        row = 0;
                        addStyledRow(clientTable, "Total Active Clients", String.valueOf(data.totalActiveClients()),
                                        row++ % 2 == 1);
                        addStyledRow(clientTable, "Clients w/ Multiple Tx",
                                        String.valueOf(data.clientsWithMultipleTransactions()), row++ % 2 == 1);
                        addStyledRow(clientTable, "Appointments by Broker", String.valueOf(data.appointmentsByBroker()),
                                        row++ % 2 == 1);
                        addStyledRow(clientTable, "Appointments by Client", String.valueOf(data.appointmentsByClient()),
                                        row++ % 2 == 1);
                        document.add(clientTable);

                        // ── Section 8: Trends ──
                        addSectionTitle(document, "Trends", SEC_TRENDS);
                        PdfPTable trendTable = createStyledTable();
                        addStyledHeader(trendTable, "Metric", "Value", SEC_TRENDS);
                        row = 0;
                        addStyledRow(trendTable, "Busiest Month", data.busiestMonth(), row++ % 2 == 1);
                        addStyledRow(trendTable, "Idle Transactions", String.valueOf(data.idleTransactions()),
                                        row++ % 2 == 1);
                        document.add(trendTable);
                        document.close();
                        logExportAudit(brokerId, "PDF", filters);
                        return out.toByteArray();
                } catch (Exception e) {
                        log.error("Error generating PDF export", e);
                        throw new RuntimeException("Failed to generate PDF export", e);
                }
        }

        // ── PDF Helper Methods ───────────────────────────────────────────

        private void addReportHeader(com.lowagie.text.Document document, String brokerName,
                        AnalyticsFilterRequest filters) throws DocumentException {
                // Navy banner
                PdfPTable banner = new PdfPTable(1);
                banner.setWidthPercentage(100);
                banner.setSpacingAfter(0);

                Font bannerTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, WHITE);
                Font bannerSubFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new java.awt.Color(200, 210, 230));

                Paragraph bannerContent = new Paragraph();
                bannerContent.add(new Chunk("CourtierPro Analytics Report", bannerTitleFont));
                bannerContent.add(Chunk.NEWLINE);
                bannerContent.add(new Chunk("Professional Broker Performance Summary", bannerSubFont));

                PdfPCell bannerCell = new PdfPCell(bannerContent);
                bannerCell.setBackgroundColor(NAVY);
                bannerCell.setPadding(20);
                bannerCell.setPaddingBottom(15);
                bannerCell.setBorder(Rectangle.NO_BORDER);
                bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                banner.addCell(bannerCell);
                document.add(banner);

                // Accent line
                PdfPTable accentLine = new PdfPTable(1);
                accentLine.setWidthPercentage(100);
                accentLine.setSpacingAfter(15);
                PdfPCell lineCell = new PdfPCell(new Phrase(""));
                lineCell.setBackgroundColor(ACCENT);
                lineCell.setFixedHeight(3);
                lineCell.setBorder(Rectangle.NO_BORDER);
                accentLine.addCell(lineCell);
                document.add(accentLine);

                // Metadata row
                Font metaLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_MUTED);
                Font metaValueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK);

                PdfPTable metaTable = new PdfPTable(
                                filters.getStartDate() != null && filters.getEndDate() != null ? 3 : 2);
                metaTable.setWidthPercentage(100);
                metaTable.setSpacingAfter(20);

                addMetaCell(metaTable, "BROKER", brokerName, metaLabelFont, metaValueFont);
                addMetaCell(metaTable, "GENERATED",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")),
                                metaLabelFont, metaValueFont);
                if (filters.getStartDate() != null && filters.getEndDate() != null) {
                        addMetaCell(metaTable, "PERIOD",
                                        filters.getStartDate() + "  to  " + filters.getEndDate(),
                                        metaLabelFont, metaValueFont);
                }
                document.add(metaTable);
        }

        private void addMetaCell(PdfPTable table, String label, String value,
                        Font labelFont, Font valueFont) {
                Paragraph p = new Paragraph();
                p.add(new Chunk(label, labelFont));
                p.add(Chunk.NEWLINE);
                p.add(new Chunk(value, valueFont));
                PdfPCell cell = new PdfPCell(p);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(5);
                table.addCell(cell);
        }

        private void addSectionTitle(com.lowagie.text.Document document, String title,
                        java.awt.Color accentColor) throws DocumentException {
                // Spacer
                document.add(new Paragraph(" "));

                // Colored left-bar + title
                PdfPTable header = new PdfPTable(new float[] { 4f, 96f });
                header.setWidthPercentage(100);
                header.setSpacingAfter(6);

                PdfPCell colorBar = new PdfPCell(new Phrase(""));
                colorBar.setBackgroundColor(accentColor);
                colorBar.setBorder(Rectangle.NO_BORDER);
                colorBar.setFixedHeight(24);
                header.addCell(colorBar);

                Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, TEXT_DARK);
                PdfPCell titleCell = new PdfPCell(new Phrase(title, sectionFont));
                titleCell.setBorder(Rectangle.NO_BORDER);
                titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                titleCell.setPaddingLeft(8);
                header.addCell(titleCell);

                document.add(header);
        }

        private PdfPTable createStyledTable() throws DocumentException {
                PdfPTable table = new PdfPTable(new float[] { 55f, 45f });
                table.setWidthPercentage(100);
                table.setSpacingAfter(5);
                return table;
        }

        private void addStyledHeader(PdfPTable table, String col1, String col2,
                        java.awt.Color bgColor) {
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, WHITE);

                PdfPCell c1 = new PdfPCell(new Phrase(col1, headerFont));
                c1.setBackgroundColor(bgColor);
                c1.setPadding(8);
                c1.setBorderWidth(0);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(col2, headerFont));
                c2.setBackgroundColor(bgColor);
                c2.setPadding(8);
                c2.setBorderWidth(0);
                table.addCell(c2);
        }

        private void addStyledRow(PdfPTable table, String label, String value, boolean alternate) {
                Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
                Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
                java.awt.Color bg = alternate ? ROW_ALT : WHITE;

                PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
                c1.setBackgroundColor(bg);
                c1.setPadding(7);
                c1.setBorderWidthTop(0);
                c1.setBorderWidthLeft(0);
                c1.setBorderWidthRight(0);
                c1.setBorderWidthBottom(0.5f);
                c1.setBorderColorBottom(BORDER_CLR);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
                c2.setBackgroundColor(bg);
                c2.setPadding(7);
                c2.setBorderWidthTop(0);
                c2.setBorderWidthLeft(0);
                c2.setBorderWidthRight(0);
                c2.setBorderWidthBottom(0.5f);
                c2.setBorderColorBottom(BORDER_CLR);
                c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c2);
        }

        // ── Page Footer Event ────────────────────────────────────────────

        private static class PdfFooterEvent extends com.lowagie.text.pdf.PdfPageEventHelper {
                @Override
                public void onEndPage(PdfWriter writer, com.lowagie.text.Document document) {
                        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
                        Phrase left = new Phrase("Confidential — CourtierPro", footerFont);
                        Phrase right = new Phrase("Page " + writer.getPageNumber(), footerFont);

                        float x = document.left();
                        float xEnd = document.right();
                        float y = document.bottom() - 20;

                        com.lowagie.text.pdf.ColumnText.showTextAligned(
                                        writer.getDirectContent(),
                                        Element.ALIGN_LEFT, left, x, y, 0);
                        com.lowagie.text.pdf.ColumnText.showTextAligned(
                                        writer.getDirectContent(),
                                        Element.ALIGN_RIGHT, right, xEnd, y, 0);
                }
        }

        private void logExportAudit(UUID brokerId, String type, AnalyticsFilterRequest filters) {
                String filterString = String.format("Start:%s, End:%s, Type:%s, Client:%s",
                                filters.getStartDate(), filters.getEndDate(), filters.getTransactionType(),
                                filters.getClientName());
                AnalyticsExportAuditEvent event = AnalyticsExportAuditEvent.builder()
                                .brokerId(brokerId)
                                .timestamp(LocalDateTime.now())
                                .exportType(type)
                                .filtersApplied(filterString)
                                .build();
                analyticsExportAuditRepository.save(event);
        }

        private double round(double value) {
                return Math.round(value * 10.0) / 10.0;
        }
}