package com.example.courtierprobackend.audit.appointment_audit.dataaccesslayer;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAudit {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(nullable = false)
    private String action; // CREATED, CONFIRMED, DECLINED, RESCHEDULED

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(columnDefinition = "TEXT")
    private String details;
}
