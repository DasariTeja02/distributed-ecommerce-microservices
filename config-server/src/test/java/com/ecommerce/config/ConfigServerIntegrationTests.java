package com.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldServeApplicationConfiguration() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/application/default", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo("application");
    }

    @Test
    void shouldServeOrderServiceConfiguration() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/order-service/default", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo("order-service");
    }

    @Test
    void shouldServeApiGatewayConfiguration() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api-gateway/default", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo("api-gateway");
    }
}
