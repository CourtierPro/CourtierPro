package com.example.courtierprobackend.audit.appointment_audit.businesslayer;

import com.example.courtierprobackend.audit.appointment_audit.dataaccesslayer.AppointmentAudit;
import com.example.courtierprobackend.audit.appointment_audit.dataaccesslayer.AppointmentAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentAuditServiceImpl implements AppointmentAuditService {

    private final AppointmentAuditRepository appointmentAuditRepository;

    @Override
    @Transactional
    public void logAction(UUID appointmentId, String action, UUID performedBy, String details) {
        AppointmentAudit audit = AppointmentAudit.builder()
                .appointmentId(appointmentId)
                .action(action)
                .performedBy(performedBy)
                .performedAt(Instant.now())
                .details(details)
                .build();

        appointmentAuditRepository.save(audit);
    }
}
