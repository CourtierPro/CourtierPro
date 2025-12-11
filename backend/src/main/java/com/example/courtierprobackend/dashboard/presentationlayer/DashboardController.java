package com.example.courtierprobackend.dashboard.presentationlayer;

import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
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

        return ResponseEntity.ok(AdminDashboardStats.builder()
                .totalUsers(totalUsers)
                .activeBrokers(activeBrokers)
                .systemHealth("99.9%")
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
        private String systemHealth;
    }
}
