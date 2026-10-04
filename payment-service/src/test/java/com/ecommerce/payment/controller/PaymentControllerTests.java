package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.ProcessPaymentRequest;
import com.ecommerce.payment.dto.RefundPaymentRequest;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void shouldProcessPaymentAndReturn201() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId("cust-333")
                .amount(new BigDecimal("120.50"))
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(paymentId)
                .orderId(orderId)
                .customerId("cust-333")
                .amount(new BigDecimal("120.50"))
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-1234")
                .createdAt(Instant.now())
                .build();

        when(paymentService.processPayment(any(ProcessPaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(120.50));
    }

    @Test
    void shouldReturn402WhenPaymentProcessingFails() throws Exception {
        UUID orderId = UUID.randomUUID();
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId("cust-333")
                .amount(new BigDecimal("120.50"))
                .simulateFailure(true)
                .build();

        when(paymentService.processPayment(any(ProcessPaymentRequest.class)))
                .thenThrow(new PaymentProcessingException("Payment authorization declined"));

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.message").value("Payment authorization declined"));
    }

    @Test
    void shouldRefundPaymentAndReturn200() throws Exception {
        UUID orderId = UUID.randomUUID();
        RefundPaymentRequest request = RefundPaymentRequest.builder()
                .orderId(orderId)
                .reason("Customer requested")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .orderId(orderId)
                .status(PaymentStatus.REFUNDED)
                .failureReason("Refunded: Customer requested")
                .build();

        when(paymentService.refundPayment(any(RefundPaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void shouldGetAllPaymentsAndReturn200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId("cust-111")
                .amount(new BigDecimal("99.99"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getAllPayments()).thenReturn(java.util.List.of(response));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("cust-111"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }
}
