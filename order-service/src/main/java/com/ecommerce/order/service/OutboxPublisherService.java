package com.ecommerce.order.service;

import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderConfirmedEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.model.OutboxEvent;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                  OrderKafkaProducer kafkaProducer,
                                  ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Periodically polls the outbox table for unpublished events and publishes them to Kafka.
     * Retries automatically if Kafka was temporarily unavailable.
     */
    @Scheduled(fixedDelay = 2000)
    public void processOutbox() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[Outbox Publisher] Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            publishSingleEvent(outboxEvent);
        }
    }

    public void publishSingleEvent(OutboxEvent outboxEvent) {
        try {
            switch (outboxEvent.getEventType()) {
                case "OrderCreatedEvent" -> {
                    OrderCreatedEvent event = objectMapper.readValue(outboxEvent.getPayload(), OrderCreatedEvent.class);
                    kafkaProducer.publishOrderCreated(event);
                }
                case "OrderConfirmedEvent" -> {
                    OrderConfirmedEvent event = objectMapper.readValue(outboxEvent.getPayload(), OrderConfirmedEvent.class);
                    kafkaProducer.publishOrderConfirmed(event);
                }
                case "OrderCancelledEvent" -> {
                    OrderCancelledEvent event = objectMapper.readValue(outboxEvent.getPayload(), OrderCancelledEvent.class);
                    kafkaProducer.publishOrderCancelled(event);
                }
                default -> log.warn("[Outbox Publisher] Unknown event type: {}", outboxEvent.getEventType());
            }

            outboxEvent.setPublished(true);
            outboxEvent.setPublishedAt(Instant.now());
            outboxEvent.setLastError(null);
            outboxEventRepository.save(outboxEvent);
            log.info("[Outbox Publisher] Successfully published outbox event: {} (Type: {})",
                    outboxEvent.getId(), outboxEvent.getEventType());

        } catch (Exception e) {
            log.error("[Outbox Publisher] Failed to publish outbox event {}: {}", outboxEvent.getId(), e.getMessage());
            outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
            outboxEvent.setLastError(e.getMessage() != null && e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000)
                    : e.getMessage());
            outboxEventRepository.save(outboxEvent);
        }
    }
}
