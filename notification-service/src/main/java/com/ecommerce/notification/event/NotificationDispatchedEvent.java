package com.ecommerce.notification.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationDispatchedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String recipient,
        String channel,
        String status,
        Instant timestamp,
        String correlationId
) {}
