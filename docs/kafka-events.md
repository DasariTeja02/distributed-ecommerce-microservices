# Kafka Event Catalog & Choreography Saga Specification

## Overview

In the Distributed E-Commerce Microservices platform, distributed transactions are coordinated using **Choreography-based Saga** and asynchronous messaging over **Apache Kafka**.

Each microservice autonomously listens to relevant business events, executes local database transactions, and emits subsequent domain events. If any step fails (e.g., inventory shortage or credit card decline), compensating events are broadcast to undo previously committed partial actions.

---

## Event Summary Matrix

| Event Name | Kafka Topic | Producer | Consumer(s) | Partition Key | Business Role |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `OrderCreatedEvent` | `order-events` | `order-service` | `inventory-service` | `orderId` | Initiates Saga workflow |
| `InventoryReservedEvent` | `inventory-events` | `inventory-service` | `payment-service`, `order-service` | `orderId` | Stock reserved; triggers payment |
| `InventoryReservationFailedEvent` | `inventory-events` | `inventory-service` | `order-service` | `orderId` | Insufficient stock; aborts Saga |
| `InventoryReleasedEvent` | `inventory-events` | `inventory-service` | `order-service` | `orderId` | Confirms stock hold release |
| `PaymentCompletedEvent` | `payment-events` | `payment-service` | `order-service` | `orderId` | Funds captured; confirms order |
| `PaymentFailedEvent` | `payment-events` | `payment-service` | `order-service`, `inventory-service` | `orderId` | Payment declined; triggers compensation |
| `PaymentRefundedEvent` | `payment-events` | `payment-service` | `order-service` | `orderId` | Funds refunded during cancellation |
| `OrderConfirmedEvent` | `order-events` | `order-service` | `notification-service` | `orderId` | Order finalization; triggers customer receipt |
| `OrderCancelledEvent` | `order-events` | `order-service` | `inventory-service`, `payment-service`, `notification-service` | `orderId` | Saga rollback; releases holds & sends alert |
| `NotificationDispatchedEvent` | `notification-events` | `notification-service` | Audit / Monitoring | `orderId` | Confirms customer communication |

---

## Detailed Event Specifications

---

### 1. `OrderCreatedEvent`

- **Topic**: `order-events`
- **Producer**: `order-service` (via Transactional Outbox pattern)
- **Consumer(s)**: `inventory-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Published immediately after a customer checks out and the order is persisted in `PENDING` status. Starts the Saga workflow.
- **Event Structure**:
```json
{
  "eventId": "b3e0a12e-9d21-4f81-817a-2457199c1042",
  "eventType": "OrderCreatedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "customerId": "cust-001",
  "totalAmount": 1299.99,
  "items": [
    {
      "productId": "PROD-LAPTOP-01",
      "quantity": 1,
      "price": 1299.99
    }
  ],
  "timestamp": "2026-10-04T12:00:00.000Z",
  "correlationId": "corr-8f921a9d-5412"
}
```
- **Failure / Compensation Behavior**:
  If the downstream inventory service is unavailable, Kafka retains the event across consumer restarts until successfully processed. If inventory is insufficient, `inventory-service` emits `InventoryReservationFailedEvent`.

---

### 2. `InventoryReservedEvent`

- **Topic**: `inventory-events`
- **Producer**: `inventory-service`
- **Consumer(s)**: `payment-service`, `order-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Emitted when warehouse stock is successfully decremented from available inventory and moved into `reserved_quantity`. Signals `payment-service` to authorize and charge the payment.
- **Event Structure**:
```json
{
  "eventId": "f74a9101-768a-4467-bc18-87612c8b091a",
  "eventType": "InventoryReservedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "customerId": "cust-001",
  "totalAmount": 1299.99,
  "items": [
    {
      "productId": "PROD-LAPTOP-01",
      "quantity": 1
    }
  ],
  "status": "RESERVED",
  "timestamp": "2026-10-04T12:00:01.000Z",
  "correlationId": "corr-8f921a9d-5412"
}
```
- **Failure / Compensation Behavior**:
  If payment subsequently fails, `payment-service` emits `PaymentFailedEvent`, causing `inventory-service` to release the held stock.

---

### 3. `InventoryReservationFailedEvent`

- **Topic**: `inventory-events`
- **Producer**: `inventory-service`
- **Consumer(s)**: `order-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Emitted when one or more items in the order do not have sufficient stock. Immediately transitions the order to `CANCELLED`.
- **Event Structure**:
```json
{
  "eventId": "18df602a-9e12-401f-bf77-789a4401c991",
  "eventType": "InventoryReservationFailedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "customerId": "cust-001",
  "reason": "Insufficient stock for product PROD-LAPTOP-01 (requested: 5, available: 1)",
  "timestamp": "2026-10-04T12:00:01.500Z",
  "correlationId": "corr-8f921a9d-5412"
}
```
- **Failure / Compensation Behavior**:
  No payment was ever initiated, so no refund is required. `order-service` updates status to `CANCELLED` and broadcasts `OrderCancelledEvent`.

---

### 4. `InventoryReleasedEvent`

- **Topic**: `inventory-events`
- **Producer**: `inventory-service`
- **Consumer(s)**: `order-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Emitted during compensation when reserved items are restored back to the available inventory pool.
- **Event Structure**:
```json
{
  "eventId": "3c846b01-5231-419b-a019-91a0b3e61882",
  "eventType": "InventoryReleasedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "status": "RELEASED",
  "timestamp": "2026-10-04T12:00:03.000Z",
  "correlationId": "corr-8f921a9d-5412"
}
```

