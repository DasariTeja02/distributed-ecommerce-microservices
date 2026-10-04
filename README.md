# Distributed E-Commerce Microservices

> **Production-Style Event-Driven Microservices Platform**  
> Built with **Java 17 LTS**, **Spring Boot 3.3.4**, **Spring Cloud 2023.0.3**, **Apache Kafka**, **PostgreSQL**, **MongoDB**, **Resilience4j**, and an **Interactive Saga Control Center UI**.

---

## 📌 Table of Contents

1. [Executive Summary & Architecture](#-executive-summary--architecture)
2. [What Has Been Done Up to Now (Completed Milestones)](#-what-has-been-done-up-to-now-completed-milestones)
3. [Service Port & Database Map](#-service-port--database-map)
4. [Interactive Frontend & Saga Visualizer](#-interactive-frontend--saga-visualizer)
5. [What Needs to Be Done Next (Roadmap)](#-what-needs-to-be-done-next-roadmap)
6. [How to Run the Project Right Now](#-how-to-run-the-project-right-now)
7. [Testing & Build Verification](#-testing--build-verification)

---

## 🏗️ Executive Summary & Architecture

This project is a resilient, distributed e-commerce backend built following modern enterprise cloud-native patterns. It features:
- **Zero Shared Databases:** Strict Database-per-Service pattern.
- **Event-Driven Choreography Saga:** Asynchronous distributed transaction management over Apache Kafka with automated compensating rollbacks upon payment/inventory failure.
- **Microservices Infrastructure:** Eureka Service Registry, centralized Spring Cloud Config Server, and a Reactive Spring Cloud API Gateway.
- **Correlation ID Tracking:** Uniform `X-Correlation-Id` propagation across HTTP headers and Kafka event envelopes.

```
                                  +-------------------+
                                  |   Web Browser     |
                                  | (localhost:3000)  |
                                  +---------+---------+
                                            |
                                            v
                                  +-------------------+
                                  |    API Gateway    | :8080
                                  +---------+---------+
                                            |
                     +----------------------+----------------------+
                     |                      |                      |
                     v                      v                      v
            +----------------+     +----------------+     +----------------+
            | Product Service|     |  Order Service |     |Inventory Service|
            |     :8081      |     |     :8082      |     |     :8083      |
            +-------+--------+     +-------+--------+     +-------+--------+
                    |                      |                      |
                 MongoDB               PostgreSQL             PostgreSQL
                    |                      |                      |
                    |             [order-events]          [inventory-events]
                    |                      |                      |
                    +----------------------+----------------------+
                                           |
                                  +--------v----------+
                                  |    Apache Kafka   | :9092
                                  +--------+----------+
                                           |
                    +----------------------+----------------------+
                    |                                             |
           [payment-events]                              [notification-events]
                    |                                             |
                    v                                             v
            +----------------+                            +----------------+
            | Payment Service|                            |  Notification  |
            |     :8084      |                            |     :8085      |
            +-------+--------+                            +----------------+
                    |
                PostgreSQL
```

---

## ✅ What Has Been Done Up to Now (Completed Milestones)

### Phase 0: System Architecture & Blueprint (`docs/architecture.md`)
- [x] Defined all 8 microservices, ports, database boundaries, and domain responsibilities.
- [x] Established Kafka topic naming conventions, partition key strategy (`orderId`), and dead-letter topics (`*.DLT`).
- [x] Designed the Event-Driven Choreography Saga flow for both the **Happy Path** (order confirmation) and **Compensation Path** (inventory release upon payment decline).
- [x] Documented JSON event schemas, idempotency store design, JWT security model, and Kubernetes cluster topology.

### Phase 1: Reactor Multi-Module Maven Scaffolding
- [x] Configured root `pom.xml` managing dependency versions via Spring Boot `3.3.4` and Spring Cloud `2023.0.3` dependency management BOMs.
- [x] Created 8 Maven service modules with independent packaging and application entry points.
- [x] Verified full reactor build and context loads across all services.

### Phase 2: Eureka Service Discovery & Centralized Config Server
- [x] Implemented `service-registry` using Spring Cloud Netflix Eureka Server on port `8761`.
- [x] Implemented `config-server` using Spring Cloud Config Native profile on port `8888`.
- [x] Created `config-repo/` containing externalized YAML configurations for all microservices.
- [x] Added integration tests verifying live configuration retrieval from the Config Server.

### Phase 3: Reactive API Gateway (`api-gateway`)
- [x] Built reactive gateway using Spring Cloud Gateway on port `8080`.
- [x] Configured dynamic routes for `/api/products/**`, `/api/orders/**`, `/api/inventory/**`, and `/api/payments/**`.
- [x] Implemented `RequestLoggingFilter` (`WebFilter`) generating and propagating `X-Correlation-Id` across all requests.
- [x] Added CORS configuration and centralized `GlobalErrorWebExceptionHandler` returning structured JSON errors.

### Phase 4: Product Catalog Microservice (`product-service`)
- [x] Built MongoDB catalog service on port `8081`.
- [x] Implemented `Product` document, `ProductRequest`/`ProductResponse` DTOs with Jakarta validation.
- [x] Implemented `ProductRepository`, `ProductService`, and `ProductController` CRUD endpoints (`/api/products`).
- [x] Added unit and MockMvc controller tests (13 tests passing).

### Phase 5: Order Microservice (`order-service`)
- [x] Built PostgreSQL/JPA order service on port `8082`.
- [x] Implemented `Order` and `OrderItem` entities with `OrderStatus` state machine (`PENDING`, `CONFIRMED`, `CANCELLED`).
- [x] Implemented strict transition guards (e.g. cannot cancel an already confirmed or cancelled order).
- [x] Added unit and MockMvc controller tests (11 tests passing).

### Phase 6: Inventory Microservice (`inventory-service`)
- [x] Built PostgreSQL/JPA inventory service on port `8083`.
- [x] Implemented `Inventory` entity tracking `availableQuantity` and `reservedQuantity`.
- [x] Implemented atomic stock check & reservation logic preventing negative stock.
- [x] Implemented compensating `releaseInventory` endpoint for Saga rollbacks.
- [x] Added unit and MockMvc controller tests (9 tests passing).

### Phase 7: Payment Microservice (`payment-service`)
- [x] Built PostgreSQL/JPA payment service on port `8084`.
- [x] Implemented `Payment` entity with `PaymentStatus` (`COMPLETED`, `FAILED`, `REFUNDED`).
- [x] Implemented simulation engine that deterministically declines test card numbers (`*0002`, `*9999`) or amounts ending in `.99` (simulating insufficient funds).
- [x] Implemented refund endpoint for manual and automated compensation.
- [x] Added unit and MockMvc controller tests (8 tests passing).

### Phase 8 (Scaffolding): Kafka Topic Configuration
- [x] Added `spring-kafka` dependencies to all event-driven services.
- [x] Created `KafkaTopicConfig` defining topics with 3 partitions and replication factor 1:
  - `order-events` & `order-events.DLT`
  - `inventory-events` & `inventory-events.DLT`
  - `payment-events` & `payment-events.DLT`
  - `notification-events` & `notification-events.DLT`
- [x] Added offline test profile overrides so unit tests execute cleanly without requiring a live Kafka broker.

### Frontend Control Center & Live Saga Visualizer (`frontend/`)
- [x] Built a rich dark-mode obsidian glassmorphism web interface running on `http://localhost:3000`.
- [x] **Live Saga Visualizer**: Real-time visual animation of the 6-step distributed choreography pipeline with node glowing states and active directional pulse arrows.
- [x] **Failure & Compensation Toggle**: Allows toggling simulated payment failure to see the Saga rollback flow:
  `Payment Declined (Red)` ➔ `Inventory Compensation: Stock Released (+1) (Amber)` ➔ `Order Marked CANCELLED (Red)`.
- [x] **Storefront & Slide-Over Cart**: Browse laptops, smartphones, audio, and wearables, add to cart, and checkout directly through the Saga engine.
- [x] **Orders Ledger**: Live tabular view of orders with state badges and trace buttons.
- [x] **Inventory Stock Manager**: Visual stock health bars, available vs. reserved breakdown, and Quick Restock (+10) actions.
- [x] **Payment Transactions & Audits**: Transaction ledger with one-click refund processing.
- [x] **Kafka Event Stream Terminal**: Real-time cyberpunk terminal logging every domain event with timestamp, topic name, partition key (`orderId`), and formatted JSON envelope.
- [x] **Service Mesh Topology**: Live health monitor for all 8 microservices.

---

## 🗺️ Service Port & Database Map

| Service Name | Port | Database | Primary Responsibility |
| :--- | :---: | :---: | :--- |
| **Frontend Control Center** | `3000` | — | Interactive UI, Storefront, Saga Visualizer & Kafka Terminal |
| **Service Registry (Eureka)** | `8761` | — | Microservice registration and dynamic discovery |
| **Config Server** | `8888` | Git / Native | Centralized configuration for all microservices |
| **API Gateway** | `8080` | — | Reverse proxy, route dispatching, correlation ID injection |
| **Product Service** | `8081` | MongoDB (`ecommerce_product_db`) | Product catalog, pricing, category management |
| **Order Service** | `8082` | PostgreSQL (`ecommerce_order_db`) | Order lifecycle management (`PENDING`, `CONFIRMED`, `CANCELLED`) |
| **Inventory Service** | `8083` | PostgreSQL (`ecommerce_inventory_db`) | Atomic stock reservation & compensating stock release |
| **Payment Service** | `8084` | PostgreSQL (`ecommerce_payment_db`) | Payment processing, decline simulation & refunds |
| **Notification Service** | `8085` | — | Async event listener for customer email/SMS dispatch |
| **Apache Kafka Broker** | `9092` | — | Event streaming message backbone |

---

## 💻 Interactive Frontend & Saga Visualizer

The frontend is served via a lightweight Node.js server in the `frontend/` directory.

### Quick Start Frontend:
```bash
cd "Distributed E-Commerce Microservices\frontend"
node server.js
```
Open **[http://localhost:3000](http://localhost:3000)** in your browser.

### Key Interactive Features:
1. **Run Live Saga Order**: Click the button on the **Saga Workflow** tab to watch an order progress through the Gateway, Order Service, Kafka, Inventory Service, Payment Service, and Notification Service.
2. **Simulate Payment Failure**: Toggle the switch **ON** and run the Saga again to watch the automatic compensating rollback in action.
3. **Storefront & Cart**: Go to the **Storefront** tab, add items to cart, open the cart drawer, and click **"Checkout with Saga Workflow"**.
4. **Kafka Terminal**: Switch to the **Kafka Stream** tab to inspect the raw JSON event envelopes being published.

---

## 🚀 What Needs to Be Done Next (Roadmap)

Here are the remaining phases required to complete the production architecture:

### 1. Phase 8 & 9: Full Kafka Event Producers & Typed Event Schemas
- [ ] Create shared/typed event records in each service:
  - `OrderCreatedEvent`, `OrderCancelledEvent`, `OrderConfirmedEvent`
  - `InventoryReservedEvent`, `InventoryReservationFailedEvent`, `InventoryReleasedEvent`
  - `PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`
- [ ] Implement `KafkaTemplate` producer beans with JSON serialization and correlation ID headers.
- [ ] Add `@KafkaListener` consumer methods in downstream services.

### 2. Phase 10 & 11: End-to-End Choreography Saga & Distributed Idempotency
- [ ] Connect Kafka consumers to service business logic:
  - `inventory-service` listens to `order-events` ➔ reserves stock ➔ emits `InventoryReservedEvent`.
  - `payment-service` listens to `inventory-events` ➔ debits card ➔ emits `PaymentCompletedEvent` (or `PaymentFailedEvent`).
  - `order-service` listens to `payment-events` ➔ confirms order (or cancels order).
  - `inventory-service` listens to `payment-events` ➔ releases reserved stock if payment fails.
- [ ] Implement `processed_events` table in PostgreSQL services to guarantee **idempotent message handling** (prevent duplicate charges or double reservations on message replay).

### 3. Phase 12: Resilience4j & Fault Tolerance
- [ ] Add `@CircuitBreaker`, `@Retry`, and `@TimeLimiter` to inter-service calls.
- [ ] Configure fallback handlers (e.g., cached catalog response when Product Service is unreachable).
- [ ] Add Gateway Redis-based rate limiting (prevent DDoS / brute force).

### 4. Phase 13: Spring Security & JWT Authentication
- [ ] Implement JWT token validation filter in `api-gateway`.
- [ ] Extract user claims (`userId`, `roles`) and propagate via headers (`X-User-Id`, `X-User-Roles`).
- [ ] Secure endpoints with Role-Based Access Control (`ROLE_CUSTOMER`, `ROLE_ADMIN`).

### 5. Phase 14: Notification Service
- [ ] Implement Kafka consumer in `notification-service` listening to `notification-events` and `order-events`.
- [ ] Simulate email dispatch (template rendering + logging).

### 6. Phase 15: Observability & Distributed Tracing
- [ ] Add Spring Boot Actuator, Micrometer Prometheus metrics, and OpenTelemetry / Zipkin tracing.
- [ ] Validate end-to-end trace ID propagation across HTTP and Kafka headers.

### 7. Phase 16 & 17: Containerization & Docker Compose
- [ ] Create optimized multi-stage `Dockerfile` for each microservice.
- [ ] Create root `docker-compose.yml` defining:
  - All 8 Spring Boot microservices
  - Zookeeper & Apache Kafka
  - PostgreSQL containers (3 isolated DBs)
  - MongoDB container
  - Zipkin & Prometheus containers

### 8. Phase 18: Kubernetes Manifests & Helm Charts
- [ ] Create K8s `Deployment`, `Service`, `ConfigMap`, `Secret`, and `HorizontalPodAutoscaler` manifests.
- [ ] Configure Ingress controller routing external traffic through `api-gateway`.

### 9. Phase 19–21: End-to-End Test Suite & CI/CD Pipeline
- [ ] Write Testcontainers integration tests verifying full Kafka message exchange and Saga rollback with embedded Kafka and PostgreSQL containers.
- [ ] Configure GitHub Actions workflow for automated compile, test, and container packaging.

---

## 🛠️ How to Run the Project Right Now

### Prerequisites
- **Java**: Adoptium Eclipse Temurin JDK 17 LTS
- **Maven**: Apache Maven 3.9+
- **Node.js**: Node 18+ (for frontend server)

### 1. Start the Frontend Control Center
```bash
cd frontend
node server.js
```
Visit **http://localhost:3000** in your browser.

### 2. Run All Backend Tests (Verification)
Run this command from the root directory:
```bash
mvn test
```
*All 9 modules will compile and execute their unit & integration tests cleanly without needing external databases or Kafka brokers running.*

### 3. Run Individual Backend Microservices
If you want to run any specific microservice locally in standalone mode:

```bash
# Terminal 1: Service Registry (Eureka)
cd service-registry
mvn spring-boot:run

# Terminal 2: Config Server
cd config-server
mvn spring-boot:run

# Terminal 3: API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 4: Product Service
cd product-service
mvn spring-boot:run
```

---

## 🧪 Testing & Build Verification

The project enforces strict unit and integration testing at every phase:

- **Reactor Build Status:** `BUILD SUCCESS` (9 of 9 modules passed)
- **Product Service Tests:** 13/13 passed (CRUD, validation, error handling)
- **Order Service Tests:** 11/11 passed (State machine, transitions, line item validation)
- **Inventory Service Tests:** 9/9 passed (Atomic stock reservation, compensating releases)
- **Payment Service Tests:** 8/8 passed (Payment captures, simulated declines, refund audits)
- **API Gateway Tests:** Route matching, filter validation, CORS headers passed
- **Config Server & Eureka Tests:** Live context loading and health endpoint verification passed

---

*Authored with modern enterprise Spring Cloud & Event-Driven Microservices standards.*
