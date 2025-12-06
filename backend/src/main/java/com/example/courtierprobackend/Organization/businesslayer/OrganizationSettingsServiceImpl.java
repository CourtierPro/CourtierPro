package com.example.courtierprobackend.Organization.businesslayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettingsRepository;
import com.example.courtierprobackend.Organization.datamapperlayer.OrganizationSettingsMapper;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
import com.example.courtierprobackend.audit.organization_settings_audit.businesslayer.OrganizationSettingsAuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrganizationSettingsServiceImpl implements OrganizationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationSettingsServiceImpl.class);

    private final OrganizationSettingsRepository repository;
    private final OrganizationSettingsMapper mapper;

    private final OrganizationSettingsAuditService organizationSettingsAuditService;

    @Override
    @Transactional
    public OrganizationSettingsResponseModel getSettings() {
        OrganizationSettings settings = repository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(this::createDefaultSettings);
        return mapper.toResponseModel(settings);
    }

    @Override
    @Transactional
    public OrganizationSettingsResponseModel updateSettings(
            UpdateOrganizationSettingsRequestModel request,
            String adminUserId
    ) {

        OrganizationSettings settings = repository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(this::createDefaultSettings);

        // 🔹 Garder l’ancienne langue pour l’audit
        String previousDefaultLang = settings.getDefaultLanguage();

        // Apply changes
        settings.setDefaultLanguage(request.getDefaultLanguage());
        settings.setInviteSubjectEn(request.getInviteSubjectEn());
        settings.setInviteBodyEn(request.getInviteBodyEn());
        settings.setInviteSubjectFr(request.getInviteSubjectFr());
        settings.setInviteBodyFr(request.getInviteBodyFr());
        settings.setUpdatedAt(Instant.now());

        OrganizationSettings saved = repository.save(settings);

        log.info("Organization Settings Updated by admin {}. defaultLanguage={}, updatedAt={}",
                adminUserId, saved.getDefaultLanguage(), saved.getUpdatedAt());


        organizationSettingsAuditService.recordSettingsUpdated(
                adminUserId,
                "unknown",
                previousDefaultLang,
                saved.getDefaultLanguage()
        );

        return mapper.toResponseModel(saved);
    }

    private OrganizationSettings createDefaultSettings() {
        OrganizationSettings settings = OrganizationSettings.builder()
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome to CourtierPro")
                .inviteBodyEn("Hi {{name}}, your CourtierPro account has been created.")
                .inviteSubjectFr("Bienvenue sur CourtierPro")
                .inviteBodyFr("Bonjour {{name}}, votre compte CourtierPro a été créé.")
                .updatedAt(Instant.now())
                .build();

        return repository.save(settings);
    }
}
