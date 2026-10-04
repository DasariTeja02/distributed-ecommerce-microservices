package com.ecommerce.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        String eventType,
        UUID orderId,
        String customerId,
        BigDecimal totalAmount,
        List<OrderItemDto> items,
        Instant timestamp,
        String correlationId
) {
    public record OrderItemDto(String productId, Integer quantity, BigDecimal price) {}
}
