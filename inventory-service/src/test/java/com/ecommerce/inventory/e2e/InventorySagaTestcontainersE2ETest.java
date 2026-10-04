package com.ecommerce.inventory.e2e;

import com.ecommerce.inventory.dto.ReservationItemRequest;
import com.ecommerce.inventory.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.ProcessedEvent;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.InventoryReservationRepository;
import com.ecommerce.inventory.repository.ProcessedEventRepository;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-End Testcontainers tests for Inventory Service Saga operations:
 * - Stock reservation upon OrderCreatedEvent
 * - Insufficient stock handling and failure emission
 * - Compensation rollback: releasing reserved stock upon PaymentFailedEvent
 * - Idempotency: duplicate events safely discarded
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=inventory-service-e2e-group"
})
public class InventorySagaTestcontainersE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_inventory_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        }
        if (kafka.isRunning()) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        }
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, String> createTestKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        String bootstrap = kafka.isRunning() ? kafka.getBootstrapServers() : "localhost:9092";
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    @DisplayName("Inventory Saga 1: OrderCreatedEvent atomically reserves stock in PostgreSQL")
    void testOrderCreated_ReservesStockSuccessfully() throws Exception {
        String productId = "PROD-INV-" + UUID.randomUUID();
        inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .availableQuantity(25)
                .reservedQuantity(0)
                .updatedAt(Instant.now())
                .build());

        UUID orderId = UUID.randomUUID();
        String eventId = "evt-inv-order-" + UUID.randomUUID();

        Map<String, Object> orderCreatedPayload = Map.of(
                "eventId", eventId,
                "eventType", "OrderCreatedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-inv-1",
                "items", List.of(
                        Map.of("productId", productId, "quantity", 5, "price", 100.0)
                ),
                "totalAmount", 500.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-inv-reserve-1"
        );

        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        producer.send("order-events", orderId.toString(), objectMapper.writeValueAsString(orderCreatedPayload)).get(5, TimeUnit.SECONDS);

        // Await stock deduction in database
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(inv.getAvailableQuantity()).isEqualTo(20);
            assertThat(inv.getReservedQuantity()).isEqualTo(5);
        });

        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("Inventory Saga 2: Insufficient stock leaves inventory unchanged")
    void testOrderCreated_InsufficientStock_LeavesStockUnchanged() throws Exception {
        String productId = "PROD-LOW-" + UUID.randomUUID();
        inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .availableQuantity(2)
                .reservedQuantity(0)
                .updatedAt(Instant.now())
                .build());

        UUID orderId = UUID.randomUUID();
        String eventId = "evt-inv-insufficient-" + UUID.randomUUID();

        Map<String, Object> orderPayload = Map.of(
                "eventId", eventId,
                "eventType", "OrderCreatedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-inv-2",
                "items", List.of(
                        Map.of("productId", productId, "quantity", 10, "price", 50.0)
                ),
                "totalAmount", 500.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-inv-fail-1"
        );

        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        producer.send("order-events", orderId.toString(), objectMapper.writeValueAsString(orderPayload)).get(5, TimeUnit.SECONDS);

        // Allow listener to process and confirm stock did not go negative
        Thread.sleep(2000);
        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(inv.getAvailableQuantity()).isEqualTo(2);
        assertThat(inv.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Inventory Saga 3: Compensating rollback - PaymentFailedEvent releases reserved stock")
    void testPaymentFailed_CompensatingStockRelease() throws Exception {
        String productId = "PROD-COMP-" + UUID.randomUUID();
        inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .availableQuantity(30)
                .reservedQuantity(0)
                .updatedAt(Instant.now())
                .build());

        UUID orderId = UUID.randomUUID();

        // 1. Manually reserve 4 units first
        inventoryService.reserveInventory(ReserveInventoryRequest.builder()
                .orderId(orderId)
                .items(List.of(
                        ReservationItemRequest.builder().productId(productId).quantity(4).build()
                ))
                .build());

        Inventory afterReserve = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(afterReserve.getAvailableQuantity()).isEqualTo(26);
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(4);

        // 2. Publish PaymentFailedEvent to trigger compensation
        String eventId = "evt-pay-failed-comp-" + UUID.randomUUID();
        Map<String, Object> failurePayload = Map.of(
                "eventId", eventId,
                "eventType", "PaymentFailedEvent",
                "orderId", orderId.toString(),
                "failureReason", "Card declined by bank",
                "amount", 400.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-comp-test"
        );

        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        producer.send("payment-events", orderId.toString(), objectMapper.writeValueAsString(failurePayload)).get(5, TimeUnit.SECONDS);

        // 3. Await compensation: reserved quantity returned to available
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(inv.getAvailableQuantity()).isEqualTo(30);
            assertThat(inv.getReservedQuantity()).isEqualTo(0);
        });

        assertThat(processedEventRepository.existsById("comp-" + eventId)).isTrue();
    }

    @Test
    @DisplayName("Inventory Saga 4: Duplicate OrderCreatedEvent is ignored without double-deduction")
    void testDuplicateOrderCreatedEvent_IgnoredIdempotently() throws Exception {
        String productId = "PROD-DUP-" + UUID.randomUUID();
        inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .availableQuantity(50)
                .reservedQuantity(0)
                .updatedAt(Instant.now())
                .build());

        UUID orderId = UUID.randomUUID();
        String duplicateEventId = "evt-dup-inv-" + UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "eventId", duplicateEventId,
                "eventType", "OrderCreatedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-inv-dup",
                "items", List.of(
                        Map.of("productId", productId, "quantity", 5, "price", 20.0)
                ),
                "totalAmount", 100.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-inv-dup-test"
        );

        String json = objectMapper.writeValueAsString(payload);
        KafkaTemplate<String, String> producer = createTestKafkaTemplate();

        // Send 1st time
        producer.send("order-events", orderId.toString(), json).get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(inv.getAvailableQuantity()).isEqualTo(45);
            assertThat(inv.getReservedQuantity()).isEqualTo(5);
        });

        // Send 2nd time (duplicate)
        producer.send("order-events", orderId.toString(), json).get(5, TimeUnit.SECONDS);
        Thread.sleep(1500);

        // Verify stock was NOT deducted again
        Inventory invAfter = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(invAfter.getAvailableQuantity()).isEqualTo(45);
        assertThat(invAfter.getReservedQuantity()).isEqualTo(5);
    }
}
