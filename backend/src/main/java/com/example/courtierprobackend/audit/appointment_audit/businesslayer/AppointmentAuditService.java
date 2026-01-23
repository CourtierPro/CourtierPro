package com.example.courtierprobackend.audit.appointment_audit.businesslayer;

import java.util.UUID;

public interface AppointmentAuditService {
    void logAction(UUID appointmentId, String action, UUID performedBy, String details);
}
