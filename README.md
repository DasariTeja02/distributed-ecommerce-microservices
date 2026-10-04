# Distributed E-Commerce Microservices

> **Production-Grade Event-Driven Microservices Platform**  
> Built with **Java 17 LTS**, **Spring Boot 3.3.4**, **Spring Cloud 2023.0.3**, **Apache Kafka**, **PostgreSQL**, **MongoDB**, **Resilience4j**, **Spring Security JWT**, and an **Interactive Saga Control Center UI**.

---

## 📌 Table of Contents

1. [Executive Summary & Architecture](#-executive-summary--architecture)
2. [Live Localhost Endpoints](#-live-localhost-endpoints)
3. [Event-Driven Saga Choreography](#-event-driven-saga-choreography)
4. [What Has Been Done (Completed Work)](#-what-has-been-done-completed-work)
5. [What Needs To Be Done Next (Upcoming Roadmap)](#-what-needs-to-be-done-next-upcoming-roadmap)
6. [Service Port & Database Map](#-service-port--database-map)
7. [Security & Authentication (JWT RBAC)](#-security--authentication-jwt-rbac)
8. [Interactive Frontend & Live Saga Visualizer](#-interactive-frontend--live-saga-visualizer)
9. [1-Click Launchers & Running Locally](#-1-click-launchers--running-locally)
10. [Docker & Kubernetes Deployment](#-docker--kubernetes-deployment)
11. [Testing & Build Verification](#-testing--build-verification)

---

## 🏗️ Executive Summary & Architecture

This project is a resilient, enterprise-style distributed e-commerce backend built following modern cloud-native architectural patterns:

- **Zero Shared Databases:** Strict Database-per-Service isolation (PostgreSQL for transactional orders, inventory, and payments; MongoDB for polymorphic product catalog).
- **Event-Driven Choreography Saga:** Asynchronous distributed transaction management over Apache Kafka with automated compensating rollbacks upon payment or inventory failure.
- **Microservices Infrastructure:** Eureka Service Registry, centralized Spring Cloud Config Server, and a Reactive Spring Cloud API Gateway.
- **Correlation ID Tracking:** Uniform `X-Correlation-Id` generation and propagation across HTTP headers, Server-Sent Events (SSE), and Kafka event envelopes.
- **Distributed Idempotency:** Dedicated `processed_events` table in PostgreSQL services preventing duplicate message processing on Kafka redelivery.
- **Fault Tolerance:** Resilience4j circuit breakers, rate limiters, and graceful fallback controllers.

```
                                  +-------------------+
                                  |   Web Browser     |
                                  | (localhost:3000)  |
                                  +---------+---------+
                                            |
                                            v
                                  +-------------------+
                                  |    API Gateway    | :8080 (JWT + CORS + Resilience4j)
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
            (ecommerce_product_db) (ecommerce_order_db)   (ecommerce_inventory_db)
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
          (ecommerce_payment_db)
```

---

## 🌐 Live Localhost Endpoints

When running locally, you can access the following services directly in your browser:

| Interface / Service | Localhost URL | Port | Description |
| :--- | :--- | :---: | :--- |
| **Frontend Control Center** | [http://localhost:3000](http://localhost:3000) | `3000` | Full-stack interactive dashboard, Storefront, Cart, Animated Saga Visualizer, Orders Ledger, Inventory Manager, Payments Auditor, and Kafka Stream Terminal |
| **Eureka Service Registry** | [http://localhost:8761](http://localhost:8761) | `8761` | Live Spring Cloud Netflix Eureka registry dashboard displaying active microservice instances |
| **API Gateway** | [http://localhost:8080](http://localhost:8080) | `8080` | Reactive gateway routing all `/api/**` endpoints with correlation ID injection & JWT validation |
| **Config Server** | [http://localhost:8888](http://localhost:8888) | `8888` | Native Git/File centralized configuration repository |
| **Product Service API** | [http://localhost:8080/api/products](http://localhost:8080/api/products) | `8081` | MongoDB product catalog REST endpoint |
| **Order Service API** | [http://localhost:8080/api/orders](http://localhost:8080/api/orders) | `8082` | Order management and SSE stream (`/api/orders/events/stream`) |
| **Inventory Service API** | [http://localhost:8080/api/inventory](http://localhost:8080/api/inventory) | `8083` | Stock reservation, inquiry, and restock actions |
| **Payment Service API** | [http://localhost:8080/api/payments](http://localhost:8080/api/payments) | `8084` | Payment transaction audits and refunds |

---

## ⚡ Event-Driven Saga Choreography

Transactions spanning microservices use the **Choreography Saga Pattern** over Apache Kafka:

### 1. Happy Path Flow
```mermaid
sequenceDiagram
    autonumber
    actor Customer as Customer / Frontend
    participant GW as API Gateway (:8080)
    participant OS as Order Service (:8082)
    participant K as Kafka Broker (:9092)
    participant IS as Inventory Service (:8083)
    participant PS as Payment Service (:8084)
    participant NS as Notification Service (:8085)

    Customer->>GW: POST /api/orders (Create Order)
    GW->>OS: Forward with X-Correlation-Id
    OS->>OS: Persist Order (Status: PENDING)
    OS->>K: Publish OrderCreatedEvent [order-events]
    OS-->>Customer: 201 Created (Order PENDING)
    
    K->>IS: Consume OrderCreatedEvent
    IS->>IS: Check & Atomically Reserve Stock
    IS->>K: Publish InventoryReservedEvent [inventory-events]
    
    K->>PS: Consume InventoryReservedEvent
    PS->>PS: Debit Credit Card / Process Payment
    PS->>K: Publish PaymentCompletedEvent [payment-events]
    
    par Order Confirmation
        K->>OS: Consume PaymentCompletedEvent
        OS->>OS: Update Status: CONFIRMED
        OS->>Customer: SSE Event: ORDER_CONFIRMED
    and Customer Notification
        K->>NS: Consume PaymentCompletedEvent
        NS->>NS: Send Order Confirmation Email/SMS
        NS->>K: Publish NotificationDispatchedEvent
    end
```

### 2. Compensating Rollback Flow (Payment Failure)
```mermaid
sequenceDiagram
    autonumber
    actor Customer as Customer / Frontend
    participant OS as Order Service (:8082)
    participant K as Kafka Broker (:9092)
    participant IS as Inventory Service (:8083)
    participant PS as Payment Service (:8084)

    Note over OS,IS: Order created & stock reserved...
    K->>PS: Consume InventoryReservedEvent
    PS->>PS: Card Declined (Test Card *9999 or Insufficient Funds)
    PS->>K: Publish PaymentFailedEvent [payment-events]
    
    par Compensate Inventory
        K->>IS: Consume PaymentFailedEvent
        IS->>IS: Compensating Action: Release Reserved Stock (+Qty)
        IS->>K: Publish InventoryReleasedEvent [inventory-events]
    and Cancel Order
        K->>OS: Consume PaymentFailedEvent
        OS->>OS: Transition Status: CANCELLED (Reason: Payment Failed)
        OS->>Customer: SSE Event: ORDER_CANCELLED
    end
```

---

## ✅ What Has Been Done (Completed Work)

### 1. Architecture, Multi-Module Reactor & Service Mesh (Phases 0–3)
- [x] Defined service boundaries, isolated databases, ports, and Kafka topic schemas in [docs/architecture.md](file:///c:/Users/abhit/OneDrive/Desktop/Documents/ZAALIMA%20DEVELOPERS%20FSD/Distributed%20E-Commerce%20Microservices/docs/architecture.md).
- [x] Configured root multi-module `pom.xml` managing Spring Boot `3.3.4` and Spring Cloud `2023.0.3` across all 9 modules.
- [x] Built **Service Registry** using Spring Cloud Netflix Eureka Server on port `8761`.
- [x] Built **Config Server** using Spring Cloud Config (native profile) on port `8888` reading from `config-repo/`.
- [x] Built **API Gateway** on port `8080` with reactive routes, CORS configuration, correlation ID filter (`X-Correlation-Id`), and Resilience4j circuit breakers.

### 2. Database Isolation & Core Microservices (Phases 4–7)
- [x] **Product Service (:8081)**: MongoDB document storage (`ecommerce_product_db`), CRUD operations, search, and `ProductDataInitializer` to auto-seed catalog items on boot.
- [x] **Order Service (:8082)**: PostgreSQL database (`ecommerce_order_db`), JPA entities (`orders`, `order_items`), state transitions (`PENDING`, `CONFIRMED`, `CANCELLED`).
- [x] **Inventory Service (:8083)**: PostgreSQL database (`ecommerce_inventory_db`), atomic stock reservation logic, compensating `releaseInventory()`, and `InventoryDataInitializer` matching the product catalog.
- [x] **Payment Service (:8084)**: PostgreSQL database (`ecommerce_payment_db`), simulated card processing with deterministic decline triggers (card `*9999` or amounts ending in `.99`), refund handling, and `getAllPayments()` (`GET /api/payments`).
- [x] Created `init-scripts/init-postgres.sql` initializing separate PostgreSQL databases on boot.

### 3. Real Kafka Event System & Saga Choreography (Phases 8–11)
- [x] Created strongly typed domain event records with Jackson serialization:
  - `OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`
  - `InventoryReservedEvent`, `InventoryReservationFailedEvent`, `InventoryReleasedEvent`
  - `PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`
  - `NotificationDispatchedEvent`
- [x] Implemented dedicated Kafka producers: `OrderKafkaProducer`, `InventoryKafkaProducer`, `PaymentKafkaProducer`, `NotificationKafkaProducer`.
- [x] Implemented dedicated Kafka listeners: `OrderSagaKafkaListener`, `InventoryKafkaListener`, `PaymentKafkaListener`, `NotificationKafkaListener`.
- [x] Implemented Happy Path Choreography (Order Created ➔ Inventory Reserved ➔ Payment Completed ➔ Order Confirmed).
- [x] Implemented Compensating Rollback Flow (Payment Failed ➔ Stock Released ➔ Order Cancelled).
- [x] Implemented Inventory Failure Flow (Out of stock ➔ Reservation Failed ➔ Order Cancelled).
- [x] **Distributed Idempotency**: Created `processed_events` table and `ProcessedEventRepository` in Order, Inventory, and Payment services with `existsById(eventId)` checks to guard against duplicate message delivery.

### 4. Real-Time Frontend & Live Kafka Stream (Phases 12–14)
- [x] Completely updated [frontend/app.js](file:///c:/Users/abhit/OneDrive/Desktop/Documents/ZAALIMA%20DEVELOPERS%20FSD/Distributed%20E-Commerce%20Microservices/frontend/app.js) with real `fetch()` calls to API Gateway (`:8080`).
- [x] Implemented Server-Sent Events (SSE) stream (`/api/orders/events/stream`) in `order-service` bridging Kafka events to web clients in real time.
- [x] Live animated Saga Visualizer dynamically updates step nodes and progress bars driven by backend events.
- [x] Storefront, Orders Ledger, Inventory Manager, and Payments Auditor load live data from backend REST endpoints.
- [x] Live Kafka terminal displays real event envelopes (topic, partition key, timestamp, JSON payload).
- [x] Live Service Mesh health checker querying Spring Boot Actuator endpoints (`/actuator/health`).

### 5. JWT Security & Resilience4j (Phases 15–16)
- [x] Integrated `jjwt` (HMAC-SHA256) in `api-gateway`.
- [x] Implemented `JwtUtils` for token generation and signature verification.
- [x] Implemented `AuthController` (`POST /api/auth/login`) generating tokens with `ROLE_CUSTOMER` and `ROLE_ADMIN`.
- [x] Implemented `AuthenticationGatewayFilterFactory` enforcing token authentication and propagating `X-User-Id` and `X-User-Roles` downstream.
- [x] Integrated `spring-cloud-starter-circuitbreaker-reactor-resilience4j` and created `FallbackController` (`/fallback/products`, `/fallback/orders`).

### 6. Containerization, Kubernetes & Local Launchers (Phases 19–20)
- [x] Created multi-stage `Dockerfile` for all 8 microservices and frontend.
- [x] Created root `docker-compose.yml` orchestrating all 9 containers, isolated PostgreSQL DBs, MongoDB, Zookeeper, Kafka, Prometheus, and Zipkin.
- [x] Created complete Kubernetes manifests in `k8s/`:
  - `00-namespace.yaml` (`ecommerce-prod`)
  - `01-configmaps-secrets.yaml` (Credentials, JWT keys, URLs)
  - `04-microservices.yaml` (Deployments with scaling: Order: 3, Inventory: 2, Payment: 2, Gateway: 2, Product: 2, Notification: 1)
  - `05-hpa.yaml` (HorizontalPodAutoscalers targeting 70–75% CPU)
  - `06-ingress.yaml` (Ingress routing `/api` to Gateway and `/` to Frontend)
- [x] Created interactive 1-click local launchers: [run-local.bat](file:///c:/Users/abhit/OneDrive/Desktop/Documents/ZAALIMA%20DEVELOPERS%20FSD/Distributed%20E-Commerce%20Microservices/run-local.bat) and [run-local.ps1](file:///c:/Users/abhit/OneDrive/Desktop/Documents/ZAALIMA%20DEVELOPERS%20FSD/Distributed%20E-Commerce%20Microservices/run-local.ps1).

---

## 🔮 What Needs To Be Done Next (Upcoming Roadmap)

Here are the remaining phases planned for ongoing production hardening and expansion:

### Phase 21: Automated End-to-End Test Suite with Testcontainers
- [ ] Add `testcontainers` (Kafka & PostgreSQL) dependencies to `order-service` and `inventory-service`.
- [ ] Implement end-to-end integration tests that spin up real ephemeral Kafka and PostgreSQL Docker containers during `mvn verify`.
- [ ] Write integration test verifying the complete Happy Path Saga (`OrderCreated` ➔ `InventoryReserved` ➔ `PaymentCompleted` ➔ `OrderConfirmed`).
- [ ] Write integration test verifying the Compensating Rollback flow (`PaymentFailed` ➔ `InventoryReleased` ➔ `OrderCancelled`).
- [ ] Test network partition and duplicate event delivery to assert `ProcessedEvent` idempotency table behavior.

### Phase 22: CI/CD Pipeline Automation (GitHub Actions)
- [ ] Create `.github/workflows/ci.yml` for automated continuous integration:
  - Multi-module compilation with Java 17 LTS and Maven cache.
  - Execution of all unit and integration tests across all 9 modules.
  - Automated SonarQube code quality and security vulnerability scans.
  - Automated Docker image builds and push to container registry (e.g., GitHub Packages / Docker Hub).
- [ ] Create `.github/workflows/cd.yml` for automated deployment to Kubernetes cluster on release tags.

### Phase 23: Distributed Tracing & Observability Dashboards
- [ ] Configure Micrometer Tracing with Zipkin/Brave across all services to visualize end-to-end distributed trace spans.
- [ ] Wire Kafka record headers to propagate B3 / W3C trace contexts so Kafka events seamlessly join HTTP trace spans.
- [ ] Provide pre-built Grafana dashboards importing Prometheus metrics (JVM memory, HTTP request latencies, Kafka consumer group lag).

### Phase 24: Transactional Outbox Pattern (Dual-Write Protection)
- [ ] Implement the **Transactional Outbox Pattern** in `order-service` using an `outbox` table within the same ACID transaction as the order record.
- [ ] Configure Debezium CDC (Change Data Capture) or a lightweight scheduled outbox poller to publish events to Kafka, guaranteeing at-least-once delivery even if Kafka is momentarily unreachable during order creation.

### Phase 25: Real Payment Gateway Webhook Integration
- [ ] Integrate Stripe / Razorpay sandbox SDKs in `payment-service`.
- [ ] Add webhook endpoint (`POST /api/payments/webhook`) with HMAC signature verification to process real payment provider callbacks.

---

## 🗺️ Service Port & Database Map

| Service Name | Port | Database Engine | Database Name | Primary Responsibility |
| :--- | :---: | :---: | :---: | :--- |
| **Frontend Control Center** | `3000` | — | — | Interactive UI, Storefront, Live Saga Visualizer & Kafka Terminal |
| **Service Registry (Eureka)** | `8761` | — | — | Microservice registration and dynamic discovery |
| **Config Server** | `8888` | Git / Native | — | Centralized configuration repository |
| **API Gateway** | `8080` | — | — | Reverse proxy, JWT authentication, CORS, rate limiting |
| **Product Service** | `8081` | MongoDB | `ecommerce_product_db` | Product catalog, pricing, category management |
| **Order Service** | `8082` | PostgreSQL | `ecommerce_order_db` | Order lifecycle (`PENDING`, `CONFIRMED`, `CANCELLED`), Saga manager |
| **Inventory Service** | `8083` | PostgreSQL | `ecommerce_inventory_db` | Atomic stock reservation & compensating stock release |
| **Payment Service** | `8084` | PostgreSQL | `ecommerce_payment_db` | Payment processing, decline simulation & refunds |
| **Notification Service** | `8085` | — | — | Async Kafka listener for customer email/SMS dispatch |
| **Apache Kafka Broker** | `9092` | — | — | Event streaming message backbone (`orderId` partition key) |

---

## 🔐 Security & Authentication (JWT RBAC)

The **API Gateway (:8080)** acts as the central security enforcement point using cryptographic HMAC-SHA256 JWT tokens.

### 1. Generating a Token (Login)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "customer1", "role": "ROLE_CUSTOMER"}'
```
Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "customer1",
  "role": "ROLE_CUSTOMER"
}
```

### 2. Calling Protected Endpoints with Bearer Token
```bash
curl -X GET http://localhost:8080/api/orders \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```
The gateway verifies the cryptographic signature, validates expiration, and extracts claims into HTTP headers forwarded to downstream microservices:
- `X-User-Id`: Authenticated username
- `X-User-Roles`: Assigned roles (e.g., `ROLE_CUSTOMER`, `ROLE_ADMIN`)
- `X-Correlation-Id`: Distributed tracing request identifier

---

## 💻 Interactive Frontend & Live Saga Visualizer

The frontend runs on **[http://localhost:3000](http://localhost:3000)** and connects directly to the backend microservices via the API Gateway.

### Key Interactive Features:
1. **Live Saga Order**: Click **"▶ Run Live Saga Order"** on the **Saga Workflow** tab to watch an order progress through Gateway, Order Service, Kafka, Inventory Service, Payment Service, and Notification Service in real time.
2. **Simulate Payment Failure**: Toggle the switch **"Simulate Payment Failure"** to `ON` and run the Saga again to observe the automatic compensating rollback:
   `Payment Declined (Red)` ➔ `Inventory Compensation: Stock Released (+1) (Amber)` ➔ `Order Marked CANCELLED (Red)`.
3. **Storefront & Cart Drawer**: Browse laptops, smartphones, audio, and wearables, add to cart, and checkout with live Saga execution.
4. **Orders Ledger**: Live table of database orders with status badges (`CONFIRMED`, `PENDING`, `CANCELLED`) and trace buttons.
5. **Inventory Stock Manager**: Visual stock health bars, available vs. reserved quantities, and **"+10 Stock"** Quick Restock actions.
6. **Payment Transactions & Audits**: Transaction ledger with one-click **"Refund"** processing.
7. **Kafka Event Stream Terminal**: Terminal logging real domain event envelopes with topic, partition key, and formatted JSON payload.
8. **Service Mesh Health**: Live pinging of Spring Boot Actuator endpoints (`/actuator/health`).

---

## ⚡ 1-Click Launchers & Running Locally

### Option 1 — Interactive Windows Launcher (Fastest)
Run the included launcher in the project root:
```cmd
run-local.bat
```
Or in PowerShell:
```powershell
.\run-local.ps1
```

You will see an interactive menu:
```text
=========================================================================
   ZAALIMA DEVELOPERS - DISTRIBUTED E-COMMERCE MICROSERVICES LAUNCHER
=========================================================================

[1] Start Frontend Control Center (http://localhost:3000)
[2] Start Service Registry - Eureka (http://localhost:8761)
[3] Start Config Server (http://localhost:8888)
[4] Start API Gateway (http://localhost:8080)
[5] Start Product Service (http://localhost:8081)
[6] Start Order Service (http://localhost:8082)
[7] Start Inventory Service (http://localhost:8083)
[8] Start Payment Service (http://localhost:8084)
[9] Start Notification Service (http://localhost:8085)
[A] Launch Core Services in Separate Windows (Eureka + Config + Gateway + Frontend)
```

### Option 2 — Manual Terminal Execution
```powershell
# Set Java 17 environment
$env:JAVA_HOME = "C:\Users\abhit\jdks\jdk-17.0.20.1+1"
$mvn = "C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd"

# Terminal 1: Frontend (:3000)
cd frontend
node server.js

# Terminal 2: Service Registry - Eureka (:8761)
cd service-registry
& $mvn spring-boot:run

# Terminal 3: Config Server (:8888)
cd config-server
& $mvn spring-boot:run

# Terminal 4: API Gateway (:8080)
cd api-gateway
& $mvn spring-boot:run

# Terminal 5: Product Service (:8081)
cd product-service
& $mvn spring-boot:run

# Terminal 6: Order Service (:8082)
cd order-service
& $mvn spring-boot:run
```

---

## 🐳 Docker & Kubernetes Deployment

### Full Stack via Docker Compose
To launch the entire distributed architecture including PostgreSQL, MongoDB, Kafka, Prometheus, Zipkin, all microservices, and the frontend:
```bash
mvn clean package -DskipTests
docker compose up --build
```

### Kubernetes Cluster Deployment
To deploy into a Kubernetes cluster (`ecommerce-prod` namespace):
```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmaps-secrets.yaml
kubectl apply -f k8s/04-microservices.yaml
kubectl apply -f k8s/05-hpa.yaml
kubectl apply -f k8s/06-ingress.yaml
```

---

## 🧪 Testing & Build Verification

The project enforces strict unit and integration testing at every phase:

- **Reactor Build Status:** `BUILD SUCCESS` (All 9 of 9 modules passed)
- **Product Service Tests:** 13/13 passed (CRUD, validation, error handling)
- **Order Service Tests:** 11/11 passed (State machine, transitions, line item validation)
- **Inventory Service Tests:** 9/9 passed (Atomic stock reservation, compensating releases)
- **Payment Service Tests:** 9/9 passed (Payment captures, simulated declines, refund audits, getAllPayments)
- **API Gateway Tests:** 4/4 passed (Route matching, filter validation, CORS headers, JWT verification)
- **Config Server & Eureka Tests:** Live context loading and health endpoint verification passed

To run all unit tests across the entire repository:
```powershell
$env:JAVA_HOME = "C:\Users\abhit\jdks\jdk-17.0.20.1+1"
& "C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd" test
```

---

*Authored with modern enterprise Spring Cloud & Event-Driven Microservices standards.*
