package com.flip.backend.security;

import org.springframework.lang.NonNull;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UserDetailsService;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService uds;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService uds) {
        this.jwtService = jwtService;
        this.uds = uds;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest req,
        @NonNull HttpServletResponse res,
        @NonNull FilterChain chain
    ) throws java.io.IOException, jakarta.servlet.ServletException {
        String auth = req.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Claims claims = jwtService.parse(token).getBody();
                String email = claims.getSubject();
                var userDetails = uds.loadUserByUsername(email);
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception ignored) { /* Global exception handling */ }
        }
        chain.doFilter(req, res);
    }
}
