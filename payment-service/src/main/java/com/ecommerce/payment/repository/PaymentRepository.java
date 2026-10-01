package com.ecommerce.payment.repository;

import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Payment> findByStatus(PaymentStatus status);
}
