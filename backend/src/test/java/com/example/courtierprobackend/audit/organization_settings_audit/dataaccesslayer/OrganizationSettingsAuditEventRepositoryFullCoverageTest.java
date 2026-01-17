package com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrganizationSettingsAuditEventRepositoryFullCoverageTest {
    @Test
    void findTopNByOrderByTimestampDesc_delegatesToQuery() {
        OrganizationSettingsAuditEventRepository repo = mock(OrganizationSettingsAuditEventRepository.class, CALLS_REAL_METHODS);
        List<OrganizationSettingsAuditEvent> expected = Arrays.asList(
                OrganizationSettingsAuditEvent.builder().adminUserId("a1").build(),
                OrganizationSettingsAuditEvent.builder().adminUserId("a2").build()
        );
        when(repo.findTopNByOrderByTimestampDesc(PageRequest.of(0, 2))).thenReturn(expected);
        List<OrganizationSettingsAuditEvent> result = repo.findTopNByOrderByTimestampDesc(2);
        assertThat(result).isEqualTo(expected);
    }
}
