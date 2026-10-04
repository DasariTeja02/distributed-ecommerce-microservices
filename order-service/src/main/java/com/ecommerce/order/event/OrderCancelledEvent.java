package com.ecommerce.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String customerId,
        String reason,
        Instant timestamp,
        String correlationId
) {}
