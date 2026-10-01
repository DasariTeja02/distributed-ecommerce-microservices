package com.ecommerce.payment.dto;

import com.ecommerce.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private String customerId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
