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

        // Update all fields from request
        settings.setDefaultLanguage(request.getDefaultLanguage());
        
        // Invite template
        settings.setInviteSubjectEn(request.getInviteSubjectEn());
        settings.setInviteBodyEn(request.getInviteBodyEn());
        settings.setInviteSubjectFr(request.getInviteSubjectFr());
        settings.setInviteBodyFr(request.getInviteBodyFr());
        
        // Document Submitted template
        settings.setDocumentSubmittedSubjectEn(request.getDocumentSubmittedSubjectEn());
        settings.setDocumentSubmittedBodyEn(request.getDocumentSubmittedBodyEn());
        settings.setDocumentSubmittedSubjectFr(request.getDocumentSubmittedSubjectFr());
        settings.setDocumentSubmittedBodyFr(request.getDocumentSubmittedBodyFr());
        
        // Document Requested template
        settings.setDocumentRequestedSubjectEn(request.getDocumentRequestedSubjectEn());
        settings.setDocumentRequestedBodyEn(request.getDocumentRequestedBodyEn());
        settings.setDocumentRequestedSubjectFr(request.getDocumentRequestedSubjectFr());
        settings.setDocumentRequestedBodyFr(request.getDocumentRequestedBodyFr());
        
        // Document Review template
        settings.setDocumentReviewSubjectEn(request.getDocumentReviewSubjectEn());
        settings.setDocumentReviewBodyEn(request.getDocumentReviewBodyEn());
        settings.setDocumentReviewSubjectFr(request.getDocumentReviewSubjectFr());
        settings.setDocumentReviewBodyFr(request.getDocumentReviewBodyFr());
        
        // Stage Update template
        settings.setStageUpdateSubjectEn(request.getStageUpdateSubjectEn());
        settings.setStageUpdateBodyEn(request.getStageUpdateBodyEn());
        settings.setStageUpdateSubjectFr(request.getStageUpdateSubjectFr());
        settings.setStageUpdateBodyFr(request.getStageUpdateBodyFr());
        
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
                // Document Submitted Template
                .documentSubmittedSubjectEn("Document Submitted")
                .documentSubmittedBodyEn("Hello {{uploaderName}},\n\nDocument Submitted\n\nYour document {{documentName}} has been submitted for the transaction {{transactionId}}.\n\nDocument Type: {{documentType}}\n\nThank you for submitting this document. Your broker will review it shortly.\n\nBest regards,\nCourtierPro Team")
                .documentSubmittedSubjectFr("Document soumis")
                .documentSubmittedBodyFr("Bonjour {{uploaderName}},\n\nDocument Soumis\n\nVotre document {{documentName}} a été soumis pour la transaction {{transactionId}}.\n\nType de document: {{documentType}}\n\nMerci d'avoir soumis ce document. Votre courtier l'examinera bientôt.\n\nCordialement,\nÉquipe CourtierPro")
                // Document Requested Template
                .documentRequestedSubjectEn("Document Requested")
                .documentRequestedBodyEn("Hello {{clientName}},\n\nDocument Request\n\n{{brokerName}} has requested the following document:\n\nDocument: {{documentName}}\nType: {{documentType}}\n\nAction Required\n\nPlease submit this document as soon as possible to keep your transaction moving forward.\n\nQuestions? Contact {{brokerName}} directly.\n\nBest regards,\nCourtierPro Team")
                .documentRequestedSubjectFr("Document demandé")
                .documentRequestedBodyFr("Bonjour {{clientName}},\n\nDemande de Document\n\n{{brokerName}} a demandé le document suivant:\n\nDocument: {{documentName}}\nType: {{documentType}}\n\nAction Requise\n\nVeuillez soumettre ce document dès que possible pour que votre transaction progresse.\n\nQuestions? Contactez directement {{brokerName}}.\n\nCordialement,\nÉquipe CourtierPro")
                // Document Review Template
                .documentReviewSubjectEn("Document Reviewed")
                .documentReviewBodyEn("Hello {{clientName}},\n\nDocument Review\n\n{{brokerName}} has reviewed your document {{documentName}} for transaction {{transactionId}}.\n\nStatus: {{status}}\n\nBroker Notes:\n{{brokerNotes}}\n\nIf you have any questions about the review, please contact {{brokerName}}.\n\nBest regards,\nCourtierPro Team")
                .documentReviewSubjectFr("Document examiné")
                .documentReviewBodyFr("Bonjour {{clientName}},\n\nExamen du Document\n\n{{brokerName}} a examiné votre document {{documentName}} pour la transaction {{transactionId}}.\n\nStatut: {{status}}\n\nNotes du courtier:\n{{brokerNotes}}\n\nSi vous avez des questions concernant l'examen, veuillez contacter {{brokerName}}.\n\nCordialement,\nÉquipe CourtierPro")
                // Stage Update Template
                .stageUpdateSubjectEn("Transaction Update")
                .stageUpdateBodyEn("Hello {{clientName}},\n\nTransaction Update\n\nYour transaction has been updated!\n\nProperty: {{transactionAddress}}\nNew Stage: {{newStage}}\n\nYour broker {{brokerName}} has moved your transaction to the next stage. If you have any questions, please don't hesitate to reach out.\n\nBest regards,\nCourtierPro Team")
                .stageUpdateSubjectFr("Mise à jour de la transaction")
                .stageUpdateBodyFr("Bonjour {{clientName}},\n\nMise à Jour de la Transaction\n\nVotre transaction a été mise à jour!\n\nPropriété: {{transactionAddress}}\nNouveaux stade: {{newStage}}\n\nVotre courtier {{brokerName}} a fait avancer votre transaction à la prochaine étape. Si vous avez des questions, n'hésitez pas à nous contacter.\n\nCordialement,\nÉquipe CourtierPro")
                .updatedAt(Instant.now())
                .build();

        return repository.save(settings);
    }
}
