// backend/src/main/java/com/example/courtierprobackend/Organization/businesslayer/OrganizationSettingsServiceImpl.java
package com.example.courtierprobackend.Organization.businesslayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettingsRepository;
import com.example.courtierprobackend.Organization.datamapperlayer.OrganizationSettingsMapper;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
import com.example.courtierprobackend.audit.organization_settings_audit.businesslayer.OrganizationSettingsAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrganizationSettingsServiceImpl implements OrganizationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationSettingsServiceImpl.class);

    private final OrganizationSettingsRepository repository;
    private final OrganizationSettingsMapper mapper;
    private final OrganizationSettingsAuditService organizationSettingsAuditService;
    private final HttpServletRequest httpServletRequest;

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
            UpdateOrganizationSettingsRequestModel request
    ) {
        OrganizationSettings settings = repository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(this::createDefaultSettings);

        String previousDefaultLang = settings.getDefaultLanguage();

        boolean inviteTemplateEnChanged =
                !Objects.equals(settings.getInviteSubjectEn(), request.getInviteSubjectEn()) ||
                        !Objects.equals(settings.getInviteBodyEn(), request.getInviteBodyEn());

        boolean inviteTemplateFrChanged =
                !Objects.equals(settings.getInviteSubjectFr(), request.getInviteSubjectFr()) ||
                        !Objects.equals(settings.getInviteBodyFr(), request.getInviteBodyFr());

        settings.setDefaultLanguage(request.getDefaultLanguage());
        settings.setInviteSubjectEn(request.getInviteSubjectEn());
        settings.setInviteBodyEn(request.getInviteBodyEn());
        settings.setInviteSubjectFr(request.getInviteSubjectFr());
        settings.setInviteBodyFr(request.getInviteBodyFr());
        settings.setUpdatedAt(Instant.now());

        OrganizationSettings saved = repository.save(settings);

        String adminUserId = "unknown";
        String adminEmail = "unknown";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            adminUserId = jwt.getSubject();
            Object emailClaim = jwt.getClaims().get("email");
            if (emailClaim instanceof String emailStr && !emailStr.isBlank()) {
                adminEmail = emailStr;
            }
        } else if (auth != null) {
            adminUserId = auth.getName();
        }

        String ipAddress = httpServletRequest != null
                ? httpServletRequest.getRemoteAddr()
                : "unknown";

        log.info(
                "Organization Settings updated by admin {} (email={}, ip={}). defaultLanguage={}, updatedAt={}",
                adminUserId, adminEmail, ipAddress, saved.getDefaultLanguage(), saved.getUpdatedAt()
        );

        organizationSettingsAuditService.recordSettingsUpdated(
                adminUserId,
                adminEmail,
                ipAddress,
                previousDefaultLang,
                saved.getDefaultLanguage(),
                inviteTemplateEnChanged,
                inviteTemplateFrChanged
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
