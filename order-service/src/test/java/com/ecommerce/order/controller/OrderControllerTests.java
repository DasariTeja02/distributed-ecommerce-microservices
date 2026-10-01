package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void shouldCreateOrderAndReturn201() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("cust-202")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("prod-55")
                                .quantity(1)
                                .price(new BigDecimal("199.99"))
                                .build()
                ))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .customerId("cust-202")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("199.99"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(List.of())
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value("cust-202"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(199.99));
    }

    @Test
    void shouldReturn400WhenOrderItemsEmpty() throws Exception {
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .customerId("cust-202")
                .items(List.of()) // Empty items
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .customerId("cust-202")
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("199.99"))
                .build();

        when(orderService.getOrderById(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(orderService.getOrderById(unknownId))
                .thenThrow(new ResourceNotFoundException("Order not found with id: " + unknownId));

        mockMvc.perform(get("/api/orders/" + unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
