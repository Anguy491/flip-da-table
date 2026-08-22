package com.flip.backend.service;

import com.flip.backend.persistence.AuthHandoffCodeRepository;
import com.flip.backend.persistence.PasswordResetTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthTokenCleanupService {
    private final PasswordResetTokenRepository resetTokens;
    private final AuthHandoffCodeRepository handoffCodes;

    public AuthTokenCleanupService(PasswordResetTokenRepository resetTokens, AuthHandoffCodeRepository handoffCodes) {
        this.resetTokens = resetTokens;
        this.handoffCodes = handoffCodes;
    }

    @Scheduled(fixedDelayString = "${app.auth.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        resetTokens.deleteExpiredOrUsed(cutoff);
        handoffCodes.deleteExpiredOrConsumed(cutoff);
    }
}
