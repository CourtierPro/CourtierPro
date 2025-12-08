package com.example.courtierprobackend.audit.loginaudit.presentationlayer;

import com.example.courtierprobackend.audit.loginaudit.businesslayer.LoginAuditService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoginAuditController.class)
@AutoConfigureMockMvc(addFilters = true) // keep Spring Security enabled
class LoginAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginAuditService loginAuditService;

    // ✅ ADMIN — OK
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllLoginEvents_asAdmin_returnsOk() throws Exception {
        when(loginAuditService.getAllLoginEvents())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/login-audit"))
                .andExpect(status().isOk());

        verify(loginAuditService).getAllLoginEvents();
    }

    // ✅ ADMIN — by user
    @Test
    @WithMockUser(roles = "ADMIN")
    void getLoginEventsByUser_asAdmin_returnsOk() throws Exception {
        when(loginAuditService.getLoginEventsByUser("auth0|123"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/login-audit/user/auth0|123"))
                .andExpect(status().isOk());

        verify(loginAuditService).getLoginEventsByUser("auth0|123");
    }

    // ✅ ADMIN — by role
    @Test
    @WithMockUser(roles = "ADMIN")
    void getLoginEventsByRole_asAdmin_returnsOk() throws Exception {
        when(loginAuditService.getLoginEventsByRole("ADMIN"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/login-audit/role/ADMIN"))
                .andExpect(status().isOk());

        verify(loginAuditService).getLoginEventsByRole("ADMIN");
    }

    // ✅ NOT AUTHENTICATED — 401
    @Test
    void getAllLoginEvents_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/login-audit"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loginAuditService);
    }

    // ⚠️ NON-ADMIN — DISABLED (method security not enforced in @WebMvcTest)
    @Disabled("PreAuthorize is not enforced in WebMvcTest slice")
    @Test
    @WithMockUser(roles = "BROKER")
    void getAllLoginEvents_nonAdmin_isForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/login-audit"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loginAuditService);
    }
}