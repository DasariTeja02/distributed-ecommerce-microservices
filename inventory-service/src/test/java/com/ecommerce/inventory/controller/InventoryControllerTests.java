package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void shouldGetInventoryByProductId() throws Exception {
        InventoryResponse response = InventoryResponse.builder()
                .productId("prod-item-1")
                .availableQuantity(25)
                .reservedQuantity(5)
                .totalQuantity(30)
                .inStock(true)
                .build();

        when(inventoryService.getInventoryByProductId("prod-item-1")).thenReturn(response);

        mockMvc.perform(get("/api/inventory/prod-item-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("prod-item-1"))
                .andExpect(jsonPath("$.availableQuantity").value(25))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void shouldReserveInventorySuccessfully() throws Exception {
        UUID orderId = UUID.randomUUID();
        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
                .orderId(orderId)
                .items(List.of(
                        ReservationItemRequest.builder()
                                .productId("prod-item-1")
                                .quantity(2)
                                .build()
                ))
                .build();

        ReservationResponse response = ReservationResponse.builder()
                .orderId(orderId)
                .status("RESERVED")
                .message("Inventory successfully reserved")
                .build();

        when(inventoryService.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void shouldReturn409WhenInsufficientStock() throws Exception {
        UUID orderId = UUID.randomUUID();
        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
                .orderId(orderId)
                .items(List.of(
                        ReservationItemRequest.builder()
                                .productId("prod-item-1")
                                .quantity(100)
                                .build()
                ))
                .build();

        when(inventoryService.reserveInventory(any(ReserveInventoryRequest.class)))
                .thenThrow(new InsufficientStockException("Insufficient stock for product prod-item-1"));

        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product prod-item-1"));
    }
}
