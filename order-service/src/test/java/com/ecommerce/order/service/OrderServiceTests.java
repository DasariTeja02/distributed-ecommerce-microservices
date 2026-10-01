package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        order = Order.builder()
                .id(orderId)
                .customerId("cust-101")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("299.98"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void shouldCreateOrderAndCalculateTotalAmount() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-101")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("prod-1")
                                .quantity(2)
                                .price(new BigDecimal("100.00"))
                                .build(),
                        OrderItemRequest.builder()
                                .productId("prod-2")
                                .quantity(1)
                                .price(new BigDecimal("99.98"))
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("cust-101");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("299.98"));
        assertThat(response.getItems()).hasSize(2);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void shouldGetOrderById() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getCustomerId()).isEqualTo("cust-101");
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: " + unknownId);
    }

    @Test
    void shouldUpdateOrderStatusValidly() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.INVENTORY_RESERVED)
                .build();

        OrderResponse response = orderService.updateOrderStatus(orderId, request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
    }

    @Test
    void shouldRejectInvalidOrderStateTransition() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Attempting to jump directly from PENDING to CONFIRMED should fail
        UpdateOrderStatusRequest invalidRequest = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, invalidRequest))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Invalid order state transition from PENDING to CONFIRMED");
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, "Customer requested cancellation");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getFailureReason()).isEqualTo("Customer requested cancellation");
    }
}
