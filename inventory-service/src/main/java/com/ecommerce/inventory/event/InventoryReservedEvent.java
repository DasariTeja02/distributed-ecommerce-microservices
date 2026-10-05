package com.ecommerce.inventory.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String customerId,
        BigDecimal totalAmount,
        boolean simulateFailure,
        Instant timestamp,
        String correlationId
) {}
