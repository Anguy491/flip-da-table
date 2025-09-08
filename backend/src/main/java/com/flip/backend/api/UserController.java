package com.flip.backend.api;

import com.flip.backend.api.dto.UserDtos.*;
import com.flip.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService svc;
    public UserController(UserService svc) { this.svc = svc; }

    @GetMapping("/info")
    public ResponseEntity<UserInfo> me() { return ResponseEntity.ok(svc.getInfo()); }

    @PostMapping("/info")
    public ResponseEntity<UserInfo> update(@Valid @RequestBody UpdateRequest req) {
        return ResponseEntity.ok(svc.update(req));
    }
}
