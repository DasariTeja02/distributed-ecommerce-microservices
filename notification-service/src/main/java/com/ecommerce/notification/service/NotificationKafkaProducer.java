package com.ecommerce.notification.service;

import com.ecommerce.notification.event.NotificationDispatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaProducer.class);
    private static final String TOPIC_NOTIFICATION_EVENTS = "notification-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationKafkaProducer(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishNotificationDispatched(NotificationDispatchedEvent event) {
        String key = event.orderId().toString();
        log.info("[Notification Kafka Producer] Emitting NotificationDispatchedEvent to topic '{}', key: {}",
                TOPIC_NOTIFICATION_EVENTS, key);

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_NOTIFICATION_EVENTS, key, event);
            } catch (Exception e) {
                log.warn("[Notification Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }
    }
}
