package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.dto.RefundPaymentRequest;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse processPayment(ProcessPaymentRequest request);

    PaymentResponse getPaymentById(UUID id);

    PaymentResponse getPaymentByOrderId(UUID orderId);

    List<PaymentResponse> getPaymentsByCustomer(String customerId);

    PaymentResponse refundPayment(RefundPaymentRequest request);
}
