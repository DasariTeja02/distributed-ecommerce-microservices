package com.ecommerce.inventory.listener;

import com.ecommerce.inventory.dto.ReleaseInventoryRequest;
import com.ecommerce.inventory.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.event.InventoryReleasedEvent;
import com.ecommerce.inventory.event.InventoryReservationFailedEvent;
import com.ecommerce.inventory.event.InventoryReservedEvent;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.model.ProcessedEvent;
import com.ecommerce.inventory.repository.ProcessedEventRepository;
import com.ecommerce.inventory.service.InventoryKafkaProducer;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class InventoryKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaListener.class);

    private final InventoryService inventoryService;
    private final InventoryKafkaProducer kafkaProducer;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public InventoryKafkaListener(InventoryService inventoryService,
                                  InventoryKafkaProducer kafkaProducer,
                                  ProcessedEventRepository processedEventRepository,
                                  ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.kafkaProducer = kafkaProducer;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void handleOrderEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();
            UUID orderId = UUID.fromString(root.path("orderId").asText());
            String correlationId = root.path("correlationId").asText(null);

            if (!"OrderCreatedEvent".equalsIgnoreCase(eventType)) {
                return;
            }

            // Idempotency check (Phase 11)
            if (eventId != null && !eventId.isEmpty() && processedEventRepository.existsById(eventId)) {
                log.info("[Inventory Listener] Duplicate event ignored: {}", eventId);
                return;
            }

            log.info("[Inventory Listener] Reserving inventory for orderId: {}", orderId);

            List<com.ecommerce.inventory.dto.ReservationItemRequest> items = new ArrayList<>();
            JsonNode itemsNode = root.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    items.add(com.ecommerce.inventory.dto.ReservationItemRequest.builder()
                            .productId(item.path("productId").asText())
                            .quantity(item.path("quantity").asInt())
                            .build());
                }
            }

            try {
                inventoryService.reserveInventory(ReserveInventoryRequest.builder()
                        .orderId(orderId)
                        .items(items)
                        .build());

                InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                        UUID.randomUUID().toString(),
                        "InventoryReservedEvent",
                        orderId,
                        Instant.now(),
                        correlationId
                );
                kafkaProducer.publishInventoryReserved(reservedEvent);
                log.info("[Inventory Listener] Stock successfully reserved for order: {}", orderId);

            } catch (InsufficientStockException ex) {
                log.warn("[Inventory Listener] Insufficient stock for order: {}. Reason: {}", orderId, ex.getMessage());

                InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                        UUID.randomUUID().toString(),
                        "InventoryReservationFailedEvent",
                        orderId,
                        ex.getMessage(),
                        Instant.now(),
                        correlationId
                );
                kafkaProducer.publishInventoryReservationFailed(failedEvent);
            }

            // Record processed event
            if (eventId != null && !eventId.isEmpty()) {
                processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
            }

        } catch (Exception e) {
            log.error("[Inventory Listener] Error processing order event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "inventory-service-compensation-group")
    public void handlePaymentEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();
            UUID orderId = UUID.fromString(root.path("orderId").asText());
            String correlationId = root.path("correlationId").asText(null);

            if (!"PaymentFailedEvent".equalsIgnoreCase(eventType)) {
                return;
            }

            // Idempotency check (Phase 11)
            String compKey = "comp-" + eventId;
            if (processedEventRepository.existsById(compKey)) {
                log.info("[Inventory Listener] Duplicate compensation ignored: {}", compKey);
                return;
            }

            log.warn("[Inventory Listener] ⚠️ SAGA COMPENSATION: Payment failed for order {}. Releasing reserved stock.", orderId);

            String reason = root.path("failureReason").asText("Payment declined");
            inventoryService.releaseInventory(ReleaseInventoryRequest.builder()
                    .orderId(orderId)
                    .reason(reason)
                    .build());

            InventoryReleasedEvent releasedEvent = new InventoryReleasedEvent(
                    UUID.randomUUID().toString(),
                    "InventoryReleasedEvent",
                    orderId,
                    reason,
                    Instant.now(),
                    correlationId
            );
            kafkaProducer.publishInventoryReleased(releasedEvent);
            log.info("[Inventory Listener] Successfully released reserved inventory for order: {}", orderId);

            processedEventRepository.save(new ProcessedEvent(compKey, "InventoryReleasedCompensation", Instant.now()));

        } catch (Exception e) {
            log.error("[Inventory Listener] Error executing compensation for order: {}", e.getMessage(), e);
        }
    }
}
