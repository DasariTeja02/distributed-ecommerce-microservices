package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.exception.ResourceNotFoundException;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.InventoryReservation;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                InventoryReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(String productId) {
        log.info("Checking inventory for product: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return mapToResponse(inventory);
    }

    @Override
    public InventoryResponse createOrUpdateInventory(InventoryRequest request) {
        log.info("Updating inventory for product: {} with quantity: {}",
                request.getProductId(), request.getAvailableQuantity());

        Instant now = Instant.now();
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseGet(() -> Inventory.builder()
                        .productId(request.getProductId())
                        .reservedQuantity(0)
                        .availableQuantity(0)
                        .build());

        inventory.setAvailableQuantity(request.getAvailableQuantity());
        inventory.setUpdatedAt(now);

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory successfully saved for product: {}, available: {}",
                saved.getProductId(), saved.getAvailableQuantity());
        return mapToResponse(saved);
    }

    @Override
    public ReservationResponse reserveInventory(ReserveInventoryRequest request) {
        UUID orderId = request.getOrderId();
        log.info("Attempting inventory reservation for order: {}", orderId);

        // Pre-check stock sufficiency across all items
        for (var item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new InsufficientStockException(
                            "Product not found in inventory: " + item.getProductId()));

            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                log.warn("Insufficient stock for product: {}. Available: {}, Requested: {}",
                        item.getProductId(), inventory.getAvailableQuantity(), item.getQuantity());
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for product %s. Available: %d, Requested: %d",
                        item.getProductId(), inventory.getAvailableQuantity(), item.getQuantity()));
            }
        }

        // Atomically reserve stock and record reservation entries
        Instant now = Instant.now();
        for (var item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).get();

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            inventory.setUpdatedAt(now);
            inventoryRepository.save(inventory);

            InventoryReservation reservation = InventoryReservation.builder()
                    .orderId(orderId)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .status("RESERVED")
                    .createdAt(now)
                    .build();
            reservationRepository.save(reservation);
        }

        log.info("Inventory successfully reserved for order: {}", orderId);
        return ReservationResponse.builder()
                .orderId(orderId)
                .status("RESERVED")
                .message("Inventory successfully reserved")
                .build();
    }

    @Override
    public ReservationResponse releaseInventory(ReleaseInventoryRequest request) {
        UUID orderId = request.getOrderId();
        log.info("Releasing reserved inventory for order: {}, reason: {}", orderId, request.getReason());

        List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, "RESERVED");

        if (reservations.isEmpty()) {
            log.warn("No active reservations found to release for order: {}", orderId);
            return ReservationResponse.builder()
                    .orderId(orderId)
                    .status("NO_ACTIVE_RESERVATION")
                    .message("No active reservation to release")
                    .build();
        }

        Instant now = Instant.now();
        for (var res : reservations) {
            inventoryRepository.findByProductId(res.getProductId()).ifPresent(inv -> {
                inv.setAvailableQuantity(inv.getAvailableQuantity() + res.getQuantity());
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - res.getQuantity()));
                inv.setUpdatedAt(now);
                inventoryRepository.save(inv);
            });

            res.setStatus("RELEASED");
            reservationRepository.save(res);
        }

        log.info("Inventory released successfully for order: {}", orderId);
        return ReservationResponse.builder()
                .orderId(orderId)
                .status("RELEASED")
                .message("Inventory successfully released")
                .build();
    }

    @Override
    public void confirmReservation(UUID orderId) {
        log.info("Confirming final inventory deduction for order: {}", orderId);
        List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, "RESERVED");

        Instant now = Instant.now();
        for (var res : reservations) {
            inventoryRepository.findByProductId(res.getProductId()).ifPresent(inv -> {
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - res.getQuantity()));
                inv.setUpdatedAt(now);
                inventoryRepository.save(inv);
            });

            res.setStatus("CONFIRMED");
            reservationRepository.save(res);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        int total = inventory.getAvailableQuantity() + inventory.getReservedQuantity();
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .totalQuantity(total)
                .inStock(inventory.getAvailableQuantity() > 0)
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
