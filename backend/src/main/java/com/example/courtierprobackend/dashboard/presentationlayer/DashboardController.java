package com.example.courtierprobackend.dashboard.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.dashboard.datalayer.TimelineEntrySeen;
import com.example.courtierprobackend.dashboard.datalayer.TimelineEntrySeenRepository;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.*;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.audit.loginaudit.dataaccesslayer.LoginAuditEventRepository;
import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditRepository;
import com.example.courtierprobackend.audit.loginaudit.dataaccesslayer.LoginAuditEvent;
import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditLog;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionRepository transactionRepository;
    private final UserAccountRepository userRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final PropertyOfferRepository propertyOfferRepository;
    private final OfferRepository offerRepository;
    private final PropertyRepository propertyRepository;
    private final TimelineService timelineService;
    private final TransactionService transactionService;
    private final TimelineEntrySeenRepository timelineEntrySeenRepository;
    private final LoginAuditEventRepository loginAuditRepository;
    private final AdminDeletionAuditRepository deletionAuditRepository;

    private static final int EXPIRY_DAYS_THRESHOLD = 7;

    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDashboardStats> getClientStats(
            @RequestHeader(value = "x-user-id", required = false) String headerId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID clientId = UserContextUtils.resolveUserId(request, headerId);
        
        // Active transactions
        long activeTransactions = transactionRepository.findAllByClientId(clientId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .count();

        // Documents needed (placeholder)
        int documentsNeeded = 0; 

        return ResponseEntity.ok(ClientDashboardStats.builder()
                .activeTransactions(activeTransactions)
                .documentsNeeded(documentsNeeded)
                .build());
    }

    @GetMapping("/broker")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<BrokerDashboardStats> getBrokerStats(
            @RequestHeader(value = "x-broker-id", required = false) String headerId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, headerId);

        List<Transaction> brokerTransactions = transactionRepository.findAllByBrokerId(brokerId);
        List<Transaction> activeTransactions = brokerTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .toList();

        // Active clients (unique clients in active transactions)
        long activeClients = activeTransactions.stream()
                .map(Transaction::getClientId)
                .distinct()
                .count();

        // Total commission (Mock)
        double totalCommission = activeTransactions.size() * 5000.0;

        // Pending document reviews count
        Set<UUID> activeTransactionIds = activeTransactions.stream()
                .map(Transaction::getTransactionId)
                .collect(Collectors.toSet());
        
        int pendingDocumentReviews = (int) documentRequestRepository.findAll().stream()
                .filter(doc -> doc.getTransactionRef() != null 
                        && activeTransactionIds.contains(doc.getTransactionRef().getTransactionId())
                        && doc.getStatus() == DocumentStatusEnum.SUBMITTED)
                .count();

        // Expiring offers count
        int expiringOffersCount = countExpiringOffers(activeTransactions);

        return ResponseEntity.ok(BrokerDashboardStats.builder()
                .activeTransactions(activeTransactions.size())
                .activeClients(activeClients)
                .totalCommission(totalCommission)
                .pendingDocumentReviews(pendingDocumentReviews)
                .expiringOffersCount(expiringOffersCount)
                .build());
    }

    @GetMapping("/broker/expiring-offers")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<ExpiringOfferDTO>> getExpiringOffers(
            @RequestHeader(value = "x-broker-id", required = false) String headerId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, headerId);
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.plusDays(EXPIRY_DAYS_THRESHOLD);

        List<Transaction> activeTransactions = transactionRepository.findAllByBrokerId(brokerId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .toList();

        List<ExpiringOfferDTO> expiringOffers = new ArrayList<>();

        // Get buy-side property offers (offers we made on properties)
        for (Transaction tx : activeTransactions) {
            if (tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE) {
                List<Property> properties = propertyRepository.findByTransactionIdOrderByCreatedAtDesc(tx.getTransactionId());
                for (Property property : properties) {
                    List<PropertyOffer> offers = propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(property.getPropertyId());
                    for (PropertyOffer offer : offers) {
                        if (offer.getExpiryDate() != null 
                                && !offer.getExpiryDate().isBefore(today) 
                                && !offer.getExpiryDate().isAfter(cutoffDate)) {
                            String clientName = getClientName(tx.getClientId());
                            expiringOffers.add(ExpiringOfferDTO.builder()
                                    .offerId(offer.getPropertyOfferId())
                                    .transactionId(tx.getTransactionId())
                                    .propertyAddress(property.getAddress() != null ? property.getAddress().getStreet() : "")
                                    .clientName(clientName)
                                    .offerAmount(offer.getOfferAmount())
                                    .expiryDate(offer.getExpiryDate())
                                    .daysUntilExpiry((int) ChronoUnit.DAYS.between(today, offer.getExpiryDate()))
                                    .offerType("BUY_SIDE")
                                    .status(offer.getStatus() != null ? offer.getStatus().name() : "")
                                    .build());
                        }
                    }
                }
            }
        }

        // Get sell-side received offers
        for (Transaction tx : activeTransactions) {
            if (tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.SELL_SIDE) {
                List<Offer> offers = offerRepository.findByTransactionIdOrderByCreatedAtDesc(tx.getTransactionId());
                for (Offer offer : offers) {
                    if (offer.getExpiryDate() != null 
                            && !offer.getExpiryDate().isBefore(today) 
                            && !offer.getExpiryDate().isAfter(cutoffDate)) {
                        String clientName = getClientName(tx.getClientId());
                        String address = tx.getPropertyAddress() != null ? tx.getPropertyAddress().getStreet() : "";
                        expiringOffers.add(ExpiringOfferDTO.builder()
                                .offerId(offer.getOfferId())
                                .transactionId(tx.getTransactionId())
                                .propertyAddress(address)
                                .clientName(clientName)
                                .offerAmount(offer.getOfferAmount())
                                .expiryDate(offer.getExpiryDate())
                                .daysUntilExpiry((int) ChronoUnit.DAYS.between(today, offer.getExpiryDate()))
                                .offerType("SELL_SIDE")
                                .status(offer.getStatus() != null ? offer.getStatus().name() : "")
                                .build());
                    }
                }
            }
        }

        // Sort by days until expiry (most urgent first)
        expiringOffers.sort(Comparator.comparingInt(ExpiringOfferDTO::getDaysUntilExpiry));

        return ResponseEntity.ok(expiringOffers);
    }

    @GetMapping("/broker/pending-documents")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<PendingDocumentDTO>> getPendingDocuments(
            @RequestHeader(value = "x-broker-id", required = false) String headerId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, headerId);

        Set<UUID> activeTransactionIds = transactionRepository.findAllByBrokerId(brokerId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .map(Transaction::getTransactionId)
                .collect(Collectors.toSet());

        // Build a map of transactionId -> Transaction for property addresses
        Map<UUID, Transaction> transactionMap = transactionRepository.findAllByBrokerId(brokerId).stream()
                .collect(Collectors.toMap(Transaction::getTransactionId, t -> t, (a, b) -> a));

        List<PendingDocumentDTO> pendingDocs = documentRequestRepository.findAll().stream()
                .filter(doc -> doc.getTransactionRef() != null 
                        && activeTransactionIds.contains(doc.getTransactionRef().getTransactionId())
                        && doc.getStatus() == DocumentStatusEnum.SUBMITTED)
                .map(doc -> {
                    Transaction tx = transactionMap.get(doc.getTransactionRef().getTransactionId());
                    String clientName = getClientName(doc.getTransactionRef().getClientId());
                    String propertyAddress = tx != null && tx.getPropertyAddress() != null 
                            ? tx.getPropertyAddress().getStreet() : "";
                    
                    return PendingDocumentDTO.builder()
                            .requestId(doc.getRequestId())
                            .transactionId(doc.getTransactionRef().getTransactionId())
                            .clientName(clientName)
                            .documentType(doc.getDocType() != null ? doc.getDocType().name() : "OTHER")
                            .customTitle(doc.getCustomTitle())
                            .submittedAt(doc.getLastUpdatedAt())
                            .propertyAddress(propertyAddress)
                            .build();
                })
                .sorted(Comparator.comparing(PendingDocumentDTO::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return ResponseEntity.ok(pendingDocs);
    }

    @GetMapping("/broker/recent-activity")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> getRecentActivity(
            @RequestHeader(value = "x-broker-id", required = false) String headerId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, headerId);
        
        // Get all active transactions for this broker
        List<Transaction> allBrokerTransactions = transactionRepository.findAllByBrokerId(brokerId);
        Set<UUID> activeTransactionIds = allBrokerTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .map(Transaction::getTransactionId)
                .collect(Collectors.toSet());

        // Build lookup maps for transaction context
        Map<UUID, Transaction> transactionMap = allBrokerTransactions.stream()
                .collect(Collectors.toMap(Transaction::getTransactionId, t -> t, (a, b) -> a));

        // Build map of transactionId -> most recent property (for buy-side transactions)
        Map<UUID, Property> propertyMap = new HashMap<>();
        for (UUID txId : activeTransactionIds) {
            Transaction tx = transactionMap.get(txId);
            if (tx != null && tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE) {
                List<Property> properties = propertyRepository.findByTransactionIdOrderByCreatedAtDesc(txId);
                if (!properties.isEmpty()) {
                    propertyMap.put(txId, properties.get(0)); // Most recent property
                }
            }
        }

        // Fetch paginated timeline entries
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO> entriesPage = 
                timelineService.getRecentEntriesForTransactionsPaged(activeTransactionIds, pageable);

        // Fetch seen status for all entries in this page
        Set<UUID> entryIds = entriesPage.getContent().stream()
                .map(com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO::getId)
                .collect(Collectors.toSet());
        Set<UUID> seenEntryIds = timelineEntrySeenRepository.findByBrokerIdAndTimelineEntryIdIn(brokerId, entryIds).stream()
                .map(TimelineEntrySeen::getTimelineEntryId)
                .collect(Collectors.toSet());

        // Map timeline entries to RecentActivityDTO with enriched transaction context
        List<RecentActivityDTO> activities = entriesPage.getContent().stream()
                .map(entry -> {
                    Transaction tx = transactionMap.get(entry.getTransactionId());
                    
                    // Determine property address
                    String propertyAddress;
                    if (tx == null) {
                        propertyAddress = "";
                    } else if (tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.SELL_SIDE) {
                        // Sell-side: use transaction's property address
                        propertyAddress = tx.getPropertyAddress() != null ? tx.getPropertyAddress().getStreet() : "";
                    } else {
                        // Buy-side: use most recent property or fallback message
                        Property property = propertyMap.get(tx.getTransactionId());
                        if (property != null && property.getAddress() != null) {
                            propertyAddress = property.getAddress().getStreet();
                        } else {
                            propertyAddress = "No property selected";
                        }
                    }
                    
                    // Get client name
                    String clientName = tx != null ? getClientName(tx.getClientId()) : "";
                    
                    // Get transaction side
                    String side = tx != null && tx.getSide() != null ? tx.getSide().name() : "";
                    
                    // Get current stage based on transaction side
                    String currentStage = "";
                    if (tx != null) {
                        if (tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE) {
                            currentStage = tx.getBuyerStage() != null ? tx.getBuyerStage().name() : "";
                        } else {
                            currentStage = tx.getSellerStage() != null ? tx.getSellerStage().name() : "";
                        }
                    }
                    
                    // Check if this entry has been seen by the broker
                    boolean isSeen = seenEntryIds.contains(entry.getId());
                    
                    return RecentActivityDTO.builder()
                            .activityId(entry.getId())
                            .transactionId(entry.getTransactionId())
                            .type(entry.getType() != null ? entry.getType().name() : "")
                            .title(entry.getTitle())
                            .note(entry.getNote())
                            .occurredAt(entry.getOccurredAt())
                            .actorName(entry.getActorName())
                            .propertyAddress(propertyAddress)
                            .clientName(clientName)
                            .side(side)
                            .currentStage(currentStage)
                            .docType(entry.getDocType())
                            .transactionInfo(entry.getTransactionInfo())
                            .seen(isSeen)
                            .build();
                })
                .toList();

        // Build paginated response
        Map<String, Object> response = new HashMap<>();
        response.put("content", activities);
        response.put("page", entriesPage.getNumber());
        response.put("size", entriesPage.getSize());
        response.put("totalElements", entriesPage.getTotalElements());
        response.put("totalPages", entriesPage.getTotalPages());
        response.put("first", entriesPage.isFirst());
        response.put("last", entriesPage.isLast());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/broker/recent-activity/mark-seen")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> markEntriesAsSeen(
            @RequestHeader(value = "x-broker-id", required = false) String headerId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request,
            @RequestBody MarkActivitiesSeenRequest markRequest
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, headerId);
        
        if (markRequest.getActivityIds() == null || markRequest.getActivityIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No activity IDs provided"));
        }
        
        Instant now = Instant.now();
        int markedCount = 0;
        
        for (UUID activityId : markRequest.getActivityIds()) {
            // Check if already marked as seen
            if (!timelineEntrySeenRepository.existsByBrokerIdAndTimelineEntryId(brokerId, activityId)) {
                TimelineEntrySeen seenRecord = TimelineEntrySeen.builder()
                        .brokerId(brokerId)
                        .timelineEntryId(activityId)
                        .seenAt(now)
                        .build();
                timelineEntrySeenRepository.save(seenRecord);
                markedCount++;
            }
        }
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "markedCount", markedCount
        ));
    }

    @GetMapping("/broker/pinned-transactions")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<PinnedTransactionDTO>> getPinnedTransactions(
            @RequestHeader(value = "x-broker-id", required = false) String headerId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, headerId);
        
        Set<UUID> pinnedIds = transactionService.getPinnedTransactionIds(brokerId);
        
        List<PinnedTransactionDTO> pinned = transactionRepository.findAllByBrokerId(brokerId).stream()
                .filter(t -> pinnedIds.contains(t.getTransactionId()))
                .map(tx -> {
                    String clientName = getClientName(tx.getClientId());
                    String address = tx.getPropertyAddress() != null ? tx.getPropertyAddress().getStreet() : "";
                    
                    return PinnedTransactionDTO.builder()
                            .transactionId(tx.getTransactionId())
                            .clientName(clientName)
                            .propertyAddress(address)
                            .side(tx.getSide() != null ? tx.getSide().name() : "")
                            .status(tx.getStatus() != null ? tx.getStatus().name() : "")
                            .currentStage(tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE
                                    ? (tx.getBuyerStage() != null ? tx.getBuyerStage().name() : "")
                                    : (tx.getSellerStage() != null ? tx.getSellerStage().name() : ""))
                            .build();
                })
                .limit(6)
                .toList();

        return ResponseEntity.ok(pinned);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardStats> getAdminStats() {
        long totalUsers = userRepository.count();
        long activeBrokers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.BROKER && u.isActive())
                .count();
        long clientCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CLIENT)
                .count();
        long activeTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .count();
        // New users in last 24h
        Instant sinceInstant = Instant.now().minusSeconds(86400);
        long newUsers = userRepository.findAll().stream()
                .filter(u -> {
                    if (u.getCreatedAt() == null) return false;
                    // If getCreatedAt() returns LocalDateTime
                    return u.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().isAfter(sinceInstant);
                })
                .count();
        // Failed logins in last 24h (fallback: count all login events, or filter by some other field if available)
        long failedLogins = loginAuditRepository.findByTimestampBetweenOrderByTimestampDesc(sinceInstant, Instant.now()).stream()
                // .filter(e -> e.getSuccess() != null && !e.getSuccess()) // Uncomment if 'success' field exists
                .count(); // Remove or adjust this line if you add a 'success' field
        return ResponseEntity.ok(AdminDashboardStats.builder()
                .totalUsers(totalUsers)
                .activeBrokers(activeBrokers)
                .clientCount(clientCount)
                .activeTransactions(activeTransactions)
                .newUsers(newUsers)
                .failedLogins(failedLogins)
                .systemHealth(failedLogins < 10 ? "Healthy" : "Issues Detected")
                .build());
    }

    @GetMapping("/admin/recent-actions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecentActionsResponse> getRecentActions() {
        List<LoginAuditEvent> recentLogins = loginAuditRepository.findAllByOrderByTimestampDesc().stream().limit(5).toList();
        List<AdminDeletionAuditLog> recentDeletions = deletionAuditRepository.findAllByOrderByTimestampDesc().stream().limit(5).toList();
        return ResponseEntity.ok(RecentActionsResponse.builder()
                .recentLogins(recentLogins)
                .recentDeletions(recentDeletions)
                .build());
    }

    // --- Helper Methods ---

    private int countExpiringOffers(List<Transaction> activeTransactions) {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.plusDays(EXPIRY_DAYS_THRESHOLD);
        int count = 0;

        for (Transaction tx : activeTransactions) {
            if (tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE) {
                List<Property> properties = propertyRepository.findByTransactionIdOrderByCreatedAtDesc(tx.getTransactionId());
                for (Property property : properties) {
                    List<PropertyOffer> offers = propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(property.getPropertyId());
                    count += (int) offers.stream()
                            .filter(o -> o.getExpiryDate() != null 
                                    && !o.getExpiryDate().isBefore(today) 
                                    && !o.getExpiryDate().isAfter(cutoffDate))
                            .count();
                }
            } else {
                List<Offer> offers = offerRepository.findByTransactionIdOrderByCreatedAtDesc(tx.getTransactionId());
                count += (int) offers.stream()
                        .filter(o -> o.getExpiryDate() != null 
                                && !o.getExpiryDate().isBefore(today) 
                                && !o.getExpiryDate().isAfter(cutoffDate))
                        .count();
            }
        }
        return count;
    }

    private String getClientName(UUID clientId) {
        if (clientId == null) return "";
        return userRepository.findById(clientId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("");
    }

    // --- DTOs ---

    @Data
    @Builder
    public static class ClientDashboardStats {
        private long activeTransactions;
        private int documentsNeeded;
    }

    @Data
    @Builder
    public static class BrokerDashboardStats {
        private long activeTransactions;
        private long activeClients;
        private double totalCommission;
        private int pendingDocumentReviews;
        private int expiringOffersCount;
    }

        @Data
        @Builder
        public static class AdminDashboardStats {
                private long totalUsers;
                private long activeBrokers;
                private long clientCount;
                private long activeTransactions;
                private long newUsers;
                private long failedLogins;
                private String systemHealth;
        }

        @Data
        @Builder
        public static class RecentActionsResponse {
                private List<LoginAuditEvent> recentLogins;
                private List<AdminDeletionAuditLog> recentDeletions;
        }
}
