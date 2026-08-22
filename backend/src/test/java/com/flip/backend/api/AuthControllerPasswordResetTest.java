package com.flip.backend.api;

import com.flip.backend.security.AuthFeatureProperties;
import com.flip.backend.service.AuthService;
import com.flip.backend.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerPasswordResetTest {
    @Test
    void forgotPasswordUsesTheSameAcceptedResponseForKnownAndUnknownAccounts() throws Exception {
        var reset = mock(PasswordResetService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        mock(AuthService.class), reset, mock(AuthFeatureProperties.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String expected = "If an account exists for that email, a reset link will be sent.";
        for (String email : new String[]{"known@example.com", "unknown@example.com"}) {
            mvc.perform(post("/api/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.message").value(expected));
        }

        verify(reset).requestReset("known@example.com");
        verify(reset).requestReset("unknown@example.com");
    }
}
