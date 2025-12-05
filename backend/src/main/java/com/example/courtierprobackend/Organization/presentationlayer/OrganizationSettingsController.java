package com.example.courtierprobackend.Organization.presentationlayer;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationSettingsController {

    private final OrganizationSettingsService organizationSettingsService;

    @GetMapping
    public ResponseEntity<OrganizationSettingsResponseModel> getSettings() {
        return ResponseEntity.ok(organizationSettingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<OrganizationSettingsResponseModel> updateSettings(
            @Valid @RequestBody UpdateOrganizationSettingsRequestModel request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminUserId = auth != null ? auth.getName() : "unknown-admin";

        return ResponseEntity.ok(organizationSettingsService.updateSettings(request, adminUserId));
    }
}
