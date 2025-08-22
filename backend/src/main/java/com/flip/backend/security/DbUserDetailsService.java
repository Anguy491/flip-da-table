package com.flip.backend.security;

import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    public DbUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity u = repo.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        var authorities = Arrays.stream(u.getRoles().split(","))
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                .toList();
        return new org.springframework.security.core.userdetails.User(
                u.getEmail(), u.getPasswordHash(), authorities);
    }
}
