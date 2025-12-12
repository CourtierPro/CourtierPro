package com.example.courtierprobackend.email;

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

            String subject = isFrench ? ("Document soumis : " + displayName) : ("Document Submitted: " + displayName);

            String templatePath = isFrench
                    ? "email-templates/document_submitted_fr.html"
                    : "email-templates/document_submitted_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{uploaderName}}", escapeHtml(uploaderName))
                    .replace("{{documentName}}", escapeHtml(displayName))
                    .replace("{{transactionId}}",
                            escapeHtml(request.getTransactionRef().getTransactionId().toString()));

            sendEmail(brokerEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load document submitted email template", e);
        } catch (MessagingException e) {
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

            String subject = isFrench
                    ? ("Document demandé : " + displayName)
                    : ("Document Requested: " + displayName);

            String templatePath = isFrench
                    ? "email-templates/document_requested_fr.html"
                    : "email-templates/document_requested_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{documentName}}", escapeHtml(displayName));

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load document requested email template", e);
        } catch (MessagingException e) {
            logger.error("Failed to send document requested notification to {}", clientEmail, e);
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

            String subject = isFrench ? ("Document vérifié : " + displayName) : ("Document Reviewed: " + displayName);

            String templatePath = isFrench
                    ? "email-templates/document_review_fr.html"
                    : "email-templates/document_review_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String statusLine = isFrench
                    ? (request.getStatus().toString().equalsIgnoreCase("NEEDS_REVISION")
                            ? "Votre courtier <strong>" + escapeHtml(brokerName)
                                    + "</strong> a demandé une révision pour le document suivant :"
                            : "Votre courtier <strong>" + escapeHtml(brokerName)
                                    + "</strong> a approuvé le document suivant :")
                    : (request.getStatus().toString().equalsIgnoreCase("NEEDS_REVISION")
                            ? "Your broker <strong>" + escapeHtml(brokerName)
                                    + "</strong> requested a revision for the following document:"
                            : "Your broker <strong>" + escapeHtml(brokerName)
                                    + "</strong> approved the following document:");

            String notesBlock = "";
            if (request.getBrokerNotes() != null && !request.getBrokerNotes().isBlank()) {
                notesBlock = isFrench
                        ? "<div class=\"divider\"></div><div class=\"card\"><p class=\"label\">Notes :</p><blockquote class=\"blockquote\">"
                                + escapeHtml(request.getBrokerNotes()) + "</blockquote></div>"
                        : "<div class=\"divider\"></div><div class=\"card\"><p class=\"label\">Notes:</p><blockquote class=\"blockquote\">"
                                + escapeHtml(request.getBrokerNotes()) + "</blockquote></div>";
            }

            String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{statusLine}}", statusLine)
                    .replace("{{documentName}}", escapeHtml(displayName))
                    .replace("{{transactionId}}", escapeHtml(request.getTransactionRef().getTransactionId().toString()))
                    .replace("{{notesBlock}}", notesBlock);

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load document review email template", e);
        } catch (MessagingException e) {
            logger.error("Failed to send document status notification to {}", clientEmail, e);
        }
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
        message.setContent(body, "text/html; charset=utf-8");

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

            String subject = isFrench
                    ? ("Mise à jour de transaction : " + transactionAddress)
                    : ("Transaction Update: " + transactionAddress);

            String templatePath = isFrench
                    ? "email-templates/stage_update_fr.html"
                    : "email-templates/stage_update_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String emailBody = htmlTemplate
                    .replace("{{clientName}}", escapeHtml(clientName))
                    .replace("{{brokerName}}", escapeHtml(brokerName))
                    .replace("{{transactionAddress}}", escapeHtml(transactionAddress))
                    .replace("{{newStage}}", escapeHtml(newStage));

            sendEmail(toEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load stage update email template", e);
        } catch (MessagingException e) {
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
}
