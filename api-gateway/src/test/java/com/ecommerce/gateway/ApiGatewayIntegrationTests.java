package com.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.gateway.discovery.locator.enabled=false"
        }
)
class ApiGatewayIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldExposeActuatorHealth() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldHandleCorsPreflightRequest() {
        webTestClient.options()
                .uri("/api/products")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().exists("Access-Control-Allow-Methods");
    }

    @Test
    void shouldAttachCorrelationIdHeaderInResponse() {
        webTestClient.get()
                .uri("/actuator/health")
                .header("X-Correlation-Id", "test-corr-1234")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "test-corr-1234");
    }
}
