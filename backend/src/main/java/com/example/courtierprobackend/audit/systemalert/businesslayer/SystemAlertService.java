package com.example.courtierprobackend.audit.systemalert.businesslayer;

import com.example.courtierprobackend.audit.systemalert.dataaccesslayer.SystemAlert;
import com.example.courtierprobackend.audit.systemalert.dataaccesslayer.SystemAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemAlertService {
    private final SystemAlertRepository alertRepository;

    public List<SystemAlert> getActiveAlerts() {
        return alertRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public SystemAlert createAlert(String message, String severity) {
        SystemAlert alert = SystemAlert.builder()
                .message(message)
                .severity(severity)
                .active(true)
                .createdAt(java.time.Instant.now())
                .build();
        return alertRepository.save(alert);
    }

    public void resolveAlert(Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setActive(false);
            alertRepository.save(alert);
        });
    }
}
