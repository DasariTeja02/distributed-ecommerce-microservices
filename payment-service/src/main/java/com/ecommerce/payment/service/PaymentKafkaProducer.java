package com.ecommerce.payment.service;

import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.event.PaymentFailedEvent;
import com.ecommerce.payment.event.PaymentRefundedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class PaymentKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaProducer.class);
    private static final String TOPIC_PAYMENT_EVENTS = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentKafkaProducer(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
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
                log.warn("[Payment Kafka Producer] Failed to send to broker, logging locally: {}", e.getMessage());
            }
        }
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        String key = event.orderId().toString();
        log.info("[Payment Kafka Producer] Emitting PaymentCompletedEvent to topic '{}', key: {}, correlationId: {}",
                TOPIC_PAYMENT_EVENTS, key, event.correlationId());
        sendWithCorrelationHeader(TOPIC_PAYMENT_EVENTS, key, event, event.correlationId());
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        String key = event.orderId().toString();
        log.info("[Payment Kafka Producer] Emitting PaymentFailedEvent to topic '{}', key: {}, correlationId: {}",
                TOPIC_PAYMENT_EVENTS, key, event.correlationId());
        sendWithCorrelationHeader(TOPIC_PAYMENT_EVENTS, key, event, event.correlationId());
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        String key = event.orderId().toString();
        log.info("[Payment Kafka Producer] Emitting PaymentRefundedEvent to topic '{}', key: {}, correlationId: {}",
                TOPIC_PAYMENT_EVENTS, key, event.correlationId());
        sendWithCorrelationHeader(TOPIC_PAYMENT_EVENTS, key, event, event.correlationId());
    }
}
