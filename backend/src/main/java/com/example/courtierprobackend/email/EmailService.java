package com.example.courtierprobackend.email;

import com.example.courtierprobackend.shared.utils.StageTranslationUtil;
import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class EmailService {
    @Value("${app.frontendBaseUrl:https://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.email.provider:gmail}")
    private String emailProvider;

    @Value("${app.email.from-address:noreply@courtierpro.com}")
    private String fromAddress;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final String gmailUsername;
    private final String gmailPassword;
    private final String gmailHost;
    private final String gmailPort;
    final OrganizationSettingsService organizationSettingsService;
    final com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;
    private final software.amazon.awssdk.services.ses.SesClient sesClient;

    public EmailService(
            @Value("${gmail.username}") String gmailUsername,
            @Value("${gmail.password}") String gmailPassword,
            @Value("${gmail.host:smtp.gmail.com}") String gmailHost,
            @Value("${gmail.port:587}") String gmailPort,
            OrganizationSettingsService organizationSettingsService,
            com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository,
            software.amazon.awssdk.services.ses.SesClient sesClient) {
        this.gmailUsername = gmailUsername;
        this.gmailPassword = gmailPassword;
        this.gmailHost = gmailHost;
        this.gmailPort = gmailPort;
        this.organizationSettingsService = organizationSettingsService;
        this.userAccountRepository = userAccountRepository;
        this.sesClient = sesClient;
    }

    public boolean sendPasswordSetupEmail(String toEmail, String passwordSetupUrl) {
        return sendPasswordSetupEmail(toEmail, passwordSetupUrl, null);
    }

    /**
     * Send the password setup email using org templates and the requested language.
     *
     * @param toEmail          recipient email
     * @param passwordSetupUrl Auth0 password setup / reset link
     * @param languageCode     "en" / "fr" or null -> will fall back to org default
     */
    public boolean sendPasswordSetupEmail(String toEmail,
                                          String passwordSetupUrl,
                                          String languageCode) {
        try {
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String effectiveLang = languageCode;
            if (effectiveLang == null || effectiveLang.isBlank()) {
                effectiveLang = settings.getDefaultLanguage();
            }
            if (effectiveLang == null || effectiveLang.isBlank()) {
                effectiveLang = "en";
            }

            boolean isFrench = effectiveLang.equalsIgnoreCase("fr");

            String subject = isFrench ? settings.getInviteSubjectFr() : settings.getInviteSubjectEn();
            String bodyText = isFrench ? settings.getInviteBodyFr() : settings.getInviteBodyEn();

            if (subject == null || subject.isBlank()) {
                subject = isFrench ? "Invitation CourtierPro" : "CourtierPro Invitation";
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{name}}, votre compte CourtierPro a été créé.\n\nDéfinir votre mot de passe\n\nCliquez sur le lien ci-dessous pour finaliser la création de votre compte."
                        : "Hi {{name}}, your CourtierPro account has been created.\n\nSet Your Password\n\nClick the link below to complete your account setup.";
            }

            String bodyTextWithVars = bodyText
                    .replace("{{name}}", toEmail)
                    .replace("{{passwordLink}}", passwordSetupUrl);
            String emailBody = convertPlainTextToHtml(bodyTextWithVars);

            return sendEmail(toEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send password setup email to {}", toEmail, e);
            logger.warn("Manual password setup URL for {}: {}", toEmail, passwordSetupUrl);
            return false;
        }
    }

    public void sendDocumentSubmittedNotification(DocumentRequest request, String brokerEmail, String uploaderName,
                                                 String documentName, String docType, String brokerLanguage) {
        // Check broker's email notification preference
        var brokerOpt = userAccountRepository.findByEmail(brokerEmail);
        if (!brokerOpt.isPresent() || !brokerOpt.get().isEmailNotificationsEnabled()) {
            // Do not send email if broker has disabled email notifications
            return;
        }
        try {
            boolean isFrench = brokerLanguage != null && brokerLanguage.equalsIgnoreCase("fr");

            // Defensive null handling for docType and documentName
            String safeDocType = docType == null ? "" : docType;
            String safeDocumentName = documentName == null ? "" : documentName;

            // Translate document type based on broker's language
            String translatedDocType = translateDocumentType(safeDocType, isFrench);
            String displayName = safeDocumentName.equals(safeDocType) ? translatedDocType : safeDocumentName;

            // Get organization settings for templates and subject/body
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getDocumentSubmittedSubjectFr() : settings.getDocumentSubmittedSubjectEn();
            String bodyText = isFrench ? settings.getDocumentSubmittedBodyFr() : settings.getDocumentSubmittedBodyEn();

            // Fallback to defaults if not configured
            if (subject == null || subject.isBlank()) {
                subject = isFrench ? ("Document soumis : " + displayName) : ("Document Submitted: " + displayName);
            } else {
                // If subject from settings does not include displayName, append it
                if (!subject.contains(displayName)) {
                    subject += " : " + displayName;
                }
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{uploaderName}}, votre document {{documentName}} a été soumis pour la transaction {{transactionId}}."
                        : "Hello {{uploaderName}}, your document {{documentName}} has been submitted for the transaction {{transactionId}}.";
            }

            // Use organization settings for body if available, otherwise fallback to template
            String emailBody;
            if (bodyText != null) {
                emailBody = convertPlainTextToHtml(bodyText)
                        .replace("{{uploaderName}}", escapeHtml(uploaderName))
                        .replace("{{documentName}}", escapeHtml(displayName))
                        .replace("{{documentType}}", escapeHtml(translatedDocType))
                        .replace("{{transactionId}}", escapeHtml(request.getTransactionRef().getTransactionId().toString()));
            } else {
                String templatePath = isFrench
                        ? "email-templates/document_submitted_fr.html"
                        : "email-templates/document_submitted_en.html";
                String htmlTemplate = loadTemplateFromClasspath(templatePath);
                emailBody = htmlTemplate
                        .replace("{{subject}}", escapeHtml(subject))
                        .replace("{{uploaderName}}", escapeHtml(uploaderName))
                        .replace("{{documentName}}", escapeHtml(displayName))
                        .replace("{{transactionId}}", escapeHtml(request.getTransactionRef().getTransactionId().toString()));
            }

            sendEmail(brokerEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send document submitted notification to {}", brokerEmail, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendDocumentRequestedNotification(String clientEmail, String clientName, String brokerName,
                                                 String documentName, String docType, String brokerNotes, String clientLanguage) {
        // Check client's email notification preference
        var clientOpt = userAccountRepository.findByEmail(clientEmail);
        if (!clientOpt.isPresent() || !clientOpt.get().isEmailNotificationsEnabled()) {
            // Do not send email if client has disabled email notifications
            return;
        }
        try {
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            // Defensive null handling for docType and documentName
            String safeDocType = docType == null ? "" : docType;
            String safeDocumentName = documentName == null ? "" : documentName;

            // Translate document type based on client's language
            String translatedDocType = translateDocumentType(safeDocType, isFrench);
            String displayName = safeDocumentName.equals(safeDocType) ? translatedDocType : safeDocumentName;

            // Get organization settings for templates and subject/body
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getDocumentRequestedSubjectFr() : settings.getDocumentRequestedSubjectEn();
            String bodyText = isFrench ? settings.getDocumentRequestedBodyFr() : settings.getDocumentRequestedBodyEn();

            // Fallback to defaults if not configured
            if (subject == null || subject.isBlank()) {
                subject = isFrench ? ("Document demandé : " + displayName) : ("Document Requested: " + displayName);
            } else {
                // If subject from settings does not include displayName, append it
                if (!subject.contains(displayName)) {
                    subject += " : " + displayName;
                }
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{clientName}}, {{brokerName}} a demandé le document {{documentName}}. Veuillez le soumettre dès que possible."
                        : "Hello {{clientName}}, {{brokerName}} has requested the document {{documentName}}. Please submit it as soon as possible.";
            }

            // Prepare variable values for conditional blocks
            java.util.Map<String, String> variableValues = new java.util.HashMap<>();
            variableValues.put("brokerNotes", brokerNotes);

            // Process conditional blocks BEFORE converting to HTML
            bodyText = handleConditionalBlocks(bodyText, variableValues);

            // Use organization settings for body if available, otherwise fallback to template
            String emailBody;
            if (bodyText != null) {
                emailBody = convertPlainTextToHtml(bodyText)
                        .replace("{{clientName}}", escapeHtml(clientName))
                        .replace("{{brokerName}}", escapeHtml(brokerName))
                        .replace("{{documentName}}", escapeHtml(displayName))
                        .replace("{{documentType}}", escapeHtml(translatedDocType))
                        .replace("{{brokerNotes}}", brokerNotes != null ? escapeHtml(brokerNotes) : "");
            } else {
                String templatePath = isFrench
                        ? "email-templates/document_requested_fr.html"
                        : "email-templates/document_requested_en.html";
                String htmlTemplate = loadTemplateFromClasspath(templatePath);
                emailBody = htmlTemplate
                        .replace("{{subject}}", escapeHtml(subject))
                        .replace("{{clientName}}", escapeHtml(clientName))
                        .replace("{{brokerName}}", escapeHtml(brokerName))
                        .replace("{{documentName}}", escapeHtml(displayName));
            }

            sendEmail(clientEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send document requested notification to {}", clientEmail, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendDocumentEditedNotification(String clientEmail, String clientName, String brokerName,
                                              String documentName, String docType, String clientLanguage) {
        try {
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            // Translate document type based on client's language
            String translatedDocType = translateDocumentType(docType, isFrench);
            String displayName = documentName.equals(docType) ? translatedDocType : documentName;

            String subject = isFrench
                    ? ("Document modifié : " + displayName)
                    : ("Document Edited: " + displayName);

            String templatePath = isFrench
                    ? "email-templates/document_edited_fr.html"
                    : "email-templates/document_edited_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{documentName}}", escapeHtml(displayName));

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load document edited email template", e);
        } catch (MessagingException e) {
            logger.error("Failed to send document edited notification to {}", clientEmail, e);
        }
    }

    public void sendDocumentStatusUpdatedNotification(
            DocumentRequest request,
            String clientEmail,
            String clientName,
            String brokerName,
            String documentName,
            String docType,
            String clientLanguage) {
        try {
            // Use client's preferred language, fallback to "en"
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            // Translate document type based on language
            String translatedDocType = translateDocumentType(docType, isFrench);

            String displayName = documentName.equals(docType) ? translatedDocType : documentName;

            // Get settings for templates
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getDocumentReviewSubjectFr() : settings.getDocumentReviewSubjectEn();
            String bodyText = isFrench ? settings.getDocumentReviewBodyFr() : settings.getDocumentReviewBodyEn();

            // Fallback to defaults if not configured
            if (subject == null || subject.isBlank()) {
                subject = isFrench ? "Document examiné" : "Document Reviewed";
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour, {{brokerName}} a examiné votre document {{documentName}} pour la transaction {{transactionId}}."
                        : "Hello, {{brokerName}} has reviewed your document {{documentName}} for transaction {{transactionId}}.";
            }

            // Prepare variable values for conditional blocks
            java.util.Map<String, String> variableValues = new java.util.HashMap<>();
            variableValues.put("brokerNotes", request.getBrokerNotes());

            // Flags for decision-specific sections
            boolean approved = request.getStatus() != null &&
                    "APPROVED".equals(request.getStatus().toString());
            boolean needsRevision = request.getStatus() != null &&
                    "NEEDS_REVISION".equals(request.getStatus().toString());
            variableValues.put("isApproved", approved ? "true" : "");
            variableValues.put("isNeedsRevision", needsRevision ? "true" : "");

            // Process conditional blocks BEFORE converting to HTML
            bodyText = handleConditionalBlocks(bodyText, variableValues);

            String translatedStatus = translateDocumentStatus(request.getStatus(), isFrench);

            String resolvedClientName = (clientName != null && !clientName.trim().isEmpty())
                    ? clientName
                    : (clientEmail != null && !clientEmail.isBlank()
                    ? clientEmail
                    : (isFrench ? "client" : "there"));

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{clientName}}", escapeHtml(resolvedClientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{documentName}}", escapeHtml(displayName))
                    .replace("{{documentType}}", escapeHtml(translatedDocType))
                    .replace("{{transactionId}}", escapeHtml(request.getTransactionRef().getTransactionId().toString()))
                    .replace("{{status}}", escapeHtml(translatedStatus))
                    .replace("{{brokerNotes}}", request.getBrokerNotes() != null ? escapeHtml(request.getBrokerNotes()) : "");

            sendEmail(clientEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send document status notification to {}", clientEmail, e);
        }
    }

    // ==================== PROPERTY OFFER NOTIFICATIONS (BUY-SIDE) ====================

    /**
     * Send email notification when a property offer is made on behalf of a buyer client.
     */
    public void sendPropertyOfferMadeNotification(
            String clientEmail,
            String clientName,
            String brokerName,
            String propertyAddress,
            String offerAmount,
            int offerRound,
            String clientLanguage) {
        try {
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getPropertyOfferMadeSubjectFr() : settings.getPropertyOfferMadeSubjectEn();
            String bodyText = isFrench ? settings.getPropertyOfferMadeBodyFr() : settings.getPropertyOfferMadeBodyEn();
            if (subject == null) {
                subject = "";
            }
            if (bodyText == null) {
                bodyText = "";
            }
            String resolvedClientName = (clientName != null && !clientName.trim().isEmpty())
                    ? clientName
                    : (clientEmail != null && !clientEmail.isBlank()
                    ? clientEmail
                    : (isFrench ? "client" : "there"));

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{clientName}}", escapeHtml(resolvedClientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{propertyAddress}}", escapeHtml(propertyAddress))
                    .replace("{{offerAmount}}", escapeHtml(offerAmount))
                    .replace("{{offerRound}}", String.valueOf(offerRound));

            sendEmail(clientEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send property offer made notification to {}", clientEmail, e);
        }
    }

    /**
     * Send email notification when a property offer status changes (e.g., COUNTERED, ACCEPTED, DECLINED).
     */
    public void sendPropertyOfferStatusChangedNotification(
            String clientEmail,
            String clientName,
            String brokerName,
            String propertyAddress,
            String previousStatus,
            String newStatus,
            String counterpartyResponse,
            String clientLanguage) {
        try {
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            String translatedPreviousStatus = translateOfferStatus(previousStatus, isFrench);
            String translatedNewStatus = translateOfferStatus(newStatus, isFrench);

            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getPropertyOfferStatusSubjectFr() : settings.getPropertyOfferStatusSubjectEn();
            String bodyText = isFrench ? settings.getPropertyOfferStatusBodyFr() : settings.getPropertyOfferStatusBodyEn();
            String resolvedClientName = (clientName != null && !clientName.trim().isEmpty())
                    ? clientName
                    : (clientEmail != null && !clientEmail.isBlank()
                    ? clientEmail
                    : (isFrench ? "client" : "there"));

            // Prepare variable values for conditional blocks
            java.util.Map<String, String> variableValues = new java.util.HashMap<>();
            variableValues.put("counterpartyResponse", counterpartyResponse);

            // Process conditional blocks BEFORE converting to HTML
            bodyText = handleConditionalBlocks(bodyText, variableValues);

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{clientName}}", escapeHtml(resolvedClientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{propertyAddress}}", escapeHtml(propertyAddress))
                    .replace("{{previousStatus}}", escapeHtml(translatedPreviousStatus))
                    .replace("{{newStatus}}", escapeHtml(translatedNewStatus))
                    .replace("{{counterpartyResponse}}", escapeHtml(counterpartyResponse != null ? counterpartyResponse : ""));

            sendEmail(clientEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send property offer status notification to {}", clientEmail, e);
        }
    }

    // ==================== OFFER NOTIFICATIONS (SELL-SIDE) ====================

    /**
     * Send email notification when an offer is received on a seller client's property.
     */
    public void sendOfferReceivedNotification(
            String clientEmail,
            String clientName,
            String brokerName,
            String buyerName,
            String offerAmount,
            String clientLanguage) {
        try {
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getOfferReceivedSubjectFr() : settings.getOfferReceivedSubjectEn();
            String bodyText = isFrench ? settings.getOfferReceivedBodyFr() : settings.getOfferReceivedBodyEn();
            String resolvedClientName = (clientName != null && !clientName.trim().isEmpty())
                    ? clientName
                    : (clientEmail != null && !clientEmail.isBlank()
                    ? clientEmail
                    : (isFrench ? "client" : "there"));

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{clientName}}", escapeHtml(resolvedClientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{buyerName}}", escapeHtml(buyerName))
                    .replace("{{offerAmount}}", escapeHtml(offerAmount));

            sendEmail(clientEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send offer received notification to {}", clientEmail, e);
        }
    }

    /**
     * Send email notification when an offer status changes on a seller client's property.
     */
    public void sendOfferStatusChangedNotification(
            String clientEmail,
            String clientName,
            String brokerName,
            String buyerName,
            String previousStatus,
            String newStatus,
            String clientLanguage) {
        try {
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            String translatedPreviousStatus = translateOfferStatus(previousStatus, isFrench);
            String translatedNewStatus = translateOfferStatus(newStatus, isFrench);

            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getOfferStatusSubjectFr() : settings.getOfferStatusSubjectEn();
            String bodyText = isFrench ? settings.getOfferStatusBodyFr() : settings.getOfferStatusBodyEn();
            String resolvedClientName = (clientName != null && !clientName.trim().isEmpty())
                    ? clientName
                    : (clientEmail != null && !clientEmail.isBlank()
                    ? clientEmail
                    : (isFrench ? "client" : "there"));

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{clientName}}", escapeHtml(resolvedClientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{buyerName}}", escapeHtml(buyerName))
                    .replace("{{previousStatus}}", escapeHtml(translatedPreviousStatus))
                    .replace("{{newStatus}}", escapeHtml(translatedNewStatus));

            sendEmail(clientEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send offer status notification to {}", clientEmail, e);
        }
    }

    /**
     * Translate offer status enum values to human-readable strings.
     */
    String translateOfferStatus(String status, boolean isFrench) {
        if (status == null) return "";
        return switch (status) {
            case "OFFER_MADE" -> isFrench ? "Offre soumise" : "Offer Made";
            case "PENDING" -> isFrench ? "En attente" : "Pending";
            case "COUNTERED" -> isFrench ? "Contre-offre" : "Countered";
            case "ACCEPTED" -> isFrench ? "Acceptée" : "Accepted";
            case "DECLINED" -> isFrench ? "Refusée" : "Declined";
            case "WITHDRAWN" -> isFrench ? "Retirée" : "Withdrawn";
            case "EXPIRED" -> isFrench ? "Expirée" : "Expired";
            default -> status;
        };
    }

    String translateDocumentType(String docType, boolean isFrench) {
        if (docType == null) {
            return isFrench ? "Autre" : "Other";
        }
        return switch (docType) {
            case "MORTGAGE_PRE_APPROVAL" -> isFrench ? "Pré-approbation hypothécaire" : "Mortgage Pre-Approval";
            case "MORTGAGE_APPROVAL" -> isFrench ? "Approbation hypothécaire" : "Mortgage Approval";
            case "PROOF_OF_FUNDS" -> isFrench ? "Preuve de fonds" : "Proof of Funds";
            case "ID_VERIFICATION" -> isFrench ? "Vérification d'identité" : "ID Verification";
            case "EMPLOYMENT_LETTER" -> isFrench ? "Lettre d'emploi" : "Employment Letter";
            case "PAY_STUBS" -> isFrench ? "Talons de paie" : "Pay Stubs";
            case "CREDIT_REPORT" -> isFrench ? "Rapport de crédit" : "Credit Report";
            case "CERTIFICATE_OF_LOCATION" -> isFrench ? "Certificat de localisation" : "Certificate of Location";
            case "PROMISE_TO_PURCHASE" -> isFrench ? "Promesse d'achat" : "Promise to Purchase";
            case "INSPECTION_REPORT" -> isFrench ? "Rapport d'inspection" : "Inspection Report";
            case "INSURANCE_LETTER" -> isFrench ? "Lettre d'assurance" : "Insurance Letter";
            case "BANK_STATEMENT" -> isFrench ? "Relevé bancaire" : "Bank Statement";
            case "OTHER" -> isFrench ? "Autre" : "Other";
            default -> docType;
        };
    }


    // ... (keep existing methods until sendEmail) ...

    boolean sendEmail(String to, String subject, String body)
            throws MessagingException, UnsupportedEncodingException {
        // Close any open paragraphs and add footer
        String bodyWithFooter = body.replaceAll("</p>$", "") + "</p>" + getEmailFooter();

        if ("ses".equalsIgnoreCase(emailProvider)) {
            return sendEmailSes(to, subject, bodyWithFooter);
        } else {
            return sendEmailSmtp(to, subject, bodyWithFooter);
        }
    }

    private boolean sendEmailSes(String to, String subject, String body) {
        try {
            software.amazon.awssdk.services.ses.model.SendEmailRequest request = software.amazon.awssdk.services.ses.model.SendEmailRequest.builder()
                    .source(fromAddress)
                    .destination(d -> d.toAddresses(to))
                    .message(m -> m
                            .subject(s -> s.data(subject).charset("UTF-8"))
                            .body(b -> b.html(h -> h.data(body).charset("UTF-8"))))
                    .build();

            sesClient.sendEmail(request);
            logger.info("Email sent successfully via AWS SES to {}", to);
            return true;
        } catch (software.amazon.awssdk.services.ses.model.SesException e) {
            logger.error("Failed to send email via AWS SES to {}: {}", to, e.awsErrorDetails().errorMessage());
            // Fallback to SMTP if SES fails? Or just return false?
            // For now, let's log and fail, or we could fallback.
            // Given ports are blocked, fallback won't help.
            return false;
        }
    }

    private boolean sendEmailSmtp(String to, String subject, String body)
            throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        // Use configured host/port or fallback to Gmail defaults
        props.put("mail.smtp.host", gmailHost != null ? gmailHost : "smtp.gmail.com");
        props.put("mail.smtp.port", gmailPort != null ? gmailPort : "587");
        props.put("mail.smtp.ssl.trust", gmailHost != null ? gmailHost : "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(gmailUsername, gmailPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(gmailUsername, "CourtierPro"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(body, "text/html; charset=utf-8");

        Transport.send(message);
        logger.info("Email sent successfully via SMTP to {}", to);
        return true;
    }

    String loadTemplateFromClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);

        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public void sendStageUpdateEmail(String toEmail, String clientName, String brokerName, String transactionAddress,
                                     String newStage, String language) {
        try {
            boolean isFrench = language != null && language.equalsIgnoreCase("fr");

            // Get settings for templates
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getStageUpdateSubjectFr() : settings.getStageUpdateSubjectEn();
            String bodyText = isFrench ? settings.getStageUpdateBodyFr() : settings.getStageUpdateBodyEn();

            // Fallback to defaults if not configured
            if (subject == null || subject.isBlank()) {
                subject = isFrench ? "Mise à jour de transaction" : "Transaction Update";
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{clientName}}, votre transaction à {{transactionAddress}} a été mise à jour à {{newStage}}."
                        : "Hello {{clientName}}, your transaction at {{transactionAddress}} has been updated to {{newStage}}.";
            }

            String formattedStage = StageTranslationUtil.getTranslatedStage(newStage, language);

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{transactionAddress}}", escapeHtml(transactionAddress))
                    .replace("{{newStage}}", escapeHtml(formattedStage));

            sendEmail(toEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send stage update email to {}", toEmail, e);
        }
    }

    String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
            return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Send email change confirmation email with a token link.
     */
    public void sendEmailChangeConfirmation(com.example.courtierprobackend.user.dataaccesslayer.UserAccount user, String newEmail, String token) {
        // This is a simple implementation. You may want to use templates and localization.
        String subject = "Confirm your new email address";
        String confirmationUrl = frontendBaseUrl + "/confirm-email?token=" + token;
        String body = String.format("Hello %s,\n\nPlease confirm your new email address by clicking the link below:\n%s\n\nIf you did not request this change, please ignore this email.",
                user.getFirstName() != null ? user.getFirstName() : "User", confirmationUrl);
        sendSimpleEmail(newEmail, subject, body);
    }

    void sendSimpleEmail(String to, String subject, String body) {
        // Minimal implementation using JavaMail. You may want to use your existing logic or templates.
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(gmailUsername, gmailPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(gmailUsername, "CourtierPro"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    String convertPlainTextToHtml(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }

        // Escape HTML first to prevent XSS
        String escaped = plainText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");

        // Handle heading sizes (centered)
        escaped = escaped.replaceAll(
                "(?s)\\[HEADING-SM\\](.*?)\\[/HEADING-SM\\]",
                "<h4 style=\"font-size: 1.25em; font-weight: 700; margin: 14px 0 8px 0; text-align: center;\">$1</h4>"
        );
        escaped = escaped.replaceAll(
                "(?s)\\[HEADING-MD\\](.*?)\\[/HEADING-MD\\]",
                "<h3 style=\"font-size: 1.5em; font-weight: 700; margin: 15px 0 10px 0; text-align: center;\">$1</h3>"
        );
        escaped = escaped.replaceAll(
                "(?s)\\[HEADING-LG\\](.*?)\\[/HEADING-LG\\]",
                "<h2 style=\"font-size: 1.75em; font-weight: 700; margin: 18px 0 12px 0; text-align: center;\">$1</h2>"
        );
        // Default heading (centered)
        escaped = escaped.replaceAll(
                "(?s)\\[HEADING\\](.*?)\\[/HEADING\\]",
                "<h3 style=\"font-size: 1.5em; font-weight: 700; margin: 15px 0 10px 0; text-align: center;\">$1</h3>"
        );

        // Handle [BOX]...[/BOX] (default blue, dotall to allow multi-line content, max-width 600px, centered)
        escaped = escaped.replaceAll(
                "(?s)\\[BOX\\](.*?)\\[/BOX\\]",
                "<div style=\"border-left: 4px solid #3b82f6; background-color: #eff6ff; padding: 15px; margin: 10px 0; border-radius: 4px; max-width: 600px; text-align: center;\"><p style=\"margin: 0; color: #1e3a8a; font-weight: 500;\">$1</p></div>"
        );

        // Handle colored boxes with hex or named colors: [BOX-blue], [BOX-#FF5733], etc.
        escaped = handleDynamicBoxes(escaped);

        // Handle colored boxes [BOX-RED], [BOX-BLUE], [BOX-GREEN], [BOX-YELLOW] (legacy support)
        escaped = escaped.replaceAll(
                "(?s)\\[BOX-RED\\](.*?)\\[/BOX-RED\\]",
                "<div style=\"border-left: 4px solid #ef4444; background-color: #fee2e2; padding: 15px; margin: 10px 0; border-radius: 4px; max-width: 600px; text-align: center;\"><p style=\"margin: 0; color: #7f1d1d; font-weight: 500;\">$1</p></div>"
        );
        escaped = escaped.replaceAll(
                "(?s)\\[BOX-BLUE\\](.*?)\\[/BOX-BLUE\\]",
                "<div style=\"border-left: 4px solid #3b82f6; background-color: #eff6ff; padding: 15px; margin: 10px 0; border-radius: 4px; max-width: 600px; text-align: center;\"><p style=\"margin: 0; color: #1e3a8a; font-weight: 500;\">$1</p></div>"
        );
        escaped = escaped.replaceAll(
                "(?s)\\[BOX-GREEN\\](.*?)\\[/BOX-GREEN\\]",
                "<div style=\"border-left: 4px solid #22c55e; background-color: #f0fdf4; padding: 15px; margin: 10px 0; border-radius: 4px; max-width: 600px; text-align: center;\"><p style=\"margin: 0; color: #166534; font-weight: 500;\">$1</p></div>"
        );
        escaped = escaped.replaceAll(
                "(?s)\\[BOX-YELLOW\\](.*?)\\[/BOX-YELLOW\\]",
                "<div style=\"border-left: 4px solid #eab308; background-color: #fefce8; padding: 15px; margin: 10px 0; border-radius: 4px; max-width: 600px; text-align: center;\"><p style=\"margin: 0; color: #713f12; font-weight: 500;\">$1</p></div>"
        );

        // Handle [BOLD]...[/BOLD]
        escaped = escaped.replaceAll(
                "(?s)\\[BOLD\\](.*?)\\[/BOLD\\]",
                "<strong>$1</strong>"
        );

        // Handle [ITALIC]...[/ITALIC]
        escaped = escaped.replaceAll(
                "(?s)\\[ITALIC\\](.*?)\\[/ITALIC\\]",
                "<em>$1</em>"
        );

        // Handle [HIGHLIGHT]...[/HIGHLIGHT]
        escaped = escaped.replaceAll(
                "(?s)\\[HIGHLIGHT\\](.*?)\\[/HIGHLIGHT\\]",
                "<span style=\"background-color: #fef08a; padding: 0 4px; border-radius: 3px;\">$1</span>"
        );

        // Handle highlight colors: [HIGHLIGHT-yellow], [HIGHLIGHT-pink], etc.
        escaped = handleHighlightColors(escaped);

        // Handle [SEPARATOR]
        escaped = escaped.replace("[SEPARATOR]", "<hr style=\"border: none; border-top: 1px solid #e5e7eb; margin: 16px 0;\" />");

        // Convert double newlines to paragraph breaks
        String html = escaped.replace("\n\n", "</p><p>");

        // Convert single newlines to line breaks
        html = html.replace("\n", "<br>");

        // Wrap in paragraph tags and center within container for consistent layout
        html = "<div style=\"max-width: 600px; margin: 0 auto;\"><p>" + html + "</p></div>";

        return html;
    }

    String handleDynamicBoxes(String text) {
        // Map des couleurs nommées - BOX colors
        java.util.Map<String, String> colorMap = new java.util.HashMap<>();
        colorMap.put("gray", "#6b7280");
        colorMap.put("red", "#ef4444");
        colorMap.put("green", "#22c55e");
        colorMap.put("blue", "#3b82f6");
        colorMap.put("yellow", "#eab308");
        colorMap.put("orange", "#f97316");
        colorMap.put("white", "#ffffff");

        // Map des couleurs de background et texte associées
        java.util.Map<String, String[]> colorStyles = new java.util.HashMap<>();
        colorStyles.put("#6b7280", new String[]{"#f3f4f6", "#1f2937"}); // gray
        colorStyles.put("#ef4444", new String[]{"#fee2e2", "#7f1d1d"}); // red
        colorStyles.put("#22c55e", new String[]{"#f0fdf4", "#166534"}); // green
        colorStyles.put("#3b82f6", new String[]{"#eff6ff", "#1e3a8a"}); // blue
        colorStyles.put("#eab308", new String[]{"#fefce8", "#713f12"}); // yellow
        colorStyles.put("#f97316", new String[]{"#ffedd5", "#92400e"}); // orange
        colorStyles.put("#ffffff", new String[]{"#f9fafb", "#111827"}); // white

        // Pattern pour [BOX-color]
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?s)\\[BOX-([a-zA-Z]+)\\](.*?)\\[/BOX-[a-zA-Z]+\\]"
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String colorKey = matcher.group(1).toLowerCase();
            String content = matcher.group(2);

            // Déterminer la couleur border
            String borderColor = colorMap.getOrDefault(colorKey, colorMap.get("blue"));
            String[] bgAndText = colorStyles.get(borderColor);
            String bgColor = "#eff6ff";
            String textColor = "#1e3a8a";

            if (bgAndText != null) {
                bgColor = bgAndText[0];
                textColor = bgAndText[1];
            }

            String replacement = "<div style=\"border-left: 4px solid " + borderColor + "; background-color: " + bgColor + "; padding: 15px; margin: 10px 0; border-radius: 4px; max-width: 600px; text-align: center;\"><p style=\"margin: 0; color: " + textColor + "; font-weight: 500;\">" + content + "</p></div>";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    String handleHighlightColors(String text) {
        // Map des couleurs highlight
        java.util.Map<String, String> highlightColorMap = new java.util.HashMap<>();
        highlightColorMap.put("yellow", "#fef08a");
        highlightColorMap.put("pink", "#fbcfe8");
        highlightColorMap.put("blue", "#bfdbfe");
        highlightColorMap.put("green", "#bbf7d0");
        highlightColorMap.put("orange", "#fed7aa");

        // Pattern pour [HIGHLIGHT-color]
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?s)\\[HIGHLIGHT-([a-zA-Z]+)\\](.*?)\\[/HIGHLIGHT-[a-zA-Z]+\\]"
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String colorKey = matcher.group(1).toLowerCase();
            String content = matcher.group(2);

            // Déterminer la couleur highlight
            String bgColor = highlightColorMap.getOrDefault(colorKey, highlightColorMap.get("yellow"));

            String replacement = "<span style=\"background-color: " + bgColor + "; padding: 0 4px; border-radius: 3px;\">" + content + "</span>";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Process conditional blocks [IF-variable]...[/IF-variable]
     * The content is shown only if the variable will be replaced with a non-empty value.
     * This method should be called BEFORE variable replacement.
     */
    String handleConditionalBlocks(String text, java.util.Map<String, String> variableValues) {
        // Pattern pour [IF-variableName]...[/IF-variableName]
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\[IF-([a-zA-Z0-9_]+)\\](.*?)\\[/IF-\\1\\]",
                java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String content = matcher.group(2);

            // Check if the variable has a non-empty value
            String variableValue = variableValues.get(variableName);
            boolean hasValue = variableValue != null && !variableValue.trim().isEmpty();

            // If variable has value, keep the content (without IF tags), otherwise remove everything
            String replacement = hasValue ? content : "";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Translate document status enum to human-readable text
     */
    private String translateDocumentStatus(Object status, boolean isFrench) {
        if (status == null) return "";
        String statusStr = status.toString();

        if (isFrench) {
            return switch (statusStr) {
                case "REQUESTED" -> "Demandé";
                case "SUBMITTED" -> "Soumis";
                case "APPROVED" -> "Approuvé";
                case "NEEDS_REVISION" -> "À réviser";
                case "REJECTED" -> "Rejeté";
                default -> statusStr;
            };
        } else {
            return switch (statusStr) {
                case "REQUESTED" -> "Requested";
                case "SUBMITTED" -> "Submitted";
                case "APPROVED" -> "Approved";
                case "NEEDS_REVISION" -> "Needs Revision";
                case "REJECTED" -> "Rejected";
                default -> statusStr;
            };
        }
    }

    /**
     * Generates a standard email footer
     */
    String getEmailFooter() {
        return "<hr style=\"border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;\">" +
                "<p style=\"color: #666; font-size: 12px; line-height: 1.6;\">" +
                // French
                "Merci,<br>" +
                "Cordialement,<br>" +
                "<strong>Équipe CourtierPro</strong>" +
                "<br><br>" +
                // English
                "Thank you,<br>" +
                "Best regards,<br>" +
                "<strong>CourtierPro Team</strong>" +
                "</p>";
    }
}