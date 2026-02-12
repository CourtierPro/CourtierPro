package com.example.courtierprobackend.analytics;

import com.example.courtierprobackend.security.UserContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<AnalyticsDTO> getAnalytics(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide transactionType,
            @RequestParam(required = false) String clientName) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .clientName(clientName)
                .build();
        return ResponseEntity.ok(analyticsService.getAnalytics(brokerId, filters));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<byte[]> exportAnalyticsCsv(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide transactionType,
            @RequestParam(required = false) String clientName) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .clientName(clientName)
                .build();

        byte[] csvData = analyticsService.exportAnalyticsCsv(brokerId, filters);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=analytics_report.csv")
                .header("Content-Type", "text/csv")
                .body(csvData);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<byte[]> exportAnalyticsPdf(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide transactionType,
            @RequestParam(required = false) String clientName) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .clientName(clientName)
                .build();

        byte[] pdfData = analyticsService.exportAnalyticsPdf(brokerId, filters);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=analytics_report.pdf")
                .header("Content-Type", "application/pdf")
                .body(pdfData);
    }
}
