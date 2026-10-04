package com.ecommerce.gateway.controller;

import com.ecommerce.gateway.util.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtils jwtUtils;

    public AuthController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.getOrDefault("username", "customer");
        String role = "admin".equalsIgnoreCase(username) ? "ROLE_ADMIN" : "ROLE_CUSTOMER";
        String userId = "user-" + username.toLowerCase();

        String token = jwtUtils.generateToken(username, userId, List.of(role));

        return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "userId", userId,
                "username", username,
                "roles", List.of(role)
        ));
    }
}
