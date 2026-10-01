package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(UUID id);

    List<OrderResponse> getOrdersByCustomer(String customerId);

    List<OrderResponse> getAllOrders(OrderStatus status);

    OrderResponse updateOrderStatus(UUID id, UpdateOrderStatusRequest request);

    OrderResponse cancelOrder(UUID id, String reason);
}
