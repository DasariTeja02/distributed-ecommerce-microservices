package com.ecommerce.inventory.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationFailedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String reason,
        Instant timestamp,
        String correlationId
) {}
