package com.ecommerce.order.listener;

import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderConfirmedEvent;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.model.ProcessedEvent;
import com.ecommerce.order.repository.ProcessedEventRepository;
import com.ecommerce.order.service.OrderKafkaProducer;
import com.ecommerce.order.service.OrderSagaEventBroadcaster;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderSagaKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaKafkaListener.class);

    private final OrderService orderService;
    private final OrderKafkaProducer kafkaProducer;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderSagaEventBroadcaster broadcaster;
    private final ObjectMapper objectMapper;

    public OrderSagaKafkaListener(OrderService orderService,
                                  OrderKafkaProducer kafkaProducer,
                                  ProcessedEventRepository processedEventRepository,
                                  OrderSagaEventBroadcaster broadcaster,
                                  ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.kafkaProducer = kafkaProducer;
        this.processedEventRepository = processedEventRepository;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void handlePaymentEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();
            UUID orderId = UUID.fromString(root.path("orderId").asText());
            String correlationId = root.path("correlationId").asText(null);

            // Idempotency check (Phase 11)
            if (eventId != null && !eventId.isEmpty() && processedEventRepository.existsById(eventId)) {
                log.info("[Order Listener] Duplicate payment event ignored: {}", eventId);
                return;
            }

            log.info("[Order Listener] Consumed event '{}' for orderId: {}", eventType, orderId);

            if ("PaymentCompletedEvent".equalsIgnoreCase(eventType)) {
                // Happy path: Update order status to CONFIRMED
                orderService.updateOrderStatus(orderId, UpdateOrderStatusRequest.builder()
                        .status(OrderStatus.CONFIRMED)
                        .build());

                OrderConfirmedEvent confirmedEvent = new OrderConfirmedEvent(
                        UUID.randomUUID().toString(),
                        "OrderConfirmedEvent",
                        orderId,
                        root.path("customerId").asText("anonymous"),
                        Instant.now(),
                        correlationId
                );
                kafkaProducer.publishOrderConfirmed(confirmedEvent);

            } else if ("PaymentFailedEvent".equalsIgnoreCase(eventType)) {
                // Compensation path: Update order status to CANCELLED
                String reason = root.path("failureReason").asText("Payment declined by payment gateway");
                orderService.cancelOrder(orderId, reason);

                OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                        UUID.randomUUID().toString(),
                        "OrderCancelledEvent",
                        orderId,
                        root.path("customerId").asText("anonymous"),
                        reason,
                        Instant.now(),
                        correlationId
                );
                kafkaProducer.publishOrderCancelled(cancelledEvent);
            }

            // Record processed event
            if (eventId != null && !eventId.isEmpty()) {
                processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
            }

        } catch (Exception e) {
            log.error("[Order Listener] Error processing message from payment-events: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    public void handleInventoryEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();
            UUID orderId = UUID.fromString(root.path("orderId").asText());
            String correlationId = root.path("correlationId").asText(null);

            // Forward to broadcaster so frontend UI sees InventoryReservedEvent or InventoryReservationFailedEvent
            broadcaster.broadcast(eventType, Map.of(
                    "topic", "inventory-events",
                    "eventType", eventType,
                    "orderId", orderId.toString(),
                    "timestamp", Instant.now().toString(),
                    "correlationId", correlationId != null ? correlationId : "N/A"
            ));

            if ("InventoryReservedEvent".equalsIgnoreCase(eventType)) {
                if (eventId != null && !eventId.isEmpty() && processedEventRepository.existsById(eventId)) {
                    return;
                }
                log.info("[Order Listener] Inventory reserved for order {}. Updating status to INVENTORY_RESERVED", orderId);
                orderService.updateOrderStatus(orderId, UpdateOrderStatusRequest.builder()
                        .status(OrderStatus.INVENTORY_RESERVED)
                        .build());

                if (eventId != null && !eventId.isEmpty()) {
                    processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
                }
            } else if ("InventoryReservationFailedEvent".equalsIgnoreCase(eventType)) {
                // Idempotency check
                if (eventId != null && !eventId.isEmpty() && processedEventRepository.existsById(eventId)) {
                    return;
                }

                String reason = root.path("reason").asText("Insufficient stock to fulfill order");
                log.warn("[Order Listener] Inventory reservation failed for order {}. Cancelling order.", orderId);
                orderService.cancelOrder(orderId, reason);

                OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                        UUID.randomUUID().toString(),
                        "OrderCancelledEvent",
                        orderId,
                        "anonymous",
                        reason,
                        Instant.now(),
                        correlationId
                );
                kafkaProducer.publishOrderCancelled(cancelledEvent);

                if (eventId != null && !eventId.isEmpty()) {
                    processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
                }
            }

        } catch (Exception e) {
            log.error("[Order Listener] Error processing message from inventory-events: {}", e.getMessage(), e);
        }
    }
}
