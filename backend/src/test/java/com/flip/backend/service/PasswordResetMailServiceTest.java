package com.flip.backend.service;

import com.flip.backend.security.AuthFeatureProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PasswordResetMailServiceTest {
    @Test
    void sendsMultipartRecoveryMailWithFragmentTokenAndIdempotencyHeader() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        AuthFeatureProperties properties = mock(AuthFeatureProperties.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        when(properties.publicUrl()).thenReturn("https://game.anguy.dev");
        when(properties.mailFrom()).thenReturn("Flip Da Table <no-reply@mail.anguy.dev>");
        when(properties.supportEmail()).thenReturn("support@anguy.dev");
        var service = new PasswordResetMailService(sender, properties, new SimpleMeterRegistry());

        service.sendPasswordReset(new PasswordResetMailEvent(
                "player@example.com", "Pixel Player", "raw-token", "password-reset/hash"));

        verify(sender).send(message);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        String rawMessage = output.toString(StandardCharsets.UTF_8);
        assertTrue(rawMessage.contains("reset-password#token=raw-token"));
        assertTrue(rawMessage.contains("Resend-Idempotency-Key: password-reset/hash"));
        assertTrue(rawMessage.contains("Reply-To: support@anguy.dev"));
        assertTrue(rawMessage.contains("multipart/alternative"));
    }
}
