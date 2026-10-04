package com.ecommerce.payment.listener;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.event.PaymentFailedEvent;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.model.ProcessedEvent;
import com.ecommerce.payment.repository.ProcessedEventRepository;
import com.ecommerce.payment.service.PaymentKafkaProducer;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaListener.class);

    private final PaymentService paymentService;
    private final PaymentKafkaProducer kafkaProducer;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentKafkaListener(PaymentService paymentService,
                                PaymentKafkaProducer kafkaProducer,
                                ProcessedEventRepository processedEventRepository,
                                ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.kafkaProducer = kafkaProducer;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "inventory-events", groupId = "payment-service-group")
    public void handleInventoryEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();
            UUID orderId = UUID.fromString(root.path("orderId").asText());
            String correlationId = root.path("correlationId").asText(null);

            if (!"InventoryReservedEvent".equalsIgnoreCase(eventType)) {
                return;
            }

            // Idempotency check (Phase 11)
            if (eventId != null && !eventId.isEmpty() && processedEventRepository.existsById(eventId)) {
                log.info("[Payment Listener] Duplicate event ignored: {}", eventId);
                return;
            }

            log.info("[Payment Listener] Consumed InventoryReservedEvent. Processing financial debit for order: {}", orderId);

            BigDecimal amount = root.has("totalAmount")
                    ? new BigDecimal(root.path("totalAmount").asText())
                    : new BigDecimal("120.00");
            String customerId = root.path("customerId").asText("cust-default");
            boolean simulateFailure = root.path("simulateFailure").asBoolean(false);

            try {
                PaymentResponse response = paymentService.processPayment(ProcessPaymentRequest.builder()
                        .orderId(orderId)
                        .customerId(customerId)
                        .amount(amount)
                        .simulateFailure(simulateFailure)
                        .build());

                PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                        UUID.randomUUID().toString(),
                        "PaymentCompletedEvent",
                        orderId,
                        customerId,
                        response.getTransactionId(),
                        amount,
                        Instant.now(),
                        correlationId
                );
                kafkaProducer.publishPaymentCompleted(completedEvent);
                log.info("[Payment Listener] Payment processed successfully for order: {}", orderId);

            } catch (PaymentProcessingException ex) {
                log.warn("[Payment Listener] Payment declined for order: {}. Reason: {}", orderId, ex.getMessage());

                PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                        UUID.randomUUID().toString(),
                        "PaymentFailedEvent",
                        orderId,
                        ex.getMessage(),
                        amount,
                        Instant.now(),
                        correlationId
                );
                kafkaProducer.publishPaymentFailed(failedEvent);
            }

            // Record processed event
            if (eventId != null && !eventId.isEmpty()) {
                processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
            }

        } catch (Exception e) {
            log.error("[Payment Listener] Error processing message from inventory-events: {}", e.getMessage(), e);
        }
    }
}
