package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    InventoryResponse getInventoryByProductId(String productId);

    InventoryResponse createOrUpdateInventory(InventoryRequest request);

    ReservationResponse reserveInventory(ReserveInventoryRequest request);

    ReservationResponse releaseInventory(ReleaseInventoryRequest request);

    void confirmReservation(UUID orderId);

    List<InventoryResponse> getAllInventory();
}
