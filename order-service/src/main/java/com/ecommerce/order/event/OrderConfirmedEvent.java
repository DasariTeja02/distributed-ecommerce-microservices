package com.ecommerce.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String customerId,
        Instant timestamp,
        String correlationId
) {}
