package com.ecommerce.gateway.service;

import com.ecommerce.gateway.model.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing user authentication and role lookups from server-side storage.
 * Prevents client-side role escalation by ensuring roles are strictly retrieved
 * from the trusted user database.
 */
@Service
public class UserService {

    private final Map<String, User> userDatabase = new ConcurrentHashMap<>();

    public UserService() {
        // Pre-seeded users with server-enforced roles
        // 1. Administrator
        registerInternal("admin", "admin123", "ROLE_ADMIN", "admin-001");
        // 2. Customers
        registerInternal("customer1", "password123", "ROLE_CUSTOMER", "cust-001");
        registerInternal("customer2", "password123", "ROLE_CUSTOMER", "cust-002");
    }

    private void registerInternal(String username, String password, String role, String id) {
        String hash = hashPassword(password);
        userDatabase.put(username.toLowerCase(), new User(
                id,
                username,
                hash,
                role,
                true,
                Instant.now()
        ));
    }

    /**
     * Authenticates credentials against server storage and returns the user with trusted roles.
     */
    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        User user = userDatabase.get(username.toLowerCase().trim());
        if (user == null || !user.enabled()) {
            return Optional.empty();
        }

        String computedHash = hashPassword(password);
        if (computedHash.equals(user.passwordHash())) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    /**
     * Registers a new customer user. Strictly enforces ROLE_CUSTOMER - clients cannot request ROLE_ADMIN.
     */
    public User register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        String key = username.toLowerCase().trim();
        if (userDatabase.containsKey(key)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        String id = "user-" + UUID.randomUUID().toString().substring(0, 8);
        User newUser = new User(
                id,
                username.trim(),
                hashPassword(password),
                "ROLE_CUSTOMER",
                true,
                Instant.now()
        );
        userDatabase.put(key, newUser);
        return newUser;
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(userDatabase.get(username.toLowerCase().trim()));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("SALT_ECOMMERCE_SECURE_" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
