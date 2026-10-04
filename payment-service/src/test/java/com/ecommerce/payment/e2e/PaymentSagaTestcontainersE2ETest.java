package com.ecommerce.payment.e2e;

import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.model.ProcessedEvent;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.ProcessedEventRepository;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-End Testcontainers tests for Payment Service Saga transactions:
 * - Payment processing upon InventoryReservedEvent
 * - Payment decline simulation and failure event emission
 * - Idempotency protection against duplicate inventory events
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=payment-service-e2e-group"
})
public class PaymentSagaTestcontainersE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_payment_db")
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
    private PaymentRepository paymentRepository;

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
    @DisplayName("Payment Saga 1: InventoryReservedEvent triggers successful financial debit")
    void testInventoryReserved_ProcessesPaymentSuccessfully() throws Exception {
        UUID orderId = UUID.randomUUID();
        String eventId = "evt-pay-inv-" + UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "eventId", eventId,
                "eventType", "InventoryReservedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-pay-1",
                "totalAmount", 249.99,
                "simulateFailure", false,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-pay-success-1"
        );

        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        producer.send("inventory-events", orderId.toString(), objectMapper.writeValueAsString(payload)).get(5, TimeUnit.SECONDS);

        // Await payment creation in PostgreSQL
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Payment> opt = paymentRepository.findByOrderId(orderId);
            assertThat(opt).isPresent();
            Payment payment = opt.get();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getTransactionId()).isNotBlank();
        });

        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("Payment Saga 2: Simulated decline marks payment FAILED")
    void testInventoryReserved_SimulateFailure_MarksPaymentFailed() throws Exception {
        UUID orderId = UUID.randomUUID();
        String eventId = "evt-pay-fail-" + UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "eventId", eventId,
                "eventType", "InventoryReservedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-pay-fail",
                "totalAmount", 99.00,
                "simulateFailure", true,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-pay-fail-1"
        );

        KafkaTemplate<String, String> producer = createTestKafkaTemplate();
        producer.send("inventory-events", orderId.toString(), objectMapper.writeValueAsString(payload)).get(5, TimeUnit.SECONDS);

        // Await payment marked FAILED in PostgreSQL
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Payment> opt = paymentRepository.findByOrderId(orderId);
            assertThat(opt).isPresent();
            Payment payment = opt.get();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isNotBlank();
        });

        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    @DisplayName("Payment Saga 3: Duplicate InventoryReservedEvent is ignored idempotently")
    void testDuplicateInventoryReservedEvent_IgnoredIdempotently() throws Exception {
        UUID orderId = UUID.randomUUID();
        String duplicateEventId = "evt-dup-pay-" + UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "eventId", duplicateEventId,
                "eventType", "InventoryReservedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-dup-pay",
                "totalAmount", 120.00,
                "simulateFailure", false,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-dup-pay"
        );

        String json = objectMapper.writeValueAsString(payload);
        KafkaTemplate<String, String> producer = createTestKafkaTemplate();

        // 1st transmission
        producer.send("inventory-events", orderId.toString(), json).get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<Payment> opt = paymentRepository.findByOrderId(orderId);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        });

        long countBefore = paymentRepository.count();

        // 2nd transmission (duplicate)
        producer.send("inventory-events", orderId.toString(), json).get(5, TimeUnit.SECONDS);
        Thread.sleep(1500);

        // Verify no duplicate record was created
        assertThat(paymentRepository.count()).isEqualTo(countBefore);
    }
}
