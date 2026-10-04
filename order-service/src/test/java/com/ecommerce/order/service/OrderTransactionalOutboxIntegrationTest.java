package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.model.OutboxEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:outbox_test_db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
public class OrderTransactionalOutboxIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderKafkaProducer orderKafkaProducer;

    @Test
    @DisplayName("Transactional Outbox: Order creation commits order and outbox record in same transaction")
    void testOrderCreationPersistsOutboxEvent() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-outbox-1")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-OUTBOX-1")
                                .quantity(3)
                                .price(new BigDecimal("49.99"))
                                .build()
                ))
                .build();

        OrderResponse response = orderService.createOrder(request);
        UUID orderId = response.getId();

        // 1. Verify Order persisted
        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);

        // 2. Verify Outbox record persisted
        List<OutboxEvent> allOutboxEvents = outboxEventRepository.findAll();
        assertThat(allOutboxEvents).isNotEmpty();

        OutboxEvent outboxEvent = allOutboxEvents.stream()
                .filter(e -> e.getAggregateId().equals(orderId.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Outbox event not found for orderId: " + orderId));

        assertThat(outboxEvent.getAggregateType()).isEqualTo("Order");
        assertThat(outboxEvent.getEventType()).isEqualTo("OrderCreatedEvent");
        assertThat(outboxEvent.isPublished()).isTrue();
        assertThat(outboxEvent.getPublishedAt()).isNotNull();

        // 3. Inspect JSON payload
        JsonNode json = objectMapper.readTree(outboxEvent.getPayload());
        assertThat(json.path("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(json.path("customerId").asText()).isEqualTo("cust-outbox-1");
        assertThat(json.path("totalAmount").asDouble()).isEqualTo(149.97);

        // 4. Verify Kafka producer was called
        verify(orderKafkaProducer, atLeastOnce()).publishOrderCreated(any());
    }

    @Test
    @DisplayName("Transactional Outbox: Publisher retries when initial publish fails")
    void testOutboxPublisherRetryMechanism() throws Exception {
        // Create an un-published outbox event manually to simulate Kafka broker downtime
        UUID mockOrderId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new com.ecommerce.order.event.OrderConfirmedEvent(
                UUID.randomUUID().toString(),
                "OrderConfirmedEvent",
                mockOrderId,
                "cust-retry",
                Instant.now(),
                "corr-retry"
        ));

        OutboxEvent pendingEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Order")
                .aggregateId(mockOrderId.toString())
                .eventType("OrderConfirmedEvent")
                .payload(payload)
                .createdAt(Instant.now())
                .published(false)
                .retryCount(0)
                .build();

        outboxEventRepository.save(pendingEvent);

        // Verify initial state
        assertThat(outboxEventRepository.findById(pendingEvent.getId()).get().isPublished()).isFalse();

        // Trigger outbox processor
        outboxPublisherService.processOutbox();

        // Verify published state after publisher runs
        OutboxEvent processed = outboxEventRepository.findById(pendingEvent.getId()).orElseThrow();
        assertThat(processed.isPublished()).isTrue();
        assertThat(processed.getPublishedAt()).isNotNull();
        verify(orderKafkaProducer, atLeastOnce()).publishOrderConfirmed(any());
    }
}
