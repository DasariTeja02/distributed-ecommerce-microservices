package com.ecommerce.gateway.controller;

import com.ecommerce.gateway.model.User;
import com.ecommerce.gateway.service.UserService;
import com.ecommerce.gateway.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication & Identity", description = "User authentication, JWT issuance, and registration APIs")
public class AuthController {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    public AuthController(JwtUtils jwtUtils, UserService userService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT", description = "Verifies username/password against server storage. Client cannot choose its role; trusted server role is embedded in the JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful, JWT issued",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"token\":\"eyJhbG...\",\"type\":\"Bearer\",\"userId\":\"cust-001\",\"username\":\"customer1\",\"roles\":[\"ROLE_CUSTOMER\"]}"))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or disabled account",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":\"Unauthorized\",\"message\":\"Invalid username or password\"}")))
    })
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
    @Operation(summary = "Register a new customer account", description = "Creates a new user record strictly assigned ROLE_CUSTOMER. Clients cannot register as admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\":\"User registered successfully\",\"userId\":\"user-1234abcd\",\"username\":\"newuser\",\"roles\":[\"ROLE_CUSTOMER\"],\"token\":\"eyJ...\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error or username already taken",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":\"Bad Request\",\"message\":\"Username already exists\"}")))
    })
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
