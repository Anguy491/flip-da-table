package com.flip.backend.api;

import com.flip.backend.api.dto.AuthDtos.*;
import com.flip.backend.security.AuthFeatureProperties;
import com.flip.backend.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {
    private static final Logger log = LoggerFactory.getLogger(GoogleAuthController.class);
    private final GoogleAuthService googleAuth;
    private final AuthFeatureProperties properties;

    public GoogleAuthController(GoogleAuthService googleAuth, AuthFeatureProperties properties) {
        this.googleAuth = googleAuth;
        this.properties = properties;
    }

    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> callback(
            @CookieValue(name = "g_csrf_token", required = false) String csrfCookie,
            @RequestParam(name = "g_csrf_token", required = false) String csrfBody,
            @RequestParam(name = "credential", required = false) String credential
    ) {
        try {
            var handoff = googleAuth.begin(credential, csrfCookie, csrfBody);
            String key = GoogleAuthService.LINK_PURPOSE.equals(handoff.purpose()) ? "link" : "code";
            return redirect(properties.publicUrl() + "/auth/google/callback#" + key + "=" + handoff.code());
        } catch (Exception ex) {
            log.warn("Google sign-in callback failed: {}", ex.getClass().getSimpleName());
            return redirect(properties.publicUrl() + "/login#google_error=failed");
        }
    }

    @PostMapping("/exchange")
    public ResponseEntity<AuthResponse> exchange(@Valid @RequestBody AuthCodeRequest request) {
        return ResponseEntity.ok(googleAuth.exchange(request.code()));
    }

    @PostMapping("/link")
    public ResponseEntity<AuthResponse> link(@Valid @RequestBody GoogleLinkRequest request) {
        return ResponseEntity.ok(googleAuth.link(request.code(), request.password()));
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(location)).build();
    }
}
