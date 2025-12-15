package com.example.courtierprobackend.audit.resourcedeletion;

import com.example.courtierprobackend.audit.resourcedeletion.businesslayer.AdminResourceService;
import com.example.courtierprobackend.audit.resourcedeletion.datalayer.AdminDeletionAuditLog;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.AdminResourceController;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.DeletionPreviewResponse;
import com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto.ResourceListResponse;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminResourceService adminResourceService;

    @MockitoBean
    private UserContextFilter userContextFilter;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    // ========== List Resources Tests ==========

    @Test
    void listResources_ForTransactions_ReturnsResourceList() throws Exception {
        UUID txId = UUID.randomUUID();
        ResourceListResponse response = ResourceListResponse.builder()
                .resourceType("TRANSACTION")
                .totalCount(1)
                .deletedCount(0)
                .items(List.of(
                        ResourceListResponse.ResourceItem.builder()
                                .id(txId)
                                .summary("123 Main St, Montreal (ACTIVE)")
                                .isDeleted(false)
                                .build()
                ))
                .build();

        when(adminResourceService.listResources(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(false)))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/admin/resources/TRANSACTION")
                        .with(jwt().authorities(ROLE_ADMIN))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("TRANSACTION"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(txId.toString()));
    }

    @Test
    void listResources_WithIncludeDeleted_PassesParameter() throws Exception {
        ResourceListResponse response = ResourceListResponse.builder()
                .resourceType("TRANSACTION")
                .totalCount(0)
                .build();

        when(adminResourceService.listResources(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(true)))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/admin/resources/TRANSACTION")
                        .param("includeDeleted", "true")
                        .with(jwt().authorities(ROLE_ADMIN))
        )
                .andExpect(status().isOk());

        verify(adminResourceService).listResources(
                AdminDeletionAuditLog.ResourceType.TRANSACTION, true);
    }

    @Test
    void listResources_WithInvalidType_Returns400() throws Exception {
        mockMvc.perform(
                get("/api/admin/resources/INVALID_TYPE")
                        .with(jwt().authorities(ROLE_ADMIN))
        )
                .andExpect(status().isBadRequest());
    }

    // ========== Preview Deletion Tests ==========

    @Test
    void previewDeletion_ReturnsLinkedResources() throws Exception {
        UUID txId = UUID.randomUUID();
        DeletionPreviewResponse preview = DeletionPreviewResponse.builder()
                .resourceId(txId)
                .resourceType("TRANSACTION")
                .resourceSummary("123 Main St (ACTIVE)")
                .linkedResources(List.of(
                        DeletionPreviewResponse.LinkedResource.builder()
                                .type("TIMELINE_ENTRY")
                                .summary("Stage Update")
                                .build()
                ))
                .s3FilesToDelete(List.of(
                        DeletionPreviewResponse.S3FileToDelete.builder()
                                .fileName("document.pdf")
                                .mimeType("application/pdf")
                                .sizeBytes(1024L)
                                .build()
                ))
                .build();

        when(adminResourceService.previewDeletion(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId)))
                .thenReturn(preview);

        mockMvc.perform(
                get("/api/admin/resources/TRANSACTION/{id}/preview", txId)
                        .with(jwt().authorities(ROLE_ADMIN))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(txId.toString()))
                .andExpect(jsonPath("$.linkedResources").isArray())
                .andExpect(jsonPath("$.s3FilesToDelete[0].fileName").value("document.pdf"));
    }

    @Test
    void previewDeletion_NotFound_Returns404() throws Exception {
        UUID txId = UUID.randomUUID();
        when(adminResourceService.previewDeletion(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId)))
                .thenThrow(new NotFoundException("Transaction not found"));

        mockMvc.perform(
                get("/api/admin/resources/TRANSACTION/{id}/preview", txId)
                        .with(jwt().authorities(ROLE_ADMIN))
        )
                .andExpect(status().isNotFound());
    }

    // ========== Delete Resource Tests ==========

    @Test
    void deleteResource_WithConfirmation_Returns204() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        doNothing().when(adminResourceService).deleteResource(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId), any());

        mockMvc.perform(
                delete("/api/admin/resources/TRANSACTION/{id}", txId)
                        .param("confirmed", "true")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .requestAttr("internalUserId", adminId)
        )
                .andExpect(status().isNoContent());

        verify(adminResourceService).deleteResource(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId), any());
    }

    @Test
    void deleteResource_WithoutConfirmation_Returns400() throws Exception {
        UUID txId = UUID.randomUUID();

        mockMvc.perform(
                delete("/api/admin/resources/TRANSACTION/{id}", txId)
                        .param("confirmed", "false")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .requestAttr("internalUserId", UUID.randomUUID())
        )
                .andExpect(status().isBadRequest());

        verify(adminResourceService, never()).deleteResource(any(), any(), any());
    }

    @Test
    void deleteResource_AlreadyDeleted_Returns400() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        doThrow(new BadRequestException("Transaction is already deleted"))
                .when(adminResourceService).deleteResource(
                        eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId), any());

        mockMvc.perform(
                delete("/api/admin/resources/TRANSACTION/{id}", txId)
                        .param("confirmed", "true")
                        .with(jwt().authorities(ROLE_ADMIN))
                        .requestAttr("internalUserId", adminId)
        )
                .andExpect(status().isBadRequest());
    }

    // ========== Restore Resource Tests ==========

    @Test
    void restoreResource_Success_Returns200() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        doNothing().when(adminResourceService).restoreResource(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId), any());

        mockMvc.perform(
                post("/api/admin/resources/TRANSACTION/{id}/restore", txId)
                        .with(jwt().authorities(ROLE_ADMIN))
                        .requestAttr("internalUserId", adminId)
        )
                .andExpect(status().isOk());

        verify(adminResourceService).restoreResource(
                eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId), any());
    }

    @Test
    void restoreResource_NotDeleted_Returns400() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        doThrow(new BadRequestException("Transaction is not deleted"))
                .when(adminResourceService).restoreResource(
                        eq(AdminDeletionAuditLog.ResourceType.TRANSACTION), eq(txId), any());

        mockMvc.perform(
                post("/api/admin/resources/TRANSACTION/{id}/restore", txId)
                        .with(jwt().authorities(ROLE_ADMIN))
                        .requestAttr("internalUserId", adminId)
        )
                .andExpect(status().isBadRequest());
    }

    // ========== Audit History Tests ==========

    @Test
    void getAuditHistory_ReturnsAuditLogs() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AdminDeletionAuditLog log = AdminDeletionAuditLog.builder()
                .id(1L)
                .action(AdminDeletionAuditLog.ActionType.DELETE)
                .timestamp(LocalDateTime.now())
                .adminId(adminId)
                .resourceType(AdminDeletionAuditLog.ResourceType.TRANSACTION)
                .resourceId(resourceId)
                .resourceSnapshot("{}")
                .cascadedDeletions("[]")
                .build();

        UserAccount adminUser = new UserAccount(
                "auth0|admin", "admin@test.com", "Admin", "User", UserRole.ADMIN, "en");
        adminUser.setId(adminId);

        when(adminResourceService.getAuditHistory()).thenReturn(List.of(log));
        when(userAccountRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        mockMvc.perform(
                get("/api/admin/resources/audit-history")
                        .with(jwt().authorities(ROLE_ADMIN))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceType").value("TRANSACTION"))
                .andExpect(jsonPath("$[0].adminEmail").value("admin@test.com"))
                .andExpect(jsonPath("$[0].resourceId").value(resourceId.toString()));
    }

    @Test
    void getAuditHistory_WithUnknownAdmin_ReturnsUnknownEmail() throws Exception {
        UUID unknownAdminId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AdminDeletionAuditLog log = AdminDeletionAuditLog.builder()
                .id(1L)
                .action(AdminDeletionAuditLog.ActionType.DELETE)
                .timestamp(LocalDateTime.now())
                .adminId(unknownAdminId)
                .resourceType(AdminDeletionAuditLog.ResourceType.DOCUMENT_REQUEST)
                .resourceId(resourceId)
                .build();

        when(adminResourceService.getAuditHistory()).thenReturn(List.of(log));
        when(userAccountRepository.findById(unknownAdminId)).thenReturn(Optional.empty());

        mockMvc.perform(
                get("/api/admin/resources/audit-history")
                        .with(jwt().authorities(ROLE_ADMIN))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adminEmail").value("Unknown"));
    }
}
