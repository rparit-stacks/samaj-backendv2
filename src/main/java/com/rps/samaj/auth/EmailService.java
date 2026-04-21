package com.rps.samaj.auth;

import com.rps.samaj.config.app.RuntimeConfigService;
import org.springframework.stereotype.Service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

@Service
public class EmailService {

    private final RuntimeConfigService runtimeConfig;

    public EmailService(RuntimeConfigService runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Send a simple email. Falls back gracefully if SMTP not configured.
     */
    public boolean sendEmail(String to, String subject, String body) {
        return sendEmail(to, subject, body, null);
    }

    /**
     * Send an email. If {@code htmlBody} is provided, sends HTML; otherwise plain text.
     */
    public boolean sendEmail(String to, String subject, String textBody, String htmlBody) {
        try {
            var smtp = runtimeConfig.getSmtpConfig();

            if (!smtp.isConfigured()) {
                logToConsole("SMTP not configured. Email to: " + to + " | Subject: " + subject);
                return true; // Return true to not block the flow
            }

            String password = smtp.password() != null ? smtp.password() : "";

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.host", smtp.host());
            props.put("mail.smtp.port", String.valueOf(smtp.port()));
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtp.username(), password);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtp.fromEmail(), smtp.fromName()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "utf-8");
            if (htmlBody != null && !htmlBody.isBlank()) {
                message.setContent(htmlBody, "text/html; charset=utf-8");
            } else {
                message.setText(textBody != null ? textBody : "", "utf-8");
            }

            Transport.send(message);
            logToConsole("Email sent to: " + to);
            return true;
        } catch (Exception e) {
            logToConsole("Failed to send email to " + to + ": " + e.getMessage());
            return false; // Don't block flow if email fails
        }
    }

    /**
     * Send OTP email with formatted template.
     */
    public boolean sendOtpEmail(String email, String otp) {
        String siteName = runtimeConfig.getString(RuntimeConfigService.KEY_SITE_NAME, "Samaj");
        String subject = "Your " + siteName + " OTP Code";
        String textBody = String.format(
                "Hello,\n\n" +
                        "Your OTP code is: %s\n\n" +
                        "This code will expire in 10 minutes.\n" +
                        "Do not share this code with anyone.\n\n" +
                        "If you didn't request this code, please ignore this email.\n\n" +
                        "Regards,\n" +
                        "%s Team",
                otp,
                siteName
        );
        String htmlBody = EmailTemplates.otpHtml(siteName, otp, 10);
        return sendEmail(email, subject, textBody, htmlBody);
    }

    /**
     * Send registration confirmation email.
     */
    public boolean sendWelcomeEmail(String email, String name) {
        String subject = "Welcome to Samaj Community!";
        String body = String.format(
            "Hello %s,\n\n" +
            "Welcome to Samaj! We're excited to have you join our community.\n\n" +
            "Your account has been successfully created. You can now:\n" +
            "- Connect with other community members\n" +
            "- Explore matrimony profiles\n" +
            "- Participate in events\n" +
            "- Access exclusive content\n\n" +
            "Happy exploring!\n\n" +
            "Regards,\n" +
            "Samaj Team",
            name != null ? name : "User"
        );
        return sendEmail(email, subject, body);
    }

    /**
     * Send password reset email.
     */
    public boolean sendPasswordResetEmail(String email, String resetCode) {
        String subject = "Reset Your Samaj Password";
        String body = String.format(
            "Hello,\n\n" +
            "We received a request to reset your password. " +
            "Use the code below to reset your password:\n\n" +
            "Code: %s\n\n" +
            "This code will expire in 10 minutes.\n" +
            "If you didn't request this, please ignore this email.\n\n" +
            "Regards,\n" +
            "Samaj Team",
            resetCode
        );
        return sendEmail(email, subject, body);
    }

    /**
     * Send event notification email.
     */
    public boolean sendEventNotificationEmail(String email, String eventName, String eventDetails) {
        String subject = "New Event: " + eventName;
        String body = String.format(
            "Hello,\n\n" +
            "A new event has been posted:\n\n" +
            "%s\n\n" +
            "%s\n\n" +
            "Visit the app to register and get more details.\n\n" +
            "Regards,\n" +
            "Samaj Team",
            eventName, eventDetails != null ? eventDetails : ""
        );
        return sendEmail(email, subject, body);
    }

    private void logToConsole(String message) {
        System.out.println("[EmailService] " + message);
    }
}
