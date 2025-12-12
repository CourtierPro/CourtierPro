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
            OrganizationSettingsService organizationSettingsService
    ) {
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

    public void sendDocumentSubmittedNotification(DocumentRequest request, String brokerEmail, String uploaderName, String documentName) {
        String subject = "Document Submitted: " + documentName;
        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2c3e50;">Document Submitted</h2>
                        <p>Hello,</p>
                        <p>A document has been submitted by <strong>%s</strong>:</p>
                        <p style="background-color: #f8f9fa; padding: 15px; border-radius: 4px; font-size: 16px;">
                            <strong>%s</strong>
                        </p>
                        <p>Transaction ID: %s</p>
                        <p>Please log in to CourtierPro to review this document.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                        <p style="color: #999; font-size: 12px;">Thanks,<br>CourtierPro Team</p>
                    </div>
                </body>
                </html>
                """, uploaderName, documentName, request.getTransactionRef().getTransactionId());
        
        try {
            sendEmail(brokerEmail, subject, htmlBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send document submitted notification to {}", brokerEmail, e);
        }
    }

    public void sendDocumentRequestedNotification(String clientEmail, String clientName, String brokerName, String documentName) {
        String subject = "Document Requested: " + documentName;
        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2c3e50;">Document Request</h2>
                        <p>Hello %s,</p>
                        <p>Your broker <strong>%s</strong> has requested the following document:</p>
                        <p style="background-color: #f8f9fa; padding: 15px; border-radius: 4px; font-size: 16px;">
                            <strong>%s</strong>
                        </p>
                        <p>Please log in to CourtierPro to upload this document.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                        <p style="color: #999; font-size: 12px;">Thanks,<br>CourtierPro Team</p>
                    </div>
                </body>
                </html>
                """, clientName, brokerName, documentName);
        
        try {
            sendEmail(clientEmail, subject, htmlBody);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send document requested notification to {}", clientEmail, e);
        }
    }

        public void sendDocumentStatusUpdatedNotification(
            DocumentRequest request,
            String clientEmail,
            String brokerName,
            String documentName
        ) {
        try {
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();
            String defaultLang = settings != null ? settings.getDefaultLanguage() : null;
            boolean isFrench = defaultLang != null && defaultLang.equalsIgnoreCase("fr");

            String subject = isFrench ? ("Document vérifié : " + documentName) : ("Document Reviewed: " + documentName);

            String templatePath = isFrench
                    ? "email-templates/document_review_fr.html"
                    : "email-templates/document_review_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            String statusLine = isFrench
                    ? (request.getStatus().toString().equalsIgnoreCase("NEEDS_REVISION")
                        ? "Votre courtier <strong>" + escapeHtml(brokerName) + "</strong> a demandé une révision pour le document suivant :"
                        : "Votre courtier <strong>" + escapeHtml(brokerName) + "</strong> a approuvé le document suivant :")
                    : (request.getStatus().toString().equalsIgnoreCase("NEEDS_REVISION")
                        ? "Your broker <strong>" + escapeHtml(brokerName) + "</strong> requested a revision for the following document:"
                        : "Your broker <strong>" + escapeHtml(brokerName) + "</strong> approved the following document:");

            String notesBlock = "";
            if (request.getBrokerNotes() != null && !request.getBrokerNotes().isBlank()) {
                notesBlock = isFrench
                        ? "<div class=\"divider\"></div><div class=\"card\"><p class=\"label\">Notes :</p><blockquote class=\"blockquote\">" + escapeHtml(request.getBrokerNotes()) + "</blockquote></div>"
                        : "<div class=\"divider\"></div><div class=\"card\"><p class=\"label\">Notes:</p><blockquote class=\"blockquote\">" + escapeHtml(request.getBrokerNotes()) + "</blockquote></div>";
            }

                String emailBody = htmlTemplate
                    .replace("{{subject}}", escapeHtml(subject))
                    .replace("{{statusLine}}", statusLine)
                    .replace("{{documentName}}", escapeHtml(documentName))
                    .replace("{{transactionId}}", escapeHtml(request.getTransactionRef().getTransactionId().toString()))
                    .replace("{{notesBlock}}", notesBlock);

            sendEmail(clientEmail, subject, emailBody);
        } catch (IOException e) {
            logger.error("Failed to load document review email template", e);
        } catch (MessagingException e) {
            logger.error("Failed to send document status notification to {}", clientEmail, e);
        }
    }

    private boolean sendEmail(String to, String subject, String body) throws MessagingException, UnsupportedEncodingException {
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
