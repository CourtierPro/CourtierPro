package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
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
@RequiredArgsConstructor
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
            // 1) Charger les settings d’organisation (langue par défaut + templates)
            OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

            // 2) Déterminer la langue effective : param > default org > "en"
            String effectiveLang = languageCode;
            if (effectiveLang == null || effectiveLang.isBlank()) {
                effectiveLang = settings.getDefaultLanguage();
            }
            if (effectiveLang == null || effectiveLang.isBlank()) {
                effectiveLang = "en";
            }

            boolean isFrench = effectiveLang.equalsIgnoreCase("fr");

            // 3) Récupérer subject + body depuis la DB
            String subject;
            String bodyText;

            if (isFrench) {
                subject = settings.getInviteSubjectFr();
                bodyText = settings.getInviteBodyFr();
            } else {
                subject = settings.getInviteSubjectEn();
                bodyText = settings.getInviteBodyEn();
            }

            // 4) Fallback si les templates sont vides
            if (subject == null || subject.isBlank()) {
                subject = isFrench ? "Invitation CourtierPro" : "CourtierPro Invitation";
            }
            if (bodyText == null || bodyText.isBlank()) {
                bodyText = isFrench
                        ? "Bonjour {{name}}, votre compte CourtierPro a été créé."
                        : "Hi {{name}}, your CourtierPro account has been created.";
            }

            // 5) Remplacer {{name}} par l’email (pour l’instant)
            String displayName = toEmail;
            if (bodyText.contains("{{name}}")) {
                bodyText = bodyText.replace("{{name}}", displayName);
            }

            // 6) Textes statiques (peuvent aller en DB plus tard)
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

            // 7) Choisir le bon fichier HTML (invite_en.html / invite_fr.html)
            String templatePath = isFrench
                    ? "email-templates/invite_fr.html"
                    : "email-templates/invite_en.html";

            String htmlTemplate = loadTemplateFromClasspath(templatePath);

            // 8) Remplacer les placeholders dans le template HTML
            String emailBody = htmlTemplate;
            emailBody = emailBody.replace("{{subject}}", escapeHtml(subject));
            emailBody = emailBody.replace("{{introText}}", introText);
            emailBody = emailBody.replace("{{bodyText}}", bodyText);
            emailBody = emailBody.replace("{{buttonLabel}}", escapeHtml(buttonLabel));
            emailBody = emailBody.replace("{{passwordLink}}", passwordSetupUrl);
            emailBody = emailBody.replace("{{expiresText}}", expiresText);
            emailBody = emailBody.replace("{{footerText}}", footerText);

            // 9) Config SMTP Gmail
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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(emailBody, "text/html; charset=utf-8");

    public void sendDocumentSubmittedNotification(DocumentRequest request, String brokerEmail) {
        String subject = "Document Submitted: " + request.getCustomTitle();
        String body = "A document has been submitted for transaction " + request.getTransactionRef().getTransactionId();
        
        sendEmail(brokerEmail, subject, body);
    }

    public void sendDocumentStatusUpdatedNotification(DocumentRequest request, String clientEmail) {
        String subject = "Document Status Updated: " + request.getCustomTitle();
        String body = "Your document status is now: " + request.getStatus();

        sendEmail(clientEmail, subject, body);
    }

    private boolean sendEmail(String to, String subject, String body) {
        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).build())
                        .body(Body.builder()
                                .html(Content.builder().data(body).build()) // Use HTML body
                                .build())
                        .build())
                .source(sourceEmail)
                .build();

        try {
            sesClient.sendEmail(emailRequest);
            logger.info("Email sent successfully to {}", to);
            return true;

        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send password setup email to {}", toEmail, e);
            logger.warn("Manual password setup URL for {}: {}", toEmail, passwordSetupUrl);
            return false;
        } catch (IOException ioException) {
            logger.error("Failed to load email template from classpath", ioException);
            return false;
        }
    }

    // Charge le template HTML depuis /resources
    private String loadTemplateFromClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);

        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    // Petit escape HTML pour subject / labels
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
