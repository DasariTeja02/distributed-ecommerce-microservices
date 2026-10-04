package com.ecommerce.order.e2e;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.model.ProcessedEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-End Integration Tests for Order Service Saga Choreography
 * using real Testcontainers (PostgreSQL & Apache Kafka).
 *
 * Scenarios:
 * 1. Happy Path: OrderCreated -> InventoryReserved -> PaymentCompleted -> Order CONFIRMED
 * 2. Payment Failure: PaymentFailed -> Order CANCELLED with compensation
 * 3. Inventory Failure: InventoryReservationFailed -> Order CANCELLED
 * 4. Duplicate Event: Duplicate Kafka message safely ignored via idempotency repository
 * 5. Recovery: Service consumer recovery after pause/restart
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=order-service-e2e-group"
})
public class OrderSagaTestcontainersE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_order_db")
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
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private KafkaTemplate<String, String> createTestKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        String bootstrap = kafka.isRunning() ? kafka.getBootstrapServers() : "localhost:9092";
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    @DisplayName("E2E Saga 1: Happy Path - PaymentCompletedEvent updates order to CONFIRMED")
    void testHappyPath_PaymentCompleted_OrderConfirmed() throws Exception {
        // Step 1: Customer creates order
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-e2e-1")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-LAPTOP-1")
                                .quantity(1)
                                .price(new BigDecimal("1999.00"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();
        assertThat(orderResponse.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Step 2: Publish PaymentCompletedEvent to payment-events topic
        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        String eventId = "evt-pay-success-" + UUID.randomUUID();
        Map<String, Object> paymentPayload = Map.of(
                "eventId", eventId,
                "eventType", "PaymentCompletedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-e2e-1",
                "transactionId", "TXN-" + UUID.randomUUID(),
                "amount", 1999.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-happy-path-1"
        );

        producer.send("payment-events", orderId.toString(), objectMapper.writeValueAsString(paymentPayload)).get(5, TimeUnit.SECONDS);

        // Step 3: Await OrderSagaKafkaListener transition in PostgreSQL
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Order> opt = orderRepository.findById(orderId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });

        // Step 4: Verify idempotency table record
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("E2E Saga 2: Payment Failure - PaymentFailedEvent cancels order")
    void testPaymentFailure_CompensatesAndCancelsOrder() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-e2e-2")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-PHONE-1")
                                .quantity(1)
                                .price(new BigDecimal("999.00"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();
        assertThat(orderResponse.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Publish PaymentFailedEvent
        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        String eventId = "evt-pay-failed-" + UUID.randomUUID();
        Map<String, Object> failurePayload = Map.of(
                "eventId", eventId,
                "eventType", "PaymentFailedEvent",
                "orderId", orderId.toString(),
                "failureReason", "Card declined: Insufficient funds (Simulated)",
                "amount", 999.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-payment-failed-1"
        );

        producer.send("payment-events", orderId.toString(), objectMapper.writeValueAsString(failurePayload)).get(5, TimeUnit.SECONDS);

        // Verify order transitions to CANCELLED
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Order> opt = orderRepository.findById(orderId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        });

        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("E2E Saga 3: Inventory Failure - InventoryReservationFailedEvent cancels order")
    void testInventoryReservationFailed_CancelsOrder() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-e2e-3")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-OUT-OF-STOCK")
                                .quantity(50)
                                .price(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();

        // Publish InventoryReservationFailedEvent
        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        String eventId = "evt-inv-failed-" + UUID.randomUUID();
        Map<String, Object> failurePayload = Map.of(
                "eventId", eventId,
                "eventType", "InventoryReservationFailedEvent",
                "orderId", orderId.toString(),
                "reason", "Insufficient stock: only 2 available, requested 50",
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-inv-failed-1"
        );

        producer.send("inventory-events", orderId.toString(), objectMapper.writeValueAsString(failurePayload)).get(5, TimeUnit.SECONDS);

        // Verify order transitions to CANCELLED
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Order> opt = orderRepository.findById(orderId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        });

        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("E2E Saga 4: Duplicate Event - Same Kafka event delivered twice is processed only once")
    void testDuplicateEvent_ProcessedOnlyOnce() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-e2e-4")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-KEYBOARD")
                                .quantity(1)
                                .price(new BigDecimal("150.00"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();

        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        String duplicateEventId = "evt-duplicate-test-" + UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "eventId", duplicateEventId,
                "eventType", "PaymentCompletedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-e2e-4",
                "transactionId", "TXN-DUP-1",
                "amount", 150.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-duplicate-test"
        );

        String json = objectMapper.writeValueAsString(payload);

        // Send first time
        producer.send("payment-events", orderId.toString(), json).get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Order> opt = orderRepository.findById(orderId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });

        long countBefore = processedEventRepository.count();

        // Send second time (duplicate event with identical eventId)
        producer.send("payment-events", orderId.toString(), json).get(5, TimeUnit.SECONDS);

        // Sleep briefly to ensure listener receives it
        Thread.sleep(1500);

        // Order remains CONFIRMED and processed events count has not duplicated
        Optional<Order> optAfter = orderRepository.findById(orderId);
        assertThat(optAfter).isPresent();
        assertThat(optAfter.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(processedEventRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("E2E Saga 5: Recovery - Kafka processing pauses and resumes safely")
    void testServiceRecovery_PauseAndResumeListener() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-e2e-5")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-MOUSE")
                                .quantity(1)
                                .price(new BigDecimal("80.00"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();

        // Pause listener containers to simulate downstream service outage
        kafkaListenerEndpointRegistry.getListenerContainers().forEach(MessageListenerContainer::pause);

        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        String eventId = "evt-recovery-" + UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "eventId", eventId,
                "eventType", "PaymentCompletedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-e2e-5",
                "transactionId", "TXN-REC-1",
                "amount", 80.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-recovery-1"
        );

        producer.send("payment-events", orderId.toString(), objectMapper.writeValueAsString(payload)).get(5, TimeUnit.SECONDS);

        // While paused, order remains PENDING
        Thread.sleep(1000);
        assertThat(orderRepository.findById(orderId).get().getStatus()).isEqualTo(OrderStatus.PENDING);

        // Simulate Service Recovery: Resume listeners
        kafkaListenerEndpointRegistry.getListenerContainers().forEach(MessageListenerContainer::resume);

        // After recovery, message is consumed from Kafka offset and order transitions to CONFIRMED
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Order> opt = orderRepository.findById(orderId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }
}
