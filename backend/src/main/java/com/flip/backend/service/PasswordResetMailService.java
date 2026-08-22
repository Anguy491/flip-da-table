package com.flip.backend.service;

import com.flip.backend.security.AuthFeatureProperties;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;

@Service
public class PasswordResetMailService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailService.class);
    private final JavaMailSender mailSender;
    private final AuthFeatureProperties properties;
    private final MeterRegistry meters;

    public PasswordResetMailService(JavaMailSender mailSender, AuthFeatureProperties properties, MeterRegistry meters) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.meters = meters;
    }

    @Async("authMailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendPasswordReset(PasswordResetMailEvent event) {
        try {
            String link = properties.publicUrl() + "/reset-password#token=" + event.token();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.mailFrom());
            helper.setTo(event.email());
            helper.setReplyTo(properties.supportEmail());
            helper.setSubject("Reset your Flip Da Table password");
            String greeting = event.nickname() == null || event.nickname().isBlank() ? "Player" : event.nickname();
            String plain = "Hi " + greeting + ",\n\nUse this link within 30 minutes to reset your Flip Da Table password:\n"
                    + link + "\n\nIf you did not request this, you can ignore this email.\n\nFlip Da Table";
            String html = "<p>Hi " + escapeHtml(greeting) + ",</p>"
                    + "<p>Use the link below within 30 minutes to reset your Flip Da Table password.</p>"
                    + "<p><a href=\"" + link + "\">Reset password</a></p>"
                    + "<p>If you did not request this, you can ignore this email.</p>";
            helper.setText(plain, html);
            message.setHeader("Resend-Idempotency-Key", event.idempotencyKey());
            mailSender.send(message);
            meters.counter("auth.password_reset.email", "result", "sent").increment();
        } catch (Exception ex) {
            meters.counter("auth.password_reset.email", "result", "failed").increment();
            // SMTP exceptions can include recipient details; keep production logs free of PII.
            log.error("Password reset email delivery failed: {}", ex.getClass().getSimpleName());
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
