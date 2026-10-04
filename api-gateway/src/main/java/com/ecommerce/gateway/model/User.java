package com.ecommerce.gateway.model;

import java.time.Instant;

/**
 * Server-side User model for secure authentication and authorization.
 * Roles are retrieved strictly from server-side storage, never trusted from client input.
 */
public record User(
        String id,
        String username,
        String passwordHash,
        String role,
        boolean enabled,
        Instant createdAt
) {}
