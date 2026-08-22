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
import com.flip.backend.persistence.UserRepository;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService uds;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService uds, UserRepository users) {
        this.jwtService = jwtService;
        this.uds = uds;
        this.users = users;
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
                var user = users.findByEmailIgnoreCase(EmailNormalizer.normalize(email)).orElseThrow();
                Object rawVersion = claims.get("ver");
                int tokenVersion = rawVersion instanceof Number number ? number.intValue() : 0;
                if (tokenVersion != user.getAuthVersion()) throw new IllegalArgumentException("stale token");
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
