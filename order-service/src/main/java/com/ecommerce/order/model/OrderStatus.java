package com.ecommerce.order.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    CONFIRMED,
    FAILED,
    CANCELLED
}
