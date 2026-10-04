package com.ecommerce.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRefundedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String transactionId,
        BigDecimal amount,
        String reason,
        Instant timestamp,
        String correlationId
) {}
