package com.ecommerce.gateway;

import com.ecommerce.gateway.service.UserService;
import com.ecommerce.gateway.util.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.gateway.discovery.locator.enabled=false"
        }
)
class SecurityRbacIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Login with invalid credentials should return 401 Unauthorized")
    void loginWithInvalidCredentialsShouldFail() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "customer1", "password", "wrongpassword"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Login with valid customer credentials returns JWT with server-assigned ROLE_CUSTOMER")
    void loginWithCustomerCredentialsShouldReturnCustomerRole() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "customer1", "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.roles[0]").isEqualTo("ROLE_CUSTOMER")
                .jsonPath("$.userId").isEqualTo("cust-001");
    }

    @Test
    @DisplayName("Client cannot escalate privileges during login by sending ROLE_ADMIN in payload")
    void clientCannotEscalateRolesInLogin() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", "customer1",
                        "password", "password123",
                        "role", "ROLE_ADMIN" // Attack attempt: client tries to choose admin role
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.roles[0]").isEqualTo("ROLE_CUSTOMER"); // Server must assign ROLE_CUSTOMER
    }

    @Test
    @DisplayName("Protected endpoints without Authorization header return 401 Unauthorized")
    void protectedEndpointWithoutTokenShouldReturn401() {
        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("customerId", "cust-001"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Protected endpoints with malformed token return 401 Unauthorized")
    void protectedEndpointWithMalformedTokenShouldReturn401() {
        webTestClient.get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Customer attempting admin-only POST /api/products returns 403 Forbidden")
    void customerAttemptingProductCreationShouldReturn403() {
        String customerToken = jwtUtils.generateToken("customer1", "cust-001", List.of("ROLE_CUSTOMER"));

        webTestClient.post()
                .uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "New Laptop", "price", 999.99))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Customer attempting to view all orders GET /api/orders returns 403 Forbidden")
    void customerAttemptingToViewAllOrdersShouldReturn403() {
        String customerToken = jwtUtils.generateToken("customer1", "cust-001", List.of("ROLE_CUSTOMER"));

        webTestClient.get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Customer attempting to view all payments GET /api/payments returns 403 Forbidden")
    void customerAttemptingToViewAllPaymentsShouldReturn403() {
        String customerToken = jwtUtils.generateToken("customer1", "cust-001", List.of("ROLE_CUSTOMER"));

        webTestClient.get()
                .uri("/api/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Customer attempting direct inventory management POST /api/inventory returns 403 Forbidden")
    void customerAttemptingInventoryManagementShouldReturn403() {
        String customerToken = jwtUtils.generateToken("customer1", "cust-001", List.of("ROLE_CUSTOMER"));

        webTestClient.post()
                .uri("/api/inventory")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("productId", "PROD-001", "quantity", 100))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Customer attempting to view another customer's orders returns 403 Forbidden")
    void customerAttemptingToViewOtherCustomerOrdersShouldReturn403() {
        String customerToken = jwtUtils.generateToken("customer1", "cust-001", List.of("ROLE_CUSTOMER"));

        webTestClient.get()
                .uri("/api/orders/customer/cust-002") // requesting cust-002 while authenticated as cust-001
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Admin token is recognized with ROLE_ADMIN from server login")
    void adminLoginReturnsAdminRole() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "admin", "password", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.roles[0]").isEqualTo("ROLE_ADMIN")
                .jsonPath("$.userId").isEqualTo("admin-001");
    }
}
