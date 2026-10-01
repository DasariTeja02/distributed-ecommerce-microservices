package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.dto.RefundPaymentRequest;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID orderId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId("cust-909")
                .amount(new BigDecimal("150.00"))
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-123456789")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId("cust-909")
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId()).startsWith("TXN-");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void shouldFailPaymentWhenSimulationFlagRaised() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId("cust-909")
                .amount(new BigDecimal("150.00"))
                .simulateFailure(true)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Payment authorization declined");

        verify(paymentRepository, times(1)).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
    }

    @Test
    void shouldRefundPaymentSuccessfully() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundPaymentRequest request = RefundPaymentRequest.builder()
                .orderId(orderId)
                .reason("Customer cancelled order")
                .build();

        PaymentResponse response = paymentService.refundPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.getFailureReason()).contains("Customer cancelled order");
    }

    @Test
    void shouldReturnExistingPaymentWhenAlreadyProcessed() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId("cust-909")
                .amount(new BigDecimal("150.00"))
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
