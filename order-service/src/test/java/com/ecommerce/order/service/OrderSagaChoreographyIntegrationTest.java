package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.listener.OrderSagaKafkaListener;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.model.ProcessedEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:order_saga_db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
public class OrderSagaChoreographyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OrderSagaKafkaListener orderSagaKafkaListener;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderKafkaProducer orderKafkaProducer;

    @Test
    @DisplayName("Saga Scenario 1: Happy Path - PaymentCompletedEvent transitions order to CONFIRMED and publishes OrderConfirmedEvent")
    void testHappyPathChoreography() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-happy-1")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-TEST-1")
                                .quantity(2)
                                .price(new BigDecimal("49.99"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();
        assertThat(orderResponse.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Step 1: InventoryReservedEvent transitions order to INVENTORY_RESERVED
        String invEventId = "evt-inv-res-" + UUID.randomUUID();
        Map<String, Object> inventoryReserved = Map.of(
                "eventId", invEventId,
                "eventType", "InventoryReservedEvent",
                "orderId", orderId.toString(),
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-happy"
        );
        orderSagaKafkaListener.handleInventoryEvents(objectMapper.writeValueAsString(inventoryReserved));
        assertThat(orderRepository.findById(orderId).get().getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);

        // Step 2: PaymentCompletedEvent transitions order to CONFIRMED
        String eventId = "evt-pay-comp-" + UUID.randomUUID();
        Map<String, Object> paymentCompleted = Map.of(
                "eventId", eventId,
                "eventType", "PaymentCompletedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-happy-1",
                "transactionId", "TXN-OK-101",
                "amount", 99.98,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-happy"
        );

        orderSagaKafkaListener.handlePaymentEvents(objectMapper.writeValueAsString(paymentCompleted));

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
        verify(orderKafkaProducer, atLeastOnce()).publishOrderConfirmed(any());
    }

    @Test
    @DisplayName("Saga Scenario 2: Payment Failure - PaymentFailedEvent transitions order to CANCELLED and publishes OrderCancelledEvent")
    void testPaymentFailureRollback() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-fail-1")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-TEST-2")
                                .quantity(1)
                                .price(new BigDecimal("79.99"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();

        String eventId = "evt-pay-fail-" + UUID.randomUUID();
        Map<String, Object> paymentFailed = Map.of(
                "eventId", eventId,
                "eventType", "PaymentFailedEvent",
                "orderId", orderId.toString(),
                "failureReason", "Card expired",
                "amount", 79.99,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-fail"
        );

        orderSagaKafkaListener.handlePaymentEvents(objectMapper.writeValueAsString(paymentFailed));

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
        verify(orderKafkaProducer, atLeastOnce()).publishOrderCancelled(any());
    }

    @Test
    @DisplayName("Saga Scenario 3: Inventory Failure - InventoryReservationFailedEvent cancels order")
    void testInventoryReservationFailed() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-inv-fail")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-OOS")
                                .quantity(1)
                                .price(new BigDecimal("1200.00"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();

        String eventId = "evt-inv-oos-" + UUID.randomUUID();
        Map<String, Object> invFailed = Map.of(
                "eventId", eventId,
                "eventType", "InventoryReservationFailedEvent",
                "orderId", orderId.toString(),
                "reason", "Inventory out of stock",
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-oos"
        );

        orderSagaKafkaListener.handleInventoryEvents(objectMapper.writeValueAsString(invFailed));

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
        verify(orderKafkaProducer, atLeastOnce()).publishOrderCancelled(any());
    }

    @Test
    @DisplayName("Saga Scenario 4: Duplicate Event - ProcessedEvent table guarantees single execution")
    void testDuplicateEventSafelyIgnored() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-dup")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD-DUP")
                                .quantity(1)
                                .price(new BigDecimal("35.00"))
                                .build()
                ))
                .build();

        OrderResponse orderResponse = orderService.createOrder(request);
        UUID orderId = orderResponse.getId();

        // InventoryReservedEvent transitions to INVENTORY_RESERVED
        String invEventId = "evt-inv-dup-" + UUID.randomUUID();
        Map<String, Object> inventoryReserved = Map.of(
                "eventId", invEventId,
                "eventType", "InventoryReservedEvent",
                "orderId", orderId.toString(),
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-dup"
        );
        orderSagaKafkaListener.handleInventoryEvents(objectMapper.writeValueAsString(inventoryReserved));

        String eventId = "evt-dup-fixed-id-100";
        Map<String, Object> paymentCompleted = Map.of(
                "eventId", eventId,
                "eventType", "PaymentCompletedEvent",
                "orderId", orderId.toString(),
                "customerId", "cust-dup",
                "transactionId", "TXN-DUP-100",
                "amount", 35.00,
                "timestamp", Instant.now().toString(),
                "correlationId", "corr-dup"
        );

        String json = objectMapper.writeValueAsString(paymentCompleted);

        // First execution
        orderSagaKafkaListener.handlePaymentEvents(json);
        assertThat(orderRepository.findById(orderId).get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        long count1 = processedEventRepository.count();

        // Second execution (replay)
        orderSagaKafkaListener.handlePaymentEvents(json);
        long count2 = processedEventRepository.count();

        assertThat(count2).isEqualTo(count1);
        assertThat(orderRepository.findById(orderId).get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
