package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
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
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepository orderRepository;
    private final OrderKafkaProducer orderKafkaProducer;
    private final com.ecommerce.order.repository.OutboxEventRepository outboxEventRepository;
    private final OutboxPublisherService outboxPublisherService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) OrderKafkaProducer orderKafkaProducer,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) com.ecommerce.order.repository.OutboxEventRepository outboxEventRepository,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) OutboxPublisherService outboxPublisherService,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderKafkaProducer = orderKafkaProducer;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPublisherService = outboxPublisherService;
        this.objectMapper = objectMapper != null ? objectMapper : new com.fasterxml.jackson.databind.ObjectMapper();
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .id(orderId)
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        for (var itemReq : request.getItems()) {
            BigDecimal subtotal = itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .price(itemReq.getPrice())
                    .build();

            order.addItem(item);
        }

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);
        try { orderRepository.flush(); } catch (Exception ignored) {}
        log.info("Order created successfully with id: {}, total amount: {}", saved.getId(), saved.getTotalAmount());

        // Transactional Outbox Pattern (Priority 3):
        // 1. Construct domain event
        var itemDtos = request.getItems().stream()
                .map(i -> new com.ecommerce.order.event.OrderCreatedEvent.OrderItemDto(i.getProductId(), i.getQuantity(), i.getPrice()))
                .toList();

        com.ecommerce.order.event.OrderCreatedEvent event = new com.ecommerce.order.event.OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "OrderCreatedEvent",
                saved.getId(),
                saved.getCustomerId(),
                saved.getTotalAmount(),
                itemDtos,
                now,
                "corr-" + UUID.randomUUID().toString().substring(0, 8)
        );

        // 2. Persist in outbox table within the same ACID database transaction
        com.ecommerce.order.model.OutboxEvent createdOutboxEvent = null;
        if (outboxEventRepository != null) {
            try {
                String payloadJson = objectMapper.writeValueAsString(event);
                createdOutboxEvent = com.ecommerce.order.model.OutboxEvent.builder()
                        .id(UUID.randomUUID())
                        .aggregateType("Order")
                        .aggregateId(saved.getId().toString())
                        .eventType("OrderCreatedEvent")
                        .payload(payloadJson)
                        .createdAt(Instant.now())
                        .published(false)
                        .retryCount(0)
                        .build();

                createdOutboxEvent = outboxEventRepository.save(createdOutboxEvent);
                try { outboxEventRepository.flush(); } catch (Exception ignored) {}
                log.info("[Transactional Outbox] Saved OrderCreatedEvent to outbox table for orderId: {}", saved.getId());

                if (outboxPublisherService != null) {
                    outboxPublisherService.publishSingleEvent(createdOutboxEvent);
                } else if (orderKafkaProducer != null) {
                    orderKafkaProducer.publishOrderCreated(event);
                }
            } catch (Exception e) {
                log.error("[Transactional Outbox] Failed to serialize or save outbox event: {}", e.getMessage(), e);
                if (orderKafkaProducer != null) {
                    orderKafkaProducer.publishOrderCreated(event);
                }
            }
        } else if (orderKafkaProducer != null) {
            orderKafkaProducer.publishOrderCreated(event);
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        log.info("Fetching order with id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        log.info("Fetching orders for customer: {}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        log.info("Fetching orders with status filter: {}", status);
        List<Order> orders = (status != null)
                ? orderRepository.findByStatus(status)
                : orderRepository.findAll();

        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(UUID id, UpdateOrderStatusRequest request) {
        log.info("Updating status for order: {} to {}", id, request.getStatus());
        Order order = null;
        for (int i = 0; i < 5; i++) {
            var opt = orderRepository.findById(id);
            if (opt.isPresent()) {
                order = opt.get();
                break;
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        if (order == null) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }

        if (order.getStatus() == OrderStatus.CONFIRMED && request.getStatus() != OrderStatus.CONFIRMED) {
            log.info("Order {} is already CONFIRMED. Ignoring non-terminal transition to {}", id, request.getStatus());
            return mapToResponse(order);
        }

        validateStateTransition(order.getStatus(), request.getStatus());

        order.setStatus(request.getStatus());
        if (request.getFailureReason() != null) {
            order.setFailureReason(request.getFailureReason());
        }
        order.setUpdatedAt(Instant.now());

        Order updated = orderRepository.save(order);
        if (updated == null) {
            updated = order;
        }
        try { orderRepository.flush(); } catch (Exception ignored) {}
        log.info("Order status updated successfully to: {}", updated.getStatus());
        return mapToResponse(updated);
    }

    @Override
    public OrderResponse cancelOrder(UUID id, String reason) {
        log.info("Cancelling order: {} with reason: {}", id, reason);
        Order order = null;
        for (int i = 0; i < 5; i++) {
            var opt = orderRepository.findById(id);
            if (opt.isPresent()) {
                order = opt.get();
                break;
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        if (order == null) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Confirmed orders cannot be cancelled directly via standard cancellation.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FAILED) {
            log.warn("Order {} is already in terminal state: {}", id, order.getStatus());
            return mapToResponse(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason(reason != null ? reason : "Cancelled by user/system");
        order.setUpdatedAt(Instant.now());

        Order saved = orderRepository.save(order);
        if (saved == null) {
            saved = order;
        }
        try { orderRepository.flush(); } catch (Exception ignored) {}
        return mapToResponse(saved);
    }

    private void validateStateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }

        boolean isValid = switch (current) {
            case PENDING -> next == OrderStatus.INVENTORY_RESERVED
                    || next == OrderStatus.CANCELLED
                    || next == OrderStatus.FAILED;
            case INVENTORY_RESERVED -> next == OrderStatus.PAYMENT_PENDING
                    || next == OrderStatus.CONFIRMED
                    || next == OrderStatus.CANCELLED
                    || next == OrderStatus.FAILED;
            case PAYMENT_PENDING -> next == OrderStatus.CONFIRMED
                    || next == OrderStatus.CANCELLED
                    || next == OrderStatus.FAILED;
            case CONFIRMED, FAILED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new InvalidOrderStateException(
                    String.format("Invalid order state transition from %s to %s", current, next));
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}
