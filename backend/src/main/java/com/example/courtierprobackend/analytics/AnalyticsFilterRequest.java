package com.example.courtierprobackend.analytics;

import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class AnalyticsFilterRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private TransactionSide transactionType;
    private String clientName;
}
