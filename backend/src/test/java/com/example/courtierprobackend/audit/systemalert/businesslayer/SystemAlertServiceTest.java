package com.example.courtierprobackend.audit.systemalert.businesslayer;

import com.example.courtierprobackend.audit.systemalert.dataaccesslayer.SystemAlert;
import com.example.courtierprobackend.audit.systemalert.dataaccesslayer.SystemAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SystemAlertServiceTest {
    @Mock
    private SystemAlertRepository alertRepository;

    @InjectMocks
    private SystemAlertService alertService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getActiveAlerts_ReturnsActiveAlertsOrderedByCreatedAtDesc() {
        SystemAlert alert1 = SystemAlert.builder().id(1L).message("A1").severity("INFO").active(true).createdAt(Instant.now()).build();
        SystemAlert alert2 = SystemAlert.builder().id(2L).message("A2").severity("CRITICAL").active(true).createdAt(Instant.now().plusSeconds(10)).build();
        when(alertRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(Arrays.asList(alert2, alert1));

        List<SystemAlert> result = alertService.getActiveAlerts();
        assertThat(result).containsExactly(alert2, alert1);
        verify(alertRepository).findByActiveTrueOrderByCreatedAtDesc();
    }

    @Test
    void createAlert_SavesAndReturnsAlert() {
        String message = "Disk full";
        String severity = "CRITICAL";
        ArgumentCaptor<SystemAlert> captor = ArgumentCaptor.forClass(SystemAlert.class);
        SystemAlert saved = SystemAlert.builder().id(1L).message(message).severity(severity).active(true).createdAt(Instant.now()).build();
        when(alertRepository.save(any(SystemAlert.class))).thenReturn(saved);

        SystemAlert result = alertService.createAlert(message, severity);
        verify(alertRepository).save(captor.capture());
        SystemAlert toSave = captor.getValue();
        assertThat(toSave.getMessage()).isEqualTo(message);
        assertThat(toSave.getSeverity()).isEqualTo(severity);
        assertThat(toSave.isActive()).isTrue();
        assertThat(toSave.getCreatedAt()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void resolveAlert_SetsActiveFalseAndSaves() {
        SystemAlert alert = SystemAlert.builder().id(1L).message("A1").severity("INFO").active(true).createdAt(Instant.now()).build();
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(SystemAlert.class))).thenReturn(alert);

        alertService.resolveAlert(1L);
        assertThat(alert.isActive()).isFalse();
        verify(alertRepository).save(alert);
    }

    @Test
    void resolveAlert_DoesNothingIfAlertNotFound() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());
        alertService.resolveAlert(99L);
        verify(alertRepository, never()).save(any());
    }
}
