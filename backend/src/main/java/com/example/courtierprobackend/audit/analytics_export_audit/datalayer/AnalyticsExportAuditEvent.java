package com.example.courtierprobackend.audit.analytics_export_audit.datalayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_export_audit_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsExportAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID brokerId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String exportType; // CSV, PDF

    @Column(length = 1000)
    private String filtersApplied;
}
