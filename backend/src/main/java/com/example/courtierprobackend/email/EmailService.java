package com.example.courtierprobackend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${gmail.username}")
    private String gmailUsername;

    @Value("${gmail.password}")
    private String gmailPassword;

    public void sendPasswordSetupEmail(String toEmail, String passwordSetupUrl) {
        try {
            // Configure Gmail SMTP
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
            message.setSubject("Welcome to CourtierPro - Set Your Password");

            String emailBody = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2c3e50;">Welcome to CourtierPro!</h2>
                        <p>You've been invited to join our platform.</p>
                        <p>Click the button below to set your password:</p>
                        <table role="presentation" style="margin: 30px 0;">
                            <tr>
                                <td>
                                    <a href="%s" style="background-color: #4CAF50; color: white; padding: 14px 28px; text-decoration: none; border-radius: 4px; display: inline-block; font-weight: bold;">Set Your Password</a>
                                </td>
                            </tr>
                        </table>
                        <p style="margin-top: 30px;">Or copy and paste this URL into your browser:</p>
                        <p style="color: #666; font-size: 12px; word-break: break-all;">%s</p>
                        <p style="margin-top: 30px; color: #999; font-size: 12px;">This link will expire in 7 days.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                        <p style="color: #999; font-size: 12px;">Thanks,<br>CourtierPro Team</p>
                    </div>
                </body>
                </html>
                """, passwordSetupUrl, passwordSetupUrl);

            message.setContent(emailBody, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("✅ Password setup email sent successfully to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            
            // Log the URL so it's not lost if email fails
            System.out.println("=".repeat(80));
            System.out.println("MANUAL PASSWORD SETUP URL FOR: " + toEmail);
            System.out.println(passwordSetupUrl);
            System.out.println("=".repeat(80));
        }
    }
}
