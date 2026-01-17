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
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        // Update all fields from request
        mapper.updateEntityFromRequest(request, settings);

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

    private String loadTemplate(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("email-templates/defaults/" + filename);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load email template: {}", filename, e);
            return "";
        }
    }

    private OrganizationSettings createDefaultSettings() {
        OrganizationSettings settings = OrganizationSettings.builder()
                .defaultLanguage("fr")
                .inviteSubjectEn("Welcome to CourtierPro")
                .inviteBodyEn(loadTemplate("invite_en.txt"))
                .inviteSubjectFr("Bienvenue sur CourtierPro")
                .inviteBodyFr(loadTemplate("invite_fr.txt"))
                // Document Submitted Template
                .documentSubmittedSubjectEn("Document Submitted")
                .documentSubmittedBodyEn(loadTemplate("document_submitted_en.txt"))
                .documentSubmittedSubjectFr("Document soumis")
                .documentSubmittedBodyFr(loadTemplate("document_submitted_fr.txt"))
                // Document Requested Template
                .documentRequestedSubjectEn("Document Requested")
                .documentRequestedBodyEn(loadTemplate("document_requested_en.txt"))
                .documentRequestedSubjectFr("Document demandé")
                .documentRequestedBodyFr(loadTemplate("document_requested_fr.txt"))
                // Document Review Template
                .documentReviewSubjectEn("Document Reviewed")
                .documentReviewBodyEn(loadTemplate("document_review_en.txt"))
                .documentReviewSubjectFr("Document examiné")
                .documentReviewBodyFr(loadTemplate("document_review_fr.txt"))
                // Stage Update Template
                .stageUpdateSubjectEn("Transaction Update")
                .stageUpdateBodyEn(loadTemplate("stage_update_en.txt"))
                .stageUpdateSubjectFr("Mise à jour de la transaction")
                .stageUpdateBodyFr(loadTemplate("stage_update_fr.txt"))
                // Property Offer Made Template
                .propertyOfferMadeSubjectEn("Offer Submitted")
                .propertyOfferMadeBodyEn(loadTemplate("property_offer_made_en.txt"))
                .propertyOfferMadeSubjectFr("Offre soumise")
                .propertyOfferMadeBodyFr(loadTemplate("property_offer_made_fr.txt"))
                // Property Offer Status Template
                .propertyOfferStatusSubjectEn("Offer Status Update")
                .propertyOfferStatusBodyEn(loadTemplate("property_offer_status_en.txt"))
                .propertyOfferStatusSubjectFr("Mise à jour de l'offre")
                .propertyOfferStatusBodyFr(loadTemplate("property_offer_status_fr.txt"))
                // Offer Received Template
                .offerReceivedSubjectEn("New Offer Received")
                .offerReceivedBodyEn(loadTemplate("offer_received_en.txt"))
                .offerReceivedSubjectFr("Nouvelle offre reçue")
                .offerReceivedBodyFr(loadTemplate("offer_received_fr.txt"))
                // Offer Status Template
                .offerStatusSubjectEn("Offer Status Update")
                .offerStatusBodyEn(loadTemplate("offer_status_en.txt"))
                .offerStatusSubjectFr("Mise à jour de l'offre")
                .offerStatusBodyFr(loadTemplate("offer_status_fr.txt"))
                .updatedAt(Instant.now())
                .build();

        return repository.save(settings);
    }
}
