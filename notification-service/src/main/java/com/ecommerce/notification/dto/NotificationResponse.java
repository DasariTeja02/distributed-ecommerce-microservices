package com.ecommerce.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        String eventId,
        String eventType,
        UUID orderId,
        String recipient,
        String channel,
        String status,
        Instant timestamp,
        String correlationId
) {}
