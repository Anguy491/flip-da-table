package com.flip.backend.service;

import com.flip.backend.api.dto.UserDtos.*;
import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    private UserEntity me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new IllegalStateException("unauthenticated");
        String email = auth.getName();
        return users.findByEmail(email).orElseThrow(() -> new IllegalStateException("user not found"));
    }

    public UserInfo getInfo() {
        var u = me();
        return new UserInfo(u.getId(), u.getEmail(), u.getNickname());
    }

    public UserInfo update(UpdateRequest req) {
        var u = me();
        boolean changed = false;
        if (req.nickname != null && !req.nickname.isBlank()) {
            u.setNickname(req.nickname.trim());
            changed = true;
        }
        if (req.password != null && !req.password.isBlank()) {
            u.setPasswordHash(encoder.encode(req.password));
            changed = true;
        }
        if (changed) users.save(u);
        return new UserInfo(u.getId(), u.getEmail(), u.getNickname());
    }
}
