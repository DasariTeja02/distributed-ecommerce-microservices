package com.ecommerce.order.service;

import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderConfirmedEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaProducer.class);
    private static final String TOPIC_ORDER_EVENTS = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderSagaEventBroadcaster broadcaster;

    public OrderKafkaProducer(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
                              OrderSagaEventBroadcaster broadcaster) {
        this.kafkaTemplate = kafkaTemplate;
        this.broadcaster = broadcaster;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        String key = event.orderId().toString();
        log.info("[Kafka Producer] Emitting OrderCreatedEvent to topic '{}', key: {}", TOPIC_ORDER_EVENTS, key);

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_ORDER_EVENTS, key, event);
            } catch (Exception e) {
                log.warn("[Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }

        // Broadcast to SSE stream for live UI visualizer
        broadcaster.broadcast("OrderCreatedEvent", Map.of(
                "topic", TOPIC_ORDER_EVENTS,
                "key", key,
                "eventType", "OrderCreatedEvent",
                "orderId", event.orderId().toString(),
                "status", "PENDING",
                "totalAmount", event.totalAmount(),
                "timestamp", event.timestamp().toString(),
                "correlationId", event.correlationId() != null ? event.correlationId() : "N/A"
        ));
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        String key = event.orderId().toString();
        log.info("[Kafka Producer] Emitting OrderConfirmedEvent to topic '{}', key: {}", TOPIC_ORDER_EVENTS, key);

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_ORDER_EVENTS, key, event);
            } catch (Exception e) {
                log.warn("[Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }

        broadcaster.broadcast("OrderConfirmedEvent", Map.of(
                "topic", TOPIC_ORDER_EVENTS,
                "key", key,
                "eventType", "OrderConfirmedEvent",
                "orderId", event.orderId().toString(),
                "status", "CONFIRMED",
                "timestamp", event.timestamp().toString(),
                "correlationId", event.correlationId() != null ? event.correlationId() : "N/A"
        ));
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        String key = event.orderId().toString();
        log.info("[Kafka Producer] Emitting OrderCancelledEvent to topic '{}', key: {}", TOPIC_ORDER_EVENTS, key);

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_ORDER_EVENTS, key, event);
            } catch (Exception e) {
                log.warn("[Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }

        broadcaster.broadcast("OrderCancelledEvent", Map.of(
                "topic", TOPIC_ORDER_EVENTS,
                "key", key,
                "eventType", "OrderCancelledEvent",
                "orderId", event.orderId().toString(),
                "status", "CANCELLED",
                "reason", event.reason() != null ? event.reason() : "N/A",
                "timestamp", event.timestamp().toString(),
                "correlationId", event.correlationId() != null ? event.correlationId() : "N/A"
        ));
    }
}
