package com.ecommerce.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationGatewayFilterFactory.class);

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private final com.ecommerce.gateway.util.JwtUtils jwtUtils;

    public AuthenticationGatewayFilterFactory(com.ecommerce.gateway.util.JwtUtils jwtUtils) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("requiredRole");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod() != null ? request.getMethod().name() : "GET";

            // Allow public paths through
            if (isPublicPath(path, method)) {
                return chain.filter(exchange);
            }

            // Check Authorization Header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[Auth Filter] Missing or invalid Authorization header for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            // Cryptographic JWT signature and expiration validation
            if (!jwtUtils.validateToken(token)) {
                log.warn("[Auth Filter] Invalid or expired JWT token for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String resolvedUserId = jwtUtils.extractUserId(token);
            String username = jwtUtils.extractUsername(token);
            List<String> roles = jwtUtils.extractRoles(token);
            boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
            boolean isCustomer = roles != null && roles.contains("ROLE_CUSTOMER");

            // Explicit requiredRole check if configured on route
            if (config.getRequiredRole() != null && (roles == null || !roles.contains(config.getRequiredRole()))) {
                log.warn("[Auth Filter] User {} lacks configured required role: {}", resolvedUserId, config.getRequiredRole());
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Enforce RBAC matrix across microservices
            if (!isAdmin) {
                // Admin-only endpoints restricted from customers:
                // 1. Product mutations (create/update/delete)
                if (path.startsWith("/api/products") && !method.equals("GET")) {
                    log.warn("[Auth Filter] Forbidden: Non-admin {} attempted {} on {}", resolvedUserId, method, path);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

                // 2. Direct inventory management
                if (path.startsWith("/api/inventory") && method.equals("POST") && !path.contains("/reserve") && !path.contains("/release")) {
                    log.warn("[Auth Filter] Forbidden: Non-admin {} attempted inventory management on {}", resolvedUserId, path);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

                // 3. View all orders (only admin can list all orders across customers)
                if ((path.equals("/api/orders") || path.equals("/api/orders/")) && method.equals("GET")) {
                    log.warn("[Auth Filter] Forbidden: Customer {} attempted to view all orders", resolvedUserId);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

                // 4. View all payment records
                if ((path.equals("/api/payments") || path.equals("/api/payments/")) && method.equals("GET")) {
                    log.warn("[Auth Filter] Forbidden: Customer {} attempted to view all payments", resolvedUserId);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

                // 5. Customer can only view their own customer orders
                if (path.startsWith("/api/orders/customer/")) {
                    String targetCustomer = path.substring("/api/orders/customer/".length()).trim();
                    boolean matchesSelf = (resolvedUserId != null && resolvedUserId.equalsIgnoreCase(targetCustomer))
                            || (username != null && username.equalsIgnoreCase(targetCustomer));
                    if (!matchesSelf) {
                        log.warn("[Auth Filter] Forbidden: Customer {} tried to view orders of {}", resolvedUserId, targetCustomer);
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                }
            }

            String resolvedRoles = (roles != null && !roles.isEmpty()) ? String.join(",", roles) : "ROLE_CUSTOMER";

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(USER_ID_HEADER, resolvedUserId != null ? resolvedUserId : (username != null ? username : "anonymous"))
                    .header(USER_ROLES_HEADER, resolvedRoles)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private boolean isPublicPath(String path, String method) {
        if (path.startsWith("/api/auth/") || path.equals("/api/auth")) {
            return true;
        }
        if (path.startsWith("/actuator/")) {
            return true;
        }
        if (path.startsWith("/api/products") && "GET".equalsIgnoreCase(method)) {
            return true;
        }
        if (path.startsWith("/api/system/") || path.equals("/api/system")) {
            return true;
        }
        if (path.startsWith("/api/orders/events/stream")) {
            return true;
        }
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/webjars/")) {
            return true;
        }
        return false;
    }

    public static class Config {
        private String requiredRole;

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }
    }
}
