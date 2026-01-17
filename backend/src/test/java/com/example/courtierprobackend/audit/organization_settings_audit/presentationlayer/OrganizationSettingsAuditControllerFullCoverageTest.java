
package com.example.courtierprobackend.audit.organization_settings_audit.presentationlayer;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;

import com.example.courtierprobackend.audit.organization_settings_audit.businesslayer.OrganizationSettingsAuditService;
import com.example.courtierprobackend.audit.organization_settings_audit.dataaccesslayer.OrganizationSettingsAuditEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrganizationSettingsAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrganizationSettingsAuditControllerFullCoverageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationSettingsAuditService auditService;

    @MockBean
    private UserContextFilter userContextFilter;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogs_withNoLimit_returnsAll() throws Exception {
        when(auditService.getAllAuditEvents()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/admin/settings/audit"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
        verify(auditService).getAllAuditEvents();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogs_withLimit_returnsLimited() throws Exception {
        List<OrganizationSettingsAuditEvent> events = Arrays.asList(
                OrganizationSettingsAuditEvent.builder().adminUserId("a1").build(),
                OrganizationSettingsAuditEvent.builder().adminUserId("a2").build()
        );
        when(auditService.getRecentAuditEvents(2)).thenReturn(events);
        mockMvc.perform(get("/api/admin/settings/audit?limit=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));
        verify(auditService).getRecentAuditEvents(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogs_withInvalidLimit_returnsAll() throws Exception {
        when(auditService.getAllAuditEvents()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/admin/settings/audit?limit=0"))
                .andExpect(status().isOk());
        verify(auditService).getAllAuditEvents();
    }
}
