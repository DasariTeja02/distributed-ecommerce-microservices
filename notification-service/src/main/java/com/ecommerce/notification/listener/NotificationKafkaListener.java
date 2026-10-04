package com.ecommerce.notification.listener;

import com.ecommerce.notification.event.NotificationDispatchedEvent;
import com.ecommerce.notification.service.NotificationKafkaProducer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class NotificationKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaListener.class);

    private final NotificationKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;
    private final List<NotificationDispatchedEvent> dispatchedHistory = new CopyOnWriteArrayList<>();

    public NotificationKafkaListener(NotificationKafkaProducer kafkaProducer, ObjectMapper objectMapper) {
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
    }

    public List<NotificationDispatchedEvent> getDispatchedHistory() {
        return List.copyOf(dispatchedHistory);
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void handleOrderEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            UUID orderId = UUID.fromString(root.path("orderId").asText());
            String customerId = root.path("customerId").asText("customer@ecommerce.io");
            String correlationId = root.path("correlationId").asText(null);

            if ("OrderConfirmedEvent".equalsIgnoreCase(eventType)) {
                log.info("📧 [NOTIFICATION DISPATCH - EMAIL/SMS] Order {} is CONFIRMED! Sending receipt to customer: {}",
                        orderId, customerId);

                NotificationDispatchedEvent dispatchedEvent = new NotificationDispatchedEvent(
                        UUID.randomUUID().toString(),
                        "NotificationDispatchedEvent",
                        orderId,
                        customerId,
                        "EMAIL",
                        "SENT",
                        Instant.now(),
                        correlationId
                );
                dispatchedHistory.add(dispatchedEvent);
                kafkaProducer.publishNotificationDispatched(dispatchedEvent);

            } else if ("OrderCancelledEvent".equalsIgnoreCase(eventType)) {
                String reason = root.path("reason").asText("Processing failure");
                log.warn("📧 [NOTIFICATION DISPATCH - ALERT] Order {} was CANCELLED. Reason: {}. Notifying customer: {}",
                        orderId, reason, customerId);

                NotificationDispatchedEvent dispatchedEvent = new NotificationDispatchedEvent(
                        UUID.randomUUID().toString(),
                        "NotificationDispatchedEvent",
                        orderId,
                        customerId,
                        "EMAIL",
                        "SENT_ALERT",
                        Instant.now(),
                        correlationId
                );
                dispatchedHistory.add(dispatchedEvent);
                kafkaProducer.publishNotificationDispatched(dispatchedEvent);
            }

        } catch (Exception e) {
            log.error("[Notification Listener] Error handling order event: {}", e.getMessage(), e);
        }
    }
}
