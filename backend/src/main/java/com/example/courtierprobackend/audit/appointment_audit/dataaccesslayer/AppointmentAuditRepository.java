package com.example.courtierprobackend.audit.appointment_audit.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AppointmentAuditRepository extends JpaRepository<AppointmentAudit, UUID> {
    List<AppointmentAudit> findByAppointmentIdOrderByPerformedAtDesc(UUID appointmentId);
}
