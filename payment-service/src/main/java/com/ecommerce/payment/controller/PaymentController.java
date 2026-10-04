package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.dto.RefundPaymentRequest;
import com.ecommerce.payment.service.PaymentService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Operations", description = "Payment authorization, processing, transaction receipts, and refunds")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    @Operation(summary = "Process order payment", description = "Executes payment transaction against simulated payment gateway (also orchestrated asynchronously via Kafka).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment processed successfully (SUCCESS)"),
            @ApiResponse(responseCode = "400", description = "Payment transaction failed or declined (FAILED)")
    })
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieves payment record by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment record found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "Payment UUID") @PathVariable UUID id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Retrieves payment transaction associated with a specific order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment record found"),
            @ApiResponse(responseCode = "404", description = "No payment record for order")
    })
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @Parameter(description = "Order UUID") @PathVariable UUID orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all payments", description = "Lists all payment records across platform. Requires ROLE_ADMIN.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payments retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ROLE_ADMIN")
    })
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        List<PaymentResponse> response = paymentService.getAllPayments();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund order payment", description = "Issues full refund for an order upon Saga cancellation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund issued successfully"),
            @ApiResponse(responseCode = "400", description = "Payment not in refundable state")
    })
    public ResponseEntity<PaymentResponse> refundPayment(@Valid @RequestBody RefundPaymentRequest request) {
        PaymentResponse response = paymentService.refundPayment(request);
        return ResponseEntity.ok(response);
    }
}
