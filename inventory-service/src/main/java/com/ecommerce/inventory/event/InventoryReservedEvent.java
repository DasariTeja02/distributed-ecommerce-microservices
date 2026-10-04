package com.ecommerce.inventory.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        Instant timestamp,
        String correlationId
) {}
