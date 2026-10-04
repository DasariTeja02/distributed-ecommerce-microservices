package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.dto.RefundPaymentRequest;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.exception.ResourceNotFoundException;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final BigDecimal SIMULATED_DECLINE_AMOUNT = new BigDecimal("9999.99");

    private final PaymentRepository paymentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        UUID orderId = request.getOrderId();
        log.info("Processing payment for order: {}, amount: {}", orderId, request.getAmount());

        // Idempotency check: if payment already processed for this order, return existing
        var existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            log.info("Payment already exists for order: {}, returning recorded status: {}",
                    orderId, existingPayment.get().getStatus());
            return mapToResponse(existingPayment.get());
        }

        Instant now = Instant.now();
        UUID paymentId = UUID.randomUUID();

        // Check if failure simulation is triggered
        boolean isFailure = request.isSimulateFailure() ||
                (request.getAmount() != null && request.getAmount().compareTo(SIMULATED_DECLINE_AMOUNT) == 0);

        if (isFailure) {
            String failureReason = "Payment authorization declined: Insufficient funds or simulated card decline";
            log.warn("Payment failed for order: {}. Reason: {}", orderId, failureReason);

            Payment failedPayment = Payment.builder()
                    .id(paymentId)
                    .orderId(orderId)
                    .customerId(request.getCustomerId())
                    .amount(request.getAmount())
                    .status(PaymentStatus.FAILED)
                    .failureReason(failureReason)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            paymentRepository.save(failedPayment);
            if (meterRegistry != null) {
                meterRegistry.counter("payment.failure").increment();
            }
            throw new PaymentProcessingException(failureReason);
        }

        String transactionId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Payment saved = paymentRepository.save(payment);
        if (meterRegistry != null) {
            meterRegistry.counter("payment.success").increment();
        }
        log.info("Payment successful for order: {}, txn: {}", orderId, saved.getTransactionId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        log.info("Fetching payment with id: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        log.info("Fetching payment for order: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCustomer(String customerId) {
        log.info("Fetching payments for customer: {}", customerId);
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        log.info("Fetching all payments");
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse refundPayment(RefundPaymentRequest request) {
        UUID orderId = request.getOrderId();
        log.info("Initiating refund for order: {}, reason: {}", orderId, request.getReason());

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment for order {} is already refunded", orderId);
            return mapToResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentProcessingException(
                    "Cannot refund payment with status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason("Refunded: " + (request.getReason() != null ? request.getReason() : "Order cancellation"));
        payment.setUpdatedAt(Instant.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Payment refunded successfully for order: {}", orderId);
        return mapToResponse(saved);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
