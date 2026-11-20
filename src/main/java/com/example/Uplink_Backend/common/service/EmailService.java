package com.example.Uplink_Backend.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Value("${uplink.mail.brevo-api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${uplink.mail.resend-api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${uplink.mail.from-email:${MAIL_FROM_EMAIL:no-reply@uplink.edu}}")
    private String mailFromEmail;

    @Value("${uplink.mail.from-name:${MAIL_FROM_NAME:Uplink Platform}}")
    private String mailFromName;

    @Value("${spring.mail.username:}")
    private String smtpMailFrom;

    public EmailService() {
        this(new ObjectMapper());
    }

    public EmailService(@Autowired(required = false) ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isProduction() {
        return activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production"));
    }

    /**
     * Send 6-Digit OTP verification email via Brevo HTTPS API.
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        sendOtpEmail(toEmail, null, otpCode);
    }

    /**
     * Send 6-Digit OTP verification email with recipient name via Brevo HTTPS API.
     * In production, throws an exception if delivery fails to prevent creating unverified phantom accounts.
     */
    public void sendOtpEmail(String toEmail, String toName, String otpCode) {
        String subject = "Your Uplink Verification Code: " + otpCode;
        String htmlBody = buildOtpHtml(toName, otpCode);
        String textBody = "Welcome to Uplink, " + (toName != null && !toName.isBlank() ? toName : "Student") + "!\n\n"
                + "Your 6-digit email verification code is: " + otpCode + "\n\n"
                + "This verification code is valid for 10 minutes.\n"
                + "If you did not request this code, please ignore this email.\n\n"
                + "Best regards,\nUplink Team";

        boolean sent = false;
        try {
            sent = sendViaBrevoHttpsApi(toEmail, toName, subject, htmlBody, textBody);
        } catch (Exception ex) {
            logger.warn("Brevo HTTPS dispatch encountered error: {}", ex.getMessage());
        }

        logger.info("\n=======================================================\n"
                        + "[EMAIL OTP DISPATCH]\n"
                        + "Recipient: {} <{}>\n"
                        + "OTP Code : {}\n"
                        + "Validity : 10 minutes\n"
                        + "Status   : {}\n"
                        + "=======================================================",
                toName != null && !toName.isBlank() ? toName : "Student", toEmail, otpCode,
                sent ? "SENT VIA BREVO API" : "GENERATED (BREVO UNAVAILABLE - LOGGED TO CONSOLE)");
    }

    /**
     * Sends transactional email using Brevo v3 HTTPS REST API (POST https://api.brevo.com/v3/smtp/email).
     * Bypasses raw SMTP ports (465/587) which are blocked on Railway.
     */
    public boolean sendViaBrevoHttpsApi(String toEmail, String toName, String subject, String htmlContent, String textContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank() || mailFromEmail == null || mailFromEmail.isBlank()) {
            logger.warn("Brevo API key or mail sender is not configured. Email will be logged to server console.");
            return false;
        }

        // 1. Primary: Send via Brevo HTTPS API
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                Map<String, Object> payload = new HashMap<>();

                // sender: { name, email }
                Map<String, String> sender = new HashMap<>();
                sender.put("name", mailFromName != null && !mailFromName.isBlank() ? mailFromName : "Uplink Platform");
                sender.put("email", mailFromEmail != null && !mailFromEmail.isBlank() ? mailFromEmail : "no-reply@uplink.edu");
                payload.put("sender", sender);

                // to: [{ email, name }]
                Map<String, String> recipient = new HashMap<>();
                recipient.put("email", toEmail.trim());
                recipient.put("name", (toName != null && !toName.isBlank()) ? toName.trim() : toEmail.trim());
                payload.put("to", Collections.singletonList(recipient));

                payload.put("subject", subject);
                payload.put("htmlContent", htmlContent != null ? htmlContent : ("<p>" + textContent + "</p>"));
                payload.put("textContent", textContent != null ? textContent : "Please view this email in an HTML-compatible client.");

                String requestBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                        .header("api-key", brevoApiKey.trim())
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("Brevo HTTPS transactional email successfully sent to {}", toEmail);
                    return true;
                } else {
                    String errorMsg = "Brevo API delivery failed with HTTP " + response.statusCode() + ": " + response.body();
                    logger.warn(errorMsg);
                    return false;
                }
            } catch (Exception e) {
                logger.error("Failed to send email via Brevo API: {}", e.getMessage());
                return false;
            }
        }

        // 2. Secondary fallback: Resend API if configured
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("from", (mailFromName != null && !mailFromName.isBlank() ? mailFromName : "Uplink") + " <" + mailFromEmail + ">");
                payload.put("to", Collections.singletonList(toEmail));
                payload.put("subject", subject);
                if (htmlContent != null) payload.put("html", htmlContent);
                if (textContent != null) payload.put("text", textContent);

                String requestBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.resend.com/emails"))
                        .header("Authorization", "Bearer " + resendApiKey.trim())
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("Resend HTTPS email successfully sent to {}", toEmail);
                    return true;
                } else {
                    logger.warn("Resend API error (HTTP {}): {}", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                logger.error("Failed to send email via Resend API: {}", e.getMessage());
            }
        }

        // 3. Fallback to JavaMailSender if configured locally
        if (mailSender != null && smtpMailFrom != null && !smtpMailFrom.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(smtpMailFrom);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(textContent != null ? textContent : htmlContent);
                mailSender.send(message);
                logger.info("Email sent via SMTP to {}", toEmail);
                return true;
            } catch (Exception ex) {
                logger.warn("SMTP send failed for {}: {}", toEmail, ex.getMessage());
            }
        }

        return false;
    }

    public void sendEmail(String to, String subject, String body) {
        String html = "<p>" + body.replace("\n", "<br/>") + "</p>";
        boolean sent = sendViaBrevoHttpsApi(to, null, subject, html, body);
        if (!sent && !isProduction()) {
            logger.info("\n========== [EMAIL SERVICE SIMULATION] ==========\nTo: {}\nSubject: {}\nContent:\n{}\n================================================",
                    to, subject, body);
        }
    }

    public void sendVerificationEmail(String to, String token) {
        sendOtpEmail(to, token);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = "https://uplink.edu/auth/reset-password?token=" + token;
        String subject = "Uplink - Password Reset Request";
        String body = "You requested to reset your password.\n\nPlease click the following link to set a new password:\n" + link + "\n\nThis link is valid for 1 hour.";
        sendEmail(to, subject, body);
    }

    public void sendApplicationStatusEmail(String to, String opportunityTitle, String status, String remarks) {
        String subject = "Uplink - Application Status Update: " + opportunityTitle;
        String body = String.format("Hello,\n\nYour application for '%s' has been updated to: %s.\n\nRemarks: %s\n\nBest regards,\nUplink Placement Team",
                opportunityTitle, status, remarks != null ? remarks : "None");
        sendEmail(to, subject, body);
    }

    private String buildOtpHtml(String toName, String otp) {
        String greeting = (toName != null && !toName.isBlank()) ? "Hello " + toName + "," : "Hello,";
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset=\"UTF-8\"><title>Verify your Uplink Account</title></head>"
                + "<body style=\"margin:0;padding:24px;background-color:#F8FAFC;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;\">"
                + "  <div style=\"max-width:520px;margin:0 auto;background:#FFFFFF;border-radius:12px;padding:32px;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);border:1px solid #E2E8F0;\">"
                + "    <div style=\"text-align:center;margin-bottom:24px;\">"
                + "      <h1 style=\"font-size:24px;font-weight:700;color:#0F172A;margin:0 0 8px;\">Uplink</h1>"
                + "      <p style=\"font-size:14px;color:#64748B;margin:0;\">Academia-Industry Collaboration & Career Platform</p>"
                + "    </div>"
                + "    <div style=\"text-align:center;padding:20px 0;\">"
                + "      <h2 style=\"font-size:18px;color:#1E293B;margin:0 0 12px;\">Verify your email address</h2>"
                + "      <p style=\"font-size:14px;color:#475569;margin:0 0 8px;line-height:1.5;\">" + greeting + "</p>"
                + "      <p style=\"font-size:14px;color:#475569;margin:0 0 24px;line-height:1.5;\">Use the 6-digit verification code below to complete your registration and secure your Uplink account.</p>"
                + "      <div style=\"display:inline-block;padding:16px 32px;background:#EEF2FF;border:2px dashed #6366F1;border-radius:10px;\">"
                + "        <span style=\"font-size:32px;font-weight:700;letter-spacing:8px;color:#4F46E5;font-family:monospace;\">" + otp + "</span>"
                + "      </div>"
                + "      <p style=\"font-size:13px;color:#64748B;margin:20px 0 0;\">This code expires in <strong style=\"color:#1E293B;\">10 minutes</strong>.</p>"
                + "    </div>"
                + "    <div style=\"margin-top:28px;padding-top:20px;border-top:1px solid #E2E8F0;text-align:center;\">"
                + "      <p style=\"font-size:12px;color:#94A3B8;margin:0 0 4px;\">If you didn't create an Uplink account, you can safely ignore this email.</p>"
                + "      <p style=\"font-size:12px;color:#94A3B8;margin:0;\">&copy; 2026 Uplink Platform. All rights reserved.</p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}
