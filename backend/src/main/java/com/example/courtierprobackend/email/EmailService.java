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

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final String gmailUsername;
    private final String gmailPassword;
    private final OrganizationSettingsService organizationSettingsService;

    public EmailService(
            @Value("${gmail.username}") String gmailUsername,
            @Value("${gmail.password}") String gmailPassword,
            OrganizationSettingsService organizationSettingsService) {
        this.gmailUsername = gmailUsername;
        this.gmailPassword = gmailPassword;
        this.organizationSettingsService = organizationSettingsService;
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

            String subject;
            String bodyText;

            if (isFrench) {
                subject = settings.getInviteSubjectFr();
                bodyText = settings.getInviteBodyFr();
            } else {
                subject = settings.getInviteSubjectEn();
                bodyText = settings.getInviteBodyEn();
            }

            if (subject == null || subject.isBlank()) {
                subject = isFrench ? "Invitation CourtierPro" : "CourtierPro Invitation";
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{name}}, votre compte CourtierPro a été créé."
                        : "Hi {{name}}, your CourtierPro account has been created.";
            }

            String displayName = toEmail;
            if (bodyText.contains("{{name}}")) {
                bodyText = bodyText.replace("{{name}}", displayName);
            }

            String introText;
            String buttonLabel;
            String expiresText;
            String footerText;

            if (isFrench) {
                introText = "Vous avez été invité à rejoindre la plateforme CourtierPro.";
                buttonLabel = "Définir votre mot de passe";
                expiresText = "Ce lien expirera dans 7 jours.";
                footerText = "Merci,<br>L'équipe CourtierPro";
            } else {
                introText = "You have been invited to join the CourtierPro platform.";
                buttonLabel = "Set Your Password";
                expiresText = "This link will expire in 7 days.";
                footerText = "Thanks,<br>CourtierPro Team";
            }

            String templatePath = isFrench
                    ? "email-templates/invite_fr.html"
                    : "email-templates/invite_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate;
            emailBody = emailBody.replace("{{subject}}", escapeHtml(subject));
            emailBody = emailBody.replace("{{introText}}", introText);
            emailBody = emailBody.replace("{{bodyText}}", bodyText);
            emailBody = emailBody.replace("{{buttonLabel}}", escapeHtml(buttonLabel));
            emailBody = emailBody.replace("{{passwordLink}}", passwordSetupUrl);
            emailBody = emailBody.replace("{{expiresText}}", expiresText);
            emailBody = emailBody.replace("{{footerText}}", footerText);

            return sendEmail(toEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send password setup email to {}", toEmail, e);
            logger.warn("Manual password setup URL for {}: {}", toEmail, passwordSetupUrl);
            return false;
        } catch (IOException ioException) {
            logger.error("Failed to load email template from classpath", ioException);
            return false;
        }
    }

    public void sendDocumentSubmittedNotification(DocumentRequest request, String brokerEmail, String uploaderName,
            String documentName, String docType, String brokerLanguage) {
        try {
            boolean isFrench = brokerLanguage != null && brokerLanguage.equalsIgnoreCase("fr");

            // Translate document type based on broker's language
            String translatedDocType = translateDocumentType(docType, isFrench);
            String displayName = documentName.equals(docType) ? translatedDocType : documentName;

            // Get settings for templates
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getDocumentSubmittedSubjectFr() : settings.getDocumentSubmittedSubjectEn();
            String bodyText = isFrench ? settings.getDocumentSubmittedBodyFr() : settings.getDocumentSubmittedBodyEn();

            // Fallback to defaults if not configured
            if (subject == null || subject.isBlank()) {
                subject = isFrench ? "Document soumis" : "Document Submitted";
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{uploaderName}}, votre document {{documentName}} a été soumis pour la transaction {{transactionId}}."
                        : "Hello {{uploaderName}}, your document {{documentName}} has been submitted for the transaction {{transactionId}}.";
            }

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{uploaderName}}", escapeHtml(uploaderName))
                    .replace("{{documentName}}", escapeHtml(displayName))
                    .replace("{{documentType}}", escapeHtml(translatedDocType))
                    .replace("{{transactionId}}", escapeHtml(request.getTransactionRef().getTransactionId().toString()));

            sendEmail(brokerEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send document submitted notification to {}", brokerEmail, e);
        }
    }

    public void sendDocumentRequestedNotification(String clientEmail, String clientName, String brokerName,
            String documentName, String docType, String clientLanguage) {
        try {
            boolean isFrench = clientLanguage != null && clientLanguage.equalsIgnoreCase("fr");

            // Translate document type based on client's language
            String translatedDocType = translateDocumentType(docType, isFrench);
            String displayName = documentName.equals(docType) ? translatedDocType : documentName;

            // Get settings for templates
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            String subject = isFrench ? settings.getDocumentRequestedSubjectFr() : settings.getDocumentRequestedSubjectEn();
            String bodyText = isFrench ? settings.getDocumentRequestedBodyFr() : settings.getDocumentRequestedBodyEn();

            // Fallback to defaults if not configured
            if (subject == null || subject.isBlank()) {
                subject = isFrench ? "Document demandé" : "Document Requested";
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{clientName}}, {{brokerName}} a demandé le document {{documentName}}. Veuillez le soumettre dès que possible."
                        : "Hello {{clientName}}, {{brokerName}} has requested the document {{documentName}}. Please submit it as soon as possible.";
            }

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{documentName}}", escapeHtml(displayName))
                    .replace("{{documentType}}", escapeHtml(translatedDocType));

            sendEmail(clientEmail, subject, emailBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send document requested notification to {}", clientEmail, e);
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

            String emailBody = convertPlainTextToHtml(bodyText)
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{documentName}}", escapeHtml(displayName))
                    .replace("{{documentType}}", escapeHtml(translatedDocType))
                    .replace("{{transactionId}}", escapeHtml(request.getTransactionRef().getTransactionId().toString()))
                    .replace("{{status}}", escapeHtml(request.getStatus().toString()))
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

            String subject = isFrench
                    ? ("Offre soumise : " + propertyAddress)
                    : ("Offer Made: " + propertyAddress);

            String templatePath = isFrench
                    ? "email-templates/property_offer_made_fr.html"
                    : "email-templates/property_offer_made_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{propertyAddress}}", escapeHtml(propertyAddress))
                    .replace("{{offerAmount}}", escapeHtml(offerAmount))
                    .replace("{{offerRound}}", String.valueOf(offerRound));

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load property offer made email template", e);
        } catch (MessagingException e) {
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

            String subject = isFrench
                    ? ("Mise à jour de l'offre : " + propertyAddress)
                    : ("Offer Update: " + propertyAddress);

            String templatePath = isFrench
                    ? "email-templates/property_offer_status_fr.html"
                    : "email-templates/property_offer_status_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            // Build counterparty response block if present
            String counterpartyResponseBlock = "";
            if (counterpartyResponse != null && !counterpartyResponse.isBlank()) {
                String responseLabel = isFrench ? "Réponse du vendeur :" : "Seller's Response:";
                counterpartyResponseBlock = "<div class=\"card\"><p class=\"label\">" + responseLabel + "</p>" +
                        "<blockquote class=\"blockquote\">" + escapeHtml(counterpartyResponse) + "</blockquote></div>";
            }

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{propertyAddress}}", escapeHtml(propertyAddress))
                    .replace("{{previousStatus}}", escapeHtml(translatedPreviousStatus))
                    .replace("{{newStatus}}", escapeHtml(translatedNewStatus))
                    .replace("{{counterpartyResponseBlock}}", counterpartyResponseBlock);

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load property offer status email template", e);
        } catch (MessagingException e) {
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

            String subject = isFrench
                    ? ("Nouvelle offre reçue de " + buyerName)
                    : ("New Offer Received from " + buyerName);

            String templatePath = isFrench
                    ? "email-templates/offer_received_fr.html"
                    : "email-templates/offer_received_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{buyerName}}", escapeHtml(buyerName))
                    .replace("{{offerAmount}}", escapeHtml(offerAmount));

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load offer received email template", e);
        } catch (MessagingException e) {
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

            String subject = isFrench
                    ? ("Mise à jour de l'offre de " + buyerName)
                    : ("Offer Update from " + buyerName);

            String templatePath = isFrench
                    ? "email-templates/offer_status_fr.html"
                    : "email-templates/offer_status_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{buyerName}}", escapeHtml(buyerName))
                    .replace("{{previousStatus}}", escapeHtml(translatedPreviousStatus))
                    .replace("{{newStatus}}", escapeHtml(translatedNewStatus));

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load offer status email template", e);
        } catch (MessagingException e) {
            logger.error("Failed to send offer status notification to {}", clientEmail, e);
        }
    }

    /**
     * Translate offer status enum values to human-readable strings.
     */
    private String translateOfferStatus(String status, boolean isFrench) {
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

    private String translateDocumentType(String docType, boolean isFrench) {
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

    private boolean sendEmail(String to, String subject, String body)
            throws MessagingException, UnsupportedEncodingException {
        // Add footer to body
        String bodyWithFooter = body + "\n\n" + getEmailFooter();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

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
        message.setContent(bodyWithFooter, "text/html; charset=utf-8");

        Transport.send(message);
        logger.info("Email sent successfully to {}", to);
        return true;
    }

    private String loadTemplateFromClasspath(String path) throws IOException {
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

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Converts plain text to HTML by:
     * - Escaping HTML characters
     * - Converting \n\n (double newline) to paragraph breaks
     * - Converting \n (single newline) to line breaks
     */
    private String convertPlainTextToHtml(String plainText) {
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

        // Convert double newlines to paragraph breaks
        String html = escaped.replace("\n\n", "</p><p>");

        // Convert single newlines to line breaks
        html = html.replace("\n", "<br>");

        // Wrap in paragraph tags
        html = "<p>" + html + "</p>";

        return html;
    }

    /**
     * Generates a standard email footer
     */
    private String getEmailFooter() {
        return "<hr style=\"border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;\">" +
               "<p style=\"color: #666; font-size: 12px; line-height: 1.6;\">" +
               "Merci,<br>" +
               "Cordialement,<br>" +
               "<strong>Équipe CourtierPro</strong>" +
               "</p>";
    }
}
