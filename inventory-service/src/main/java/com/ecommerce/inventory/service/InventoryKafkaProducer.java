package com.ecommerce.inventory.service;

import com.ecommerce.inventory.event.InventoryReleasedEvent;
import com.ecommerce.inventory.event.InventoryReservationFailedEvent;
import com.ecommerce.inventory.event.InventoryReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaProducer.class);
    private static final String TOPIC_INVENTORY_EVENTS = "inventory-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryKafkaProducer(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInventoryReserved(InventoryReservedEvent event) {
        String key = event.orderId().toString();
        log.info("[Inventory Kafka Producer] Emitting InventoryReservedEvent to topic '{}', key: {}", TOPIC_INVENTORY_EVENTS, key);

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_INVENTORY_EVENTS, key, event);
            } catch (Exception e) {
                log.warn("[Inventory Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }
    }

    public void publishInventoryReservationFailed(InventoryReservationFailedEvent event) {
        String key = event.orderId().toString();
        log.info("[Inventory Kafka Producer] Emitting InventoryReservationFailedEvent to topic '{}', key: {}", TOPIC_INVENTORY_EVENTS, key);

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_INVENTORY_EVENTS, key, event);
            } catch (Exception e) {
                log.warn("[Inventory Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }
    }

    public void publishInventoryReleased(InventoryReleasedEvent event) {
        String key = event.orderId().toString();
        log.info("[Inventory Kafka Producer] Emitting InventoryReleasedEvent to topic '{}', key: {}", TOPIC_INVENTORY_EVENTS, key);

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_INVENTORY_EVENTS, key, event);
            } catch (Exception e) {
                log.warn("[Inventory Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }
    }
}
