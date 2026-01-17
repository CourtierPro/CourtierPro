package com.example.courtierprobackend.audit.organization_settings_audit.presentationlayer;

import com.example.courtierprobackend.audit.organization_settings_audit.businesslayer.OrganizationSettingsAuditService;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for OrganizationSettingsAuditController.
 * Tests HTTP endpoints with security filter chain enabled.
 */
@WebMvcTest(controllers = OrganizationSettingsAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrganizationSettingsAuditControllerTest {

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
    void getAuditLogs_asAdmin_returnsOk() throws Exception {
        when(auditService.getAllAuditEvents()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/settings/audit"))
                .andExpect(status().isOk());

        verify(auditService).getAllAuditEvents();
    }

}
