package com.ecommerce.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String failureReason,
        BigDecimal amount,
        Instant timestamp,
        String correlationId
) {}