---

### 5. `PaymentCompletedEvent`

- **Topic**: `payment-events`
- **Producer**: `payment-service`
- **Consumer(s)**: `order-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Emitted when customer credit card / payment authorization succeeds. Tells `order-service` that all Saga steps succeeded and the order can be `CONFIRMED`.
- **Event Structure**:
```json
{
  "eventId": "a7491028-5690-4e3a-8671-192a6b4129bb",
  "eventType": "PaymentCompletedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "paymentId": "73d91901-4462-4aa8-9121-cc5819028a3f",
  "customerId": "cust-001",
  "amount": 1299.99,
  "transactionReference": "TXN-SIM-73D91901",
  "timestamp": "2026-10-04T12:00:02.000Z",
  "correlationId": "corr-8f921a9d-5412"
}
```
- **Failure / Compensation Behavior**:
  If the order is subsequently cancelled manually by a customer or admin before fulfillment, `payment-service` listens to `OrderCancelledEvent` and issues a refund (`PaymentRefundedEvent`).

---

### 6. `PaymentFailedEvent`

- **Topic**: `payment-events`
- **Producer**: `payment-service`
- **Consumer(s)**: `order-service`, `inventory-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Emitted when the transaction is declined (e.g., card expired, insufficient funds). Triggers the compensating transaction in `inventory-service` to release the held inventory.
- **Event Structure**:
```json
{
  "eventId": "ee80124c-9821-4f10-9111-209a8bc43001",
  "eventType": "PaymentFailedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "customerId": "cust-001",
  "amount": 1299.99,
  "reason": "Card declined: insufficient balance",
  "timestamp": "2026-10-04T12:00:02.500Z",
  "correlationId": "corr-8f921a9d-5412"
}
```
- **Failure / Compensation Behavior**:
  - `inventory-service` receives `PaymentFailedEvent`, locates the reservation for `orderId`, decrements `reserved_quantity`, increments `quantity`, and emits `InventoryReleasedEvent`.
  - `order-service` receives `PaymentFailedEvent` and transitions order status to `CANCELLED`.

---

### 7. `PaymentRefundedEvent`

- **Topic**: `payment-events`
- **Producer**: `payment-service`
- **Consumer(s)**: `order-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Emitted when a previously successful transaction is refunded during order cancellation.
- **Event Structure**:
```json
{
  "eventId": "591ab392-1209-4ce8-b092-29019aa14bc8",
  "eventType": "PaymentRefundedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "paymentId": "73d91901-4462-4aa8-9121-cc5819028a3f",
  "amount": 1299.99,
  "timestamp": "2026-10-04T12:00:04.000Z",
  "correlationId": "corr-8f921a9d-5412"
}
```

---

### 8. `OrderConfirmedEvent`

- **Topic**: `order-events`
- **Producer**: `order-service`
- **Consumer(s)**: `notification-service`, Frontend SSE Broadcaster
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Signals the final success of the Saga. Triggers customer email/SMS dispatch and pushes real-time confirmation to the UI via Server-Sent Events.
- **Event Structure**:
```json
{
  "eventId": "7a892b10-6101-455b-bf99-8812c3841029",
  "eventType": "OrderConfirmedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "customerId": "cust-001",
  "totalAmount": 1299.99,
  "timestamp": "2026-10-04T12:00:02.800Z",
  "correlationId": "corr-8f921a9d-5412"
}
```

---

### 9. `OrderCancelledEvent`

- **Topic**: `order-events`
- **Producer**: `order-service`
- **Consumer(s)**: `inventory-service`, `payment-service`, `notification-service`
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Emitted when an order cannot be fulfilled or is aborted by user. Instructs all downstream services to release any remaining holds and notify the customer.
- **Event Structure**:
```json
{
  "eventId": "da809210-234b-4b11-9a76-20091aa4655f",
  "eventType": "OrderCancelledEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "customerId": "cust-001",
  "reason": "Payment authorization declined by card issuer",
  "timestamp": "2026-10-04T12:00:03.200Z",
  "correlationId": "corr-8f921a9d-5412"
}
```

---

### 10. `NotificationDispatchedEvent`

- **Topic**: `notification-events`
- **Producer**: `notification-service`
- **Consumer(s)**: Audit & Observability Collectors
- **Partition Key**: `orderId` (`UUID`)
- **Business Purpose**: Logs that an email, SMS, or alert was generated and delivered to the customer.
- **Event Structure**:
```json
{
  "eventId": "ee48921a-6701-4328-98f1-28019a278912",
  "eventType": "NotificationDispatchedEvent",
  "orderId": "c5f5905d-2490-48e0-a548-c89b88ef11bf",
  "recipient": "cust-001",
  "channel": "EMAIL",
  "status": "SENT",
  "timestamp": "2026-10-04T12:00:03.500Z",
  "correlationId": "corr-8f921a9d-5412"
}
```

---

## Partitioning Strategy

All events emitted across the Saga workflow use **`orderId`** as their Kafka record partition key:
`ProducerRecord<>(topic, orderId.toString(), payload)`

### Why Partition by `orderId`?
1. **Total In-Order Guarantee per Order**:
   Kafka guarantees strict FIFO ordering within a single partition. Partitioning by `orderId` ensures that `OrderCreatedEvent`, `InventoryReservedEvent`, `PaymentCompletedEvent`, and any compensation events for the *same* order are processed in exact chronological sequence.
2. **Horizontal Concurrency**:
   Different orders have distinct partition keys, distributing load uniformly across partition consumers and allowing parallel Saga processing at scale.
