package com.ecommerce.gateway.controller;

import com.ecommerce.gateway.model.User;
import com.ecommerce.gateway.service.UserService;
import com.ecommerce.gateway.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    public AuthController(JwtUtils jwtUtils, UserService userService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Authenticate strictly against server-side storage - client input cannot select roles
        Optional<User> authenticatedUser = userService.authenticate(username, password);
        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid username or password"
            ));
        }

        User user = authenticatedUser.get();
        List<String> roles = List.of(user.role());
        String token = jwtUtils.generateToken(user.username(), user.id(), roles);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "userId", user.id(),
                "username", user.username(),
                "roles", roles
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        try {
            User registeredUser = userService.register(username, password);
            List<String> roles = List.of(registeredUser.role());
            String token = jwtUtils.generateToken(registeredUser.username(), registeredUser.id(), roles);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully",
                    "userId", registeredUser.id(),
                    "username", registeredUser.username(),
                    "roles", roles,
                    "token", token,
                    "type", "Bearer"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Bad Request",
                    "message", e.getMessage()
            ));
        }
    }
}
