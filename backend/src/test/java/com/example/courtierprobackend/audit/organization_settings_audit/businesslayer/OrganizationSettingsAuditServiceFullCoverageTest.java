package com.example.courtierprobackend.audit.organization_settings_audit.businesslayer;

import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEvent;
import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrganizationSettingsAuditServiceFullCoverageTest {

    @Mock
    private OrganizationSettingsAuditEventRepository repository;

    private OrganizationSettingsAuditService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OrganizationSettingsAuditService(repository);
    }

    @Test
    void recordSettingsUpdated_savesEvent() {
        service.recordSettingsUpdated("admin-1", "admin@test.com", "127.0.0.1", "en", "fr", true, false);
        verify(repository).save(any(OrganizationSettingsAuditEvent.class));
    }

    @Test
    void getAllAuditEvents_returnsAll() {
        List<OrganizationSettingsAuditEvent> events = Arrays.asList(
                OrganizationSettingsAuditEvent.builder().adminUserId("a1").build(),
                OrganizationSettingsAuditEvent.builder().adminUserId("a2").build()
        );
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(events);
        List<OrganizationSettingsAuditEvent> result = service.getAllAuditEvents();
        assertThat(result).hasSize(2);
    }

    @Test
    void getRecentAuditEvents_returnsLimited() {
        List<OrganizationSettingsAuditEvent> events = Collections.singletonList(
                OrganizationSettingsAuditEvent.builder().adminUserId("a1").build()
        );
        when(repository.findTopNByOrderByTimestampDesc(1)).thenReturn(events);
        List<OrganizationSettingsAuditEvent> result = service.getRecentAuditEvents(1);
        assertThat(result).hasSize(1);
    }
}
