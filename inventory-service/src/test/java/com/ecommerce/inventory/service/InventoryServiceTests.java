package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.exception.ResourceNotFoundException;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.InventoryReservation;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTests {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(1L)
                .productId("prod-iphone15")
                .availableQuantity(50)
                .reservedQuantity(0)
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldGetInventoryByProductId() {
        when(inventoryRepository.findByProductId("prod-iphone15")).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.getInventoryByProductId("prod-iphone15");

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("prod-iphone15");
        assertThat(response.getAvailableQuantity()).isEqualTo(50);
        assertThat(response.isInStock()).isTrue();
    }

    @Test
    void shouldThrowWhenProductNotFoundInInventory() {
        when(inventoryRepository.findByProductId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryByProductId("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReserveInventorySuccessfully() {
        UUID orderId = UUID.randomUUID();
        when(inventoryRepository.findByProductId("prod-iphone15")).thenReturn(Optional.of(inventory));

        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
                .orderId(orderId)
                .items(List.of(
                        ReservationItemRequest.builder()
                                .productId("prod-iphone15")
                                .quantity(5)
                                .build()
                ))
                .build();

        ReservationResponse response = inventoryService.reserveInventory(request);

        assertThat(response.getStatus()).isEqualTo("RESERVED");
        assertThat(inventory.getAvailableQuantity()).isEqualTo(45);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(reservationRepository, times(1)).save(any(InventoryReservation.class));
    }

    @Test
    void shouldRejectReservationWhenStockInsufficient() {
        UUID orderId = UUID.randomUUID();
        when(inventoryRepository.findByProductId("prod-iphone15")).thenReturn(Optional.of(inventory));

        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
                .orderId(orderId)
                .items(List.of(
                        ReservationItemRequest.builder()
                                .productId("prod-iphone15")
                                .quantity(100) // Exceeds 50
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> inventoryService.reserveInventory(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product prod-iphone15");
    }

    @Test
    void shouldReleaseReservedInventorySuccessfully() {
        UUID orderId = UUID.randomUUID();
        inventory.setAvailableQuantity(40);
        inventory.setReservedQuantity(10);

        InventoryReservation reservation = InventoryReservation.builder()
                .id(1L)
                .orderId(orderId)
                .productId("prod-iphone15")
                .quantity(10)
                .status("RESERVED")
                .createdAt(Instant.now())
                .build();

        when(reservationRepository.findByOrderIdAndStatus(orderId, "RESERVED")).thenReturn(List.of(reservation));
        when(inventoryRepository.findByProductId("prod-iphone15")).thenReturn(Optional.of(inventory));

        ReleaseInventoryRequest request = ReleaseInventoryRequest.builder()
                .orderId(orderId)
                .reason("Payment failure rollback")
                .build();

        ReservationResponse response = inventoryService.releaseInventory(request);

        assertThat(response.getStatus()).isEqualTo("RELEASED");
        assertThat(inventory.getAvailableQuantity()).isEqualTo(50);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo("RELEASED");
    }
}
