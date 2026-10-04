package com.ecommerce.inventory.service;

import com.ecommerce.inventory.event.InventoryReleasedEvent;
import com.ecommerce.inventory.event.InventoryReservationFailedEvent;
import com.ecommerce.inventory.event.InventoryReservedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class InventoryKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaProducer.class);
    private static final String TOPIC_INVENTORY_EVENTS = "inventory-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryKafkaProducer(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private void sendWithCorrelationHeader(String topic, String key, Object payload, String correlationId) {
        if (kafkaTemplate != null) {
            try {
                ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
                if (correlationId != null && !correlationId.isBlank()) {
                    record.headers().add("X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8));
                }
                kafkaTemplate.send(record);
            } catch (Exception e) {
                log.warn("[Inventory Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }
    }

    public void publishInventoryReserved(InventoryReservedEvent event) {
        String key = event.orderId().toString();
        log.info("[Inventory Kafka Producer] Emitting InventoryReservedEvent to topic '{}', key: {}, correlationId: {}",
                TOPIC_INVENTORY_EVENTS, key, event.correlationId());
        sendWithCorrelationHeader(TOPIC_INVENTORY_EVENTS, key, event, event.correlationId());
    }

    public void publishInventoryReservationFailed(InventoryReservationFailedEvent event) {
        String key = event.orderId().toString();
        log.info("[Inventory Kafka Producer] Emitting InventoryReservationFailedEvent to topic '{}', key: {}, correlationId: {}",
                TOPIC_INVENTORY_EVENTS, key, event.correlationId());
        sendWithCorrelationHeader(TOPIC_INVENTORY_EVENTS, key, event, event.correlationId());
    }

    public void publishInventoryReleased(InventoryReleasedEvent event) {
        String key = event.orderId().toString();
        log.info("[Inventory Kafka Producer] Emitting InventoryReleasedEvent to topic '{}', key: {}, correlationId: {}",
                TOPIC_INVENTORY_EVENTS, key, event.correlationId());
        sendWithCorrelationHeader(TOPIC_INVENTORY_EVENTS, key, event, event.correlationId());
    }
}
