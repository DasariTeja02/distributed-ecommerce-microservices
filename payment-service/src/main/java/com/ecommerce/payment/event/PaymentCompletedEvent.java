package com.ecommerce.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String customerId,
        String transactionId,
        BigDecimal amount,
        Instant timestamp,
        String correlationId
) {}
