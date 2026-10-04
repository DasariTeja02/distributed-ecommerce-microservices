/**
 * Distributed E-Commerce Microservices — Centralized Service Configuration
 *
 * Single Source of Truth for all frontend links, ports, Swagger URLs, and health endpoints.
 * Conforms to Requirement 1 of the Distributed E-Commerce Microservices Control Center.
 */

const SERVICE_CONFIG = {
  frontend: {
    key: "frontend",
    name: "Frontend Control Center",
    subName: "Node.js Static & Control Dashboard",
    port: 3000,
    url: "http://localhost:3000",
    swagger: null,
    health: null,
    icon: "💻",
    description: "Main operational web console for distributed system management and live telemetry."
  },

  eureka: {
    key: "eureka",
    name: "Service Registry",
    subName: "Spring Cloud Netflix Eureka",
    port: 8761,
    url: "http://localhost:8761",
    swagger: null,
    health: "http://localhost:8761/actuator/health",
    icon: "🔭",
    description: "Decentralized service discovery registry maintaining active instance heartbeats."
  },

  configServer: {
    key: "configServer",
    name: "Config Server",
    subName: "Centralized Native / Git Config",
    port: 8888,
    url: "http://localhost:8888",
    swagger: null,
    health: "http://localhost:8888/actuator/health",
    icon: "⚙️",
    description: "Central configuration repository server backing all runtime microservices properties."
  },

  gateway: {
    key: "gateway",
    name: "API Gateway",
    subName: "Spring Cloud Gateway (Reactive)",
    port: 8080,
    url: "http://localhost:8080",
    swagger: "http://localhost:8080/swagger-ui.html",
    health: "http://localhost:8080/actuator/health",
    icon: "🚪",
    description: "High-throughput non-blocking ingress proxy with JWT validation and rate limiting."
  },

  product: {
    key: "product",
    name: "Product Service",
    subName: "Spring Data MongoDB",
    port: 8081,
    url: "http://localhost:8081",
    swagger: "http://localhost:8081/swagger-ui.html",
    health: "http://localhost:8081/actuator/health",
    icon: "📦",
    description: "Catalog store managing item hierarchies and full product specifications."
  },

  order: {
    key: "order",
    name: "Order Service",
    subName: "Spring Data JPA / PostgreSQL",
    port: 8082,
    url: "http://localhost:8082",
    swagger: "http://localhost:8082/swagger-ui.html",
    health: "http://localhost:8082/actuator/health",
    icon: "📑",
    description: "Orchestrates order state lifecycle and triggers choreography Saga domain events."
  },

  inventory: {
    key: "inventory",
    name: "Inventory Service",
    subName: "Spring Data JPA / PostgreSQL",
    port: 8083,
    url: "http://localhost:8083",
    swagger: "http://localhost:8083/swagger-ui.html",
    health: "http://localhost:8083/actuator/health",
    icon: "🏭",
    description: "Transactional stock reservations preventing overselling with idempotency guarantees."
  },

  payment: {
    key: "payment",
    name: "Payment Service",
    subName: "Spring Data JPA / PostgreSQL",
    port: 8084,
    url: "http://localhost:8084",
    swagger: "http://localhost:8084/swagger-ui.html",
    health: "http://localhost:8084/actuator/health",
    icon: "💳",
    description: "Payment debit engine executing financial state verifications and compensation refunds."
  },

  notification: {
    key: "notification",
    name: "Notification Service",
    subName: "Spring Kafka Consumer / Alerts",
    port: 8085,
    url: "http://localhost:8085",
    swagger: "http://localhost:8085/swagger-ui.html",
    health: "http://localhost:8085/actuator/health",
    icon: "🔔",
    description: "Kafka consumer consuming confirmed/cancelled events and dispatching alerts."
  },

  prometheus: {
    key: "prometheus",
    name: "Prometheus",
    subName: "Metrics & Time-Series Scrapes",
    port: 9090,
    url: "http://localhost:9090",
    swagger: null,
    health: "http://localhost:9090/-/healthy",
    icon: "🔥",
    description: "Scrapes Micrometer endpoints across microservices to collect system and JVM metrics."
  },

  grafana: {
    key: "grafana",
    name: "Grafana",
    subName: "Observability Dashboards",
    port: 3001,
    url: "http://localhost:3001",
    swagger: null,
    health: "http://localhost:3001/api/health",
    icon: "📊",
    description: "Pre-configured dashboards for Saga transactions, Kafka latency, and HTTP request throughput."
  },

  zipkin: {
    key: "zipkin",
    name: "Zipkin",
    subName: "Distributed Tracing",
    port: 9411,
    url: "http://localhost:9411",
    swagger: null,
    health: "http://localhost:9411/health",
    icon: "⚡",
    description: "Visualizes distributed trace spans across Gateway, Order, Inventory, and Payment services."
  }
};

// Expose globally for browser usage and CommonJS for potential node scripts
if (typeof window !== "undefined") {
  window.SERVICE_CONFIG = SERVICE_CONFIG;
}
if (typeof module !== "undefined" && module.exports) {
  module.exports = SERVICE_CONFIG;
}
