package com.example.courtierprobackend.dashboard.presentationlayer;

import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

        private final TransactionRepository transactionRepository;
        private final UserAccountRepository userRepository;
        private final LoginAuditEventRepository loginAuditRepository;
        private final AdminDeletionAuditRepository deletionAuditRepository;

    // -------- Helper to resolve internal user ID from UserContextFilter --------


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

        // Active transactions
        long activeTransactions = transactionRepository.findAllByBrokerId(brokerId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .count();

        // Active clients (unique clients in active transactions)
        long activeClients = transactionRepository.findAllByBrokerId(brokerId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.ACTIVE)
                .map(t -> t.getClientId())
                .distinct()
                .count();

        // Total commission (Mock)
        double totalCommission = activeTransactions * 5000.0; 

        return ResponseEntity.ok(BrokerDashboardStats.builder()
                .activeTransactions(activeTransactions)
                .activeClients(activeClients)
                .totalCommission(totalCommission)
                .build());
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
