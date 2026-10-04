package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "Stock tracking, inventory reservation, and compensation release endpoints")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory for product", description = "Returns available and reserved stock for a specific productId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory record found"),
            @ApiResponse(responseCode = "404", description = "No inventory found for specified product")
    })
    public ResponseEntity<InventoryResponse> getInventoryByProductId(
            @Parameter(description = "Product ID to query") @PathVariable String productId) {
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all inventory", description = "Retrieves all stock and reservation levels across the warehouse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory records returned")
    })
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        List<InventoryResponse> response = inventoryService.getAllInventory();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create or update inventory stock", description = "Sets or increases available quantity for a product. Requires ROLE_ADMIN.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventory record created or updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_ADMIN")
    })
    public ResponseEntity<InventoryResponse> createOrUpdateInventory(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.createOrUpdateInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory items", description = "Direct HTTP endpoint for reserving stock for order items (also orchestrated asynchronously via Kafka).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation completed successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient inventory stock")
    })
    public ResponseEntity<ReservationResponse> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        ReservationResponse response = inventoryService.reserveInventory(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    @Operation(summary = "Release reserved inventory", description = "Compensating action releasing previously reserved stock back to available pool upon Saga cancellation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation released successfully")
    })
    public ResponseEntity<ReservationResponse> releaseInventory(@Valid @RequestBody ReleaseInventoryRequest request) {
        ReservationResponse response = inventoryService.releaseInventory(request);
        return ResponseEntity.ok(response);
    }
}
