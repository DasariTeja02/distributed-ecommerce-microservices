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

    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
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

            // Allow public paths through
            if (path.startsWith("/api/auth/") || (path.startsWith("/api/products") && request.getMethod().name().equals("GET"))) {
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

            // In Phase 3 (scaffolding auth point): we propagate stub claims or header if valid format
            // Phase 13 will hook cryptographic JWT parsing & role validation
            String resolvedUserId = "user-default";
            String resolvedRoles = config.getRequiredRole() != null ? config.getRequiredRole() : "ROLE_CUSTOMER";

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(USER_ID_HEADER, resolvedUserId)
                    .header(USER_ROLES_HEADER, resolvedRoles)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
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
