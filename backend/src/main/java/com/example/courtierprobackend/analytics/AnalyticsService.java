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
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
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
                                .map(Map.Entry::getKey).orElse("");
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
                        userAccountRepository.findAllById(allClientIds).forEach(u -> 
                                clientNameMap.put(u.getId(), u.getFirstName() + " " + u.getLastName()));
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
                        return Arrays.stream(stages).map(s -> new AnalyticsDTO.PipelineStageDTO(s.name(), 0, 0.0, Collections.emptyList()))
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
                                                java.time.ZoneId.systemDefault());
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
                                                LocalDateTime.now()) / (60.0 * 24.0);
                                durationMap.computeIfAbsent(currentStage, k -> new ArrayList<>()).add(days);
                                
                                // Collect current clients for active stages
                                String name = clientNames.getOrDefault(tx.getClientId(), "Unknown Client");
                                clientsPerStage.computeIfAbsent(currentStage, k -> new ArrayList<>())
                                        .add(new AnalyticsDTO.ClientStageInfoDTO(name, round(days)));
                        }
                }

                return Arrays.stream(stages).map(stage -> {
                        String stageName = stage.name();
                        List<AnalyticsDTO.ClientStageInfoDTO> stageClients = clientsPerStage.getOrDefault(stageName, Collections.emptyList());
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

        public byte[] exportAnalyticsPdf(UUID brokerId, AnalyticsFilterRequest filters) {
                AnalyticsDTO data = getAnalytics(brokerId, filters);
                String brokerName = userAccountRepository.findById(brokerId)
                                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown Broker");
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        com.lowagie.text.Document document = new com.lowagie.text.Document(PageSize.A4);
                        PdfWriter.getInstance(document, out);
                        document.open();
                        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
                        Paragraph title = new Paragraph("CourtierPro Analytics Report", titleFont);
                        title.setAlignment(Element.ALIGN_CENTER);
                        document.add(title);
                        document.add(Chunk.NEWLINE);
                        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
                        document.add(new Paragraph("Broker: " + brokerName, metaFont));
                        document.add(new Paragraph(
                                        "Generated Date: " + LocalDateTime.now()
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                                        metaFont));
                        if (filters.getStartDate() != null && filters.getEndDate() != null) {
                                document.add(new Paragraph(
                                                "Period: " + filters.getStartDate() + " to " + filters.getEndDate(),
                                                metaFont));
                        }
                        document.add(Chunk.NEWLINE);
                        PdfPTable table = new PdfPTable(2);
                        table.setWidthPercentage(100);
                        table.setSpacingBefore(10f);
                        table.setSpacingAfter(10f);
                        addTableHeader(table, "Metric");
                        addTableHeader(table, "Value");
                        addTableRow(table, "Total Transactions", String.valueOf(data.totalTransactions()));
                        addTableRow(table, "Active Transactions", String.valueOf(data.activeTransactions()));
                        addTableRow(table, "Closed Transactions", String.valueOf(data.closedTransactions()));
                        addTableRow(table, "Success Rate", data.successRate() + "%");
                        addTableRow(table, "Buy Transactions", String.valueOf(data.buyTransactions()));
                        addTableRow(table, "Sell Transactions", String.valueOf(data.sellTransactions()));
                        addTableRow(table, "Total Showings", String.valueOf(data.totalSellShowings()));
                        document.add(table);
                        document.close();
                        logExportAudit(brokerId, "PDF", filters);
                        return out.toByteArray();
                } catch (Exception e) {
                        log.error("Error generating PDF export", e);
                        throw new RuntimeException("Failed to generate PDF export", e);
                }
        }

        private void addTableHeader(PdfPTable table, String headerTitle) {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                header.setBorderWidth(2);
                header.setPhrase(new Phrase(headerTitle));
                table.addCell(header);
        }

        private void addTableRow(PdfPTable table, String key, String value) {
                table.addCell(key);
                table.addCell(value);
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