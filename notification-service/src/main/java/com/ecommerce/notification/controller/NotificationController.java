package com.ecommerce.notification.controller;

import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.listener.NotificationKafkaListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Customer Notifications", description = "Query dispatched email/SMS notifications and order confirmation alerts")
public class NotificationController {

    private final NotificationKafkaListener listener;

    public NotificationController(NotificationKafkaListener listener) {
        this.listener = listener;
    }

    @GetMapping
    @Operation(summary = "List all dispatched notifications", description = "Retrieves recent notification events dispatched by the notification service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispatched notifications retrieved")
    })
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        List<NotificationResponse> responses = listener.getDispatchedHistory().stream()
                .map(e -> new NotificationResponse(
                        e.eventId(),
                        e.eventType(),
                        e.orderId(),
                        e.recipient(),
                        e.channel(),
                        e.status(),
                        e.timestamp(),
                        e.correlationId()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get notifications for customer", description = "Retrieves all notifications sent to a specific customer ID/email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer notifications retrieved")
    })
    public ResponseEntity<List<NotificationResponse>> getNotificationsByCustomer(
            @Parameter(description = "Customer ID or email") @PathVariable String customerId) {
        List<NotificationResponse> responses = listener.getDispatchedHistory().stream()
                .filter(e -> customerId.equalsIgnoreCase(e.recipient()))
                .map(e -> new NotificationResponse(
                        e.eventId(),
                        e.eventType(),
                        e.orderId(),
                        e.recipient(),
                        e.channel(),
                        e.status(),
                        e.timestamp(),
                        e.correlationId()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }
}
