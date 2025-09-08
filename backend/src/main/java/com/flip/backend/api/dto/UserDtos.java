package com.flip.backend.api.dto;

import jakarta.validation.constraints.Size;

public class UserDtos {
    public record UserInfo(Long userId, String email, String nickname) {}

    /** Update request: fields are optional; if present, validated. */
    public static class UpdateRequest {
        @Size(min=2, max=32)
        public String nickname;
        @Size(min=6, max=64)
        public String password;
        public UpdateRequest() {}
        public UpdateRequest(String nickname, String password) { this.nickname = nickname; this.password = password; }
    }
}
