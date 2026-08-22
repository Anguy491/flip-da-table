package com.flip.backend.api;

import com.flip.backend.api.dto.AuthDtos.*;
import com.flip.backend.service.AuthService;
import com.flip.backend.service.PasswordResetService;
import com.flip.backend.security.AuthFeatureProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService svc;
    private final PasswordResetService passwordReset;
    private final AuthFeatureProperties properties;

    public AuthController(AuthService svc, PasswordResetService passwordReset, AuthFeatureProperties properties) {
        this.svc = svc;
        this.passwordReset = passwordReset;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(svc.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(svc.login(req));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<CapabilitiesResponse> capabilities() {
        boolean googleEnabled = properties.googleEnabled();
        var google = new GoogleCapability(
                googleEnabled,
                googleEnabled ? properties.googleClientId() : "",
                googleEnabled ? properties.googleLoginUri() : ""
        );
        return ResponseEntity.ok(new CapabilitiesResponse(
                properties.passwordResetEnabled(),
                properties.supportEmail(),
                google
        ));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordReset.requestReset(req.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "If an account exists for that email, a reset link will be sent."
        ));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordReset.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
