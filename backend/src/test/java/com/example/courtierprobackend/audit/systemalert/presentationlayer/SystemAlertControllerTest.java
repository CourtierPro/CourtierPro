package com.example.courtierprobackend.audit.systemalert.presentationlayer;

import com.example.courtierprobackend.audit.systemalert.businesslayer.SystemAlertService;
import com.example.courtierprobackend.audit.systemalert.dataaccesslayer.SystemAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SystemAlertControllerTest {
    @Mock
    private SystemAlertService alertService;

    @InjectMocks
    private SystemAlertController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getActiveAlerts_returnsList() {
        List<SystemAlert> alerts = List.of(new SystemAlert(), new SystemAlert());
        when(alertService.getActiveAlerts()).thenReturn(alerts);

        ResponseEntity<List<SystemAlert>> response = controller.getActiveAlerts();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(alerts);
        verify(alertService).getActiveAlerts();
    }

    @Test
    void createAlert_returnsCreatedAlert() {
        SystemAlertController.CreateAlertRequest req = new SystemAlertController.CreateAlertRequest();
        req.setMessage("msg");
        req.setSeverity("sev");
        SystemAlert alert = new SystemAlert();
        when(alertService.createAlert("msg", "sev")).thenReturn(alert);

        ResponseEntity<SystemAlert> response = controller.createAlert(req);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(alert);
        verify(alertService).createAlert("msg", "sev");
    }
}
