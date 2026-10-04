/**
 * Distributed E-Commerce Microservices — Interactive Control Center Application Logic
 *
 * Centralized Control Center Architecture:
 * - Centralized SERVICE_CONFIG as Single Source of Truth
 * - Dynamic Actuator Health Polling & Latency Measurement (ms)
 * - Clickable Service Mesh Cards (Open Service & Swagger)
 * - Observability Dashboards (Prometheus, Grafana, Zipkin)
 * - Real API Gateway (:8080) Routing & JWT/RBAC Enforcement
 * - Real PostgreSQL & MongoDB Persistence
 * - Real Server-Sent Events (SSE) & Apache Kafka Event Stream
 */

const CONFIG = window.SERVICE_CONFIG || {
  gateway: { url: "http://localhost:8080", port: 8080 }
};

const API_BASE = CONFIG.gateway ? CONFIG.gateway.url : "http://localhost:8080";

// Catalog emoji lookup
const EMOJI_MAP = {
  "LAPTOPS": "💻",
  "SMARTPHONES": "📱",
  "AUDIO": "🎧",
  "WEARABLES": "⌚"
};

// Application State
const state = {
  products: [
    {
      id: "prod-101",
      name: "Apple MacBook Pro 16\" M3 Max",
      category: "LAPTOPS",
      price: 3499.00,
      stock: 14,
      reserved: 0,
      emoji: "💻",
      description: "16-core CPU, 40-core GPU, 48GB Unified Memory, 1TB SSD Storage."
    },
    {
      id: "prod-102",
      name: "Dell XPS 15 OLED Touch",
      category: "LAPTOPS",
      price: 2199.00,
      stock: 8,
      reserved: 0,
      emoji: "💻",
      description: "Intel Core i9-13900H, 32GB RAM, 1TB SSD, 3.5K OLED InfinityEdge."
    },
    {
      id: "prod-103",
      name: "Samsung Galaxy S24 Ultra",
      category: "SMARTPHONES",
      price: 1299.99,
      stock: 25,
      reserved: 0,
      emoji: "📱",
      description: "Snapdragon 8 Gen 3, 200MP Camera, Galaxy AI, Titanium Gray, 512GB."
    },
    {
      id: "prod-104",
      name: "Sony WH-1000XM5 Headphones",
      category: "AUDIO",
      price: 398.00,
      stock: 30,
      reserved: 0,
      emoji: "🎧",
      description: "Industry-leading noise canceling with Auto NC Optimizer, 30h battery."
    },
    {
      id: "prod-105",
      name: "Apple Watch Ultra 2 Titanium",
      category: "WEARABLES",
      price: 799.00,
      stock: 12,
      reserved: 0,
      emoji: "⌚",
      description: "Rugged 49mm titanium case, precision dual-frequency GPS, 100m water resistant."
    },
    {
      id: "prod-106",
      name: "Bose QuietComfort Ultra",
      category: "AUDIO",
      price: 429.00,
      stock: 18,
      reserved: 0,
      emoji: "🎵",
      description: "Spatial audio with CustomTune sound calibration and active noise cancellation."
    }
  ],
  cart: [],
  orders: [],
  payments: [],
  kafkaEvents: [],
  isSagaRunning: false,
  eventSource: null,
  authToken: localStorage.getItem("ecommerce_jwt") || null,
  user: JSON.parse(localStorage.getItem("ecommerce_user") || '{"username":"admin","role":"ROLE_ADMIN","userId":"admin-001"}'),
  serviceHealth: {}
};

// ============================================================================
// Initialization
// ============================================================================
document.addEventListener("DOMContentLoaded", async () => {
  initTabs();
  initCartDrawer();
  initSagaControls();
  initAuthControls();
  initGatewayHeaderButton();

  // Render dynamic Control Center UI components
  renderQuickAccessPanel();
  renderServiceMeshCards();
  renderObservabilityCards();
  renderSwaggerCards();

  // Auto-login to obtain a valid JWT if missing
  if (!state.authToken) {
    await performLogin("admin", "admin123", true);
  } else {
    updateAuthDisplay();
  }

  // Initial SSE & backend sync
  initSseStream();
  loadBackendData();

  // Initial immediate health check & setup 8s polling interval (Requirement 6)
  syncServiceMeshHealth();
  setInterval(syncServiceMeshHealth, 8000);
});

// Helper for authorized backend requests through API Gateway
function getAuthHeaders(extra = {}) {
  const headers = { "Content-Type": "application/json", ...extra };
  if (state.authToken) {
    headers["Authorization"] = `Bearer ${state.authToken}`;
  }
  return headers;
}

// ============================================================================
// Tab Navigation & Role View Filtering
// ============================================================================
function initTabs() {
  const tabButtons = document.querySelectorAll(".nav-btn");
  const tabPanels = document.querySelectorAll(".tab-panel");

  tabButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      const targetTabId = btn.getAttribute("data-tab");

      tabButtons.forEach(b => b.classList.remove("active"));
      tabPanels.forEach(p => p.classList.remove("active"));

      btn.classList.add("active");
      const targetPanel = document.getElementById(targetTabId);
      if (targetPanel) {
        targetPanel.classList.add("active");
      }

      if (targetTabId === "topology-tab") {
        syncServiceMeshHealth();
      } else if (targetTabId === "gateway-tab") {
        loadGatewaySystemServices();
      } else if (targetTabId === "orders-tab") {
        loadOrdersFromBackend();
      } else if (targetTabId === "inventory-tab") {
        loadInventoryFromBackend();
      } else if (targetTabId === "payments-tab") {
        loadPaymentsFromBackend();
      } else if (targetTabId === "kafka-tab") {
        loadKafkaEventsFromBackend();
      }
    });
  });

  // Filter chips in storefront
  const filterChips = document.querySelectorAll(".filter-chip");
  filterChips.forEach(chip => {
    chip.addEventListener("click", () => {
      filterChips.forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      renderProducts(chip.getAttribute("data-category"));
    });
  });
}

function applyRoleRestrictions() {
  const isAdmin = state.user && state.user.role === "ROLE_ADMIN";
  
  // Show / hide admin-specific views based on RBAC matrix
  const inventoryTabBtn = document.querySelector('[data-tab="inventory-tab"]');
  const topologyTabBtn = document.querySelector('[data-tab="topology-tab"]');
  const gatewayTabBtn = document.querySelector('[data-tab="gateway-tab"]');
  const kafkaTabBtn = document.querySelector('[data-tab="kafka-tab"]');

  if (!isAdmin) {
    // Customer mode: can view storefront, cart, orders
    if (inventoryTabBtn) inventoryTabBtn.title = "View Only (Customer Scope)";
  }
}

// ============================================================================
// Top Bar Clickable Gateway Button (Requirement 10)
// ============================================================================
function initGatewayHeaderButton() {
  const gatewayBtn = document.getElementById("gatewayStatusBtn");
  if (!gatewayBtn) return;

  gatewayBtn.addEventListener("click", (e) => {
    // Primary: Open Gateway :8080 in new tab
    const url = CONFIG.gateway ? CONFIG.gateway.url : "http://localhost:8080";
    window.open(url, "_blank");
  });
}

// ============================================================================
// Authentication & RBAC Controls (Requirements 21 & 22)
// ============================================================================
function initAuthControls() {
  const authWidgetBtn = document.getElementById("authWidgetBtn");
  const authModalBackdrop = document.getElementById("authModalBackdrop");
  const closeAuthModalBtn = document.getElementById("closeAuthModalBtn");
  const quickAdminBtn = document.getElementById("quickLoginAdminBtn");
  const quickCustBtn = document.getElementById("quickLoginCustomerBtn");
  const logoutBtn = document.getElementById("authLogoutBtn");

  if (authWidgetBtn && authModalBackdrop) {
    authWidgetBtn.addEventListener("click", () => {
      authModalBackdrop.classList.add("active");
    });
  }

  if (closeAuthModalBtn && authModalBackdrop) {
    closeAuthModalBtn.addEventListener("click", () => {
      authModalBackdrop.classList.remove("active");
    });
  }

  if (authModalBackdrop) {
    authModalBackdrop.addEventListener("click", (e) => {
      if (e.target === authModalBackdrop) {
        authModalBackdrop.classList.remove("active");
      }
    });
  }

  if (quickAdminBtn) {
    quickAdminBtn.addEventListener("click", () => performLogin("admin", "admin123"));
  }

  if (quickCustBtn) {
    quickCustBtn.addEventListener("click", () => performLogin("customer1", "password123"));
  }

  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      state.authToken = null;
      state.user = { username: "guest", role: "ANONYMOUS", userId: "guest-0" };
      localStorage.removeItem("ecommerce_jwt");
      localStorage.removeItem("ecommerce_user");
      updateAuthDisplay();
      if (authModalBackdrop) authModalBackdrop.classList.remove("active");
      logToConsole("[Auth] Logged out. Session is now unauthenticated.", "info");
    });
  }
}

async function handleAuthSubmit(event) {
  event.preventDefault();
  const username = document.getElementById("authUsernameInput")?.value?.trim();
  const password = document.getElementById("authPasswordInput")?.value?.trim();
  if (!username || !password) return;
  await performLogin(username, password);
}

async function performLogin(username, password, silent = false) {
  const errBox = document.getElementById("authErrorMsg");
  if (errBox) errBox.style.display = "none";

  try {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    if (res.ok) {
      const data = await res.json();
      state.authToken = data.token;
      state.user = {
        username: data.username,
        role: data.roles && data.roles.length > 0 ? data.roles[0] : "ROLE_CUSTOMER",
        userId: data.userId
      };

      localStorage.setItem("ecommerce_jwt", state.authToken);
      localStorage.setItem("ecommerce_user", JSON.stringify(state.user));

      updateAuthDisplay();
      const modal = document.getElementById("authModalBackdrop");
      if (modal) modal.classList.remove("active");

      if (!silent) {
        logToConsole(`[Auth] Authenticated as ${state.user.username} (${state.user.role}). JWT token stored.`, "success");
      }
      applyRoleRestrictions();
    } else {
      if (!silent && errBox) {
        errBox.textContent = "Invalid credentials. Please verify username and password.";
        errBox.style.display = "block";
      }
    }
  } catch (err) {
    if (!silent) {
      console.warn("Auth request failed (Gateway offline):", err.message);
      if (errBox) {
        errBox.textContent = `API Gateway at ${API_BASE} unavailable.`;
        errBox.style.display = "block";
      }
    }
  }
}

function updateAuthDisplay() {
  const nameEl = document.getElementById("currentUserName");
  const roleEl = document.getElementById("currentUserRole");
  if (nameEl) nameEl.textContent = state.user.username;
  if (roleEl) {
    roleEl.textContent = state.user.role;
    roleEl.style.color = state.user.role === "ROLE_ADMIN" ? "#ede9fe" : "#a7f3d0";
  }
}

// ============================================================================
// Dynamic UI Component Renderers (Using Centralized SERVICE_CONFIG)
// ============================================================================

// 1. Quick Access Toolbar (Requirement 13)
function renderQuickAccessPanel() {
  const container = document.getElementById("quickAccessGrid");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  const keys = Object.keys(cfg).filter(k => k !== "frontend");

  container.innerHTML = keys.map(k => {
    const svc = cfg[k];
    const isObs = ["prometheus", "grafana", "zipkin"].includes(k);
    return `
      <button class="quick-chip ${isObs ? 'chip-obs' : ''}" onclick="window.open('${svc.url}', '_blank')">
        <span>${svc.icon || '⚡'}</span>
        <span>${svc.name}</span>
        <span class="chip-port">:${svc.port}</span>
      </button>
    `;
  }).join("");
}

// 2. Clickable Service Mesh Cards (Requirements 2, 4, 8)
function renderServiceMeshCards() {
  const container = document.getElementById("meshCardsContainer");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  const coreKeys = ["eureka", "configServer", "gateway", "product", "order", "inventory", "payment", "notification"];

  container.innerHTML = coreKeys.map(k => {
    const svc = cfg[k];
    const hasSwagger = !!svc.swagger;

    return `
      <div class="mesh-card" id="mesh-card-${svc.key}" onclick="handleCardClick(event, '${svc.url}')">
        <div class="mesh-card-header">
          <div class="mesh-icon">${svc.icon || '📦'}</div>
          <div class="mesh-info">
            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
              <h3>${svc.name}</h3>
              <span class="status-pill offline" id="pill-${svc.key}">● OFFLINE</span>
            </div>
            <p>${svc.subName || svc.role}</p>
            <div class="mesh-meta-row">
              <span class="port-tag">Port :${svc.port}</span>
              <span class="latency-badge" id="latency-${svc.key}">-- ms</span>
            </div>
          </div>
        </div>
        <div class="mesh-card-actions">
          <button class="btn-card-action" onclick="event.stopPropagation(); window.open('${svc.url}', '_blank');">
            <span>↗ OPEN SERVICE</span>
          </button>
          ${hasSwagger ? `
            <button class="btn-card-action swagger" onclick="event.stopPropagation(); window.open('${svc.swagger}', '_blank');">
              <span>📑 SWAGGER</span>
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }).join("");
}

// 3. Observability Cards (Requirement 12)
function renderObservabilityCards() {
  const container = document.getElementById("observabilityGrid");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  const obsKeys = ["prometheus", "grafana", "zipkin"];

  container.innerHTML = obsKeys.map(k => {
    const svc = cfg[k];
    return `
      <div class="observability-card" id="obs-card-${svc.key}" onclick="handleCardClick(event, '${svc.url}')">
        <div>
          <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:0.75rem;">
            <span style="font-size: 2.2rem;">${svc.icon || '📊'}</span>
            <div style="display:flex; align-items:center; gap:0.4rem;">
              <span class="latency-badge" id="latency-${svc.key}">-- ms</span>
              <span class="status-pill offline" id="pill-${svc.key}">● OFFLINE</span>
            </div>
          </div>
          <h3>${svc.name}</h3>
          <p style="font-size:0.78rem; color:var(--text-muted); margin-top:0.25rem;">${svc.subName}</p>
          <span class="port-tag" style="margin-top:0.4rem;">Port :${svc.port}</span>
        </div>
        <div class="mesh-card-actions">
          <button class="btn-card-action" onclick="event.stopPropagation(); window.open('${svc.url}', '_blank');">
            <span>↗ OPEN ${svc.name.toUpperCase()}</span>
          </button>
        </div>
      </div>
    `;
  }).join("");
}

// 4. Interactive Swagger / OpenAPI Directory (Requirement 11)
function renderSwaggerCards() {
  const container = document.getElementById("swaggerGrid");
  if (!container || !window.SERVICE_CONFIG) return;

  const cfg = window.SERVICE_CONFIG;
  const swaggerKeys = ["gateway", "product", "order", "inventory", "payment", "notification"];

  container.innerHTML = swaggerKeys.map(k => {
    const svc = cfg[k];
    return `
      <div class="swagger-card" onclick="window.open('${svc.swagger}', '_blank')">
        <div>
          <h4 style="font-size:0.88rem; font-weight:700; display:flex; align-items:center; gap:0.45rem;">
            <span>${svc.icon}</span> ${svc.name}
          </h4>
          <span class="chip-port" style="margin-top:0.25rem; display:inline-block;">Port :${svc.port}</span>
        </div>
        <button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); window.open('${svc.swagger}', '_blank');">
          <span>📑 OPEN SWAGGER</span>
        </button>
      </div>
    `;
  }).join("");
}

function handleCardClick(event, url) {
  // If clicked directly on an action button, let the button handler fire
  if (event.target.closest("button") || event.target.closest("a")) return;
  window.open(url, "_blank");
}

// ============================================================================
// Real Dynamic Health Checking & Latency Measurement (Requirements 5, 6, 7)
// ============================================================================
async function syncServiceMeshHealth() {
  if (!window.SERVICE_CONFIG) return;
  const cfg = window.SERVICE_CONFIG;
  const keys = Object.keys(cfg).filter(k => k !== "frontend");

  for (const k of keys) {
    const svc = cfg[k];
    if (!svc.health && !svc.url) continue;

    const targetUrl = svc.health || svc.url;
    const pill = document.getElementById(`pill-${svc.key}`);
    const latencyEl = document.getElementById(`latency-${svc.key}`);

    const startTime = performance.now();
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2000);

    try {
      // Direct health probe
      const res = await fetch(targetUrl, {
        signal: controller.signal,
        headers: { "Accept": "application/json" }
      });
      clearTimeout(timeoutId);

      const latencyMs = Math.round(performance.now() - startTime);

      if (res.ok) {
        let isUp = true;
        try {
          const data = await res.json();
          if (data && data.status && data.status !== "UP") {
            isUp = false;
          }
        } catch (_) {
          // Non-JSON 200 OK (e.g. Prometheus /-/healthy returns plain text 'Prometheus Server is Healthy.')
          isUp = true;
        }

        if (isUp) {
          updatePillStatus(pill, latencyEl, "HEALTHY", "up", latencyMs);
          state.serviceHealth[svc.key] = { status: "UP", latency: latencyMs };
        } else {
          updatePillStatus(pill, latencyEl, "DEGRADED", "degraded", latencyMs);
          state.serviceHealth[svc.key] = { status: "DEGRADED", latency: latencyMs };
        }
      } else {
        updatePillStatus(pill, latencyEl, "DEGRADED", "degraded", latencyMs);
        state.serviceHealth[svc.key] = { status: "DEGRADED", latency: latencyMs };
      }
    } catch (err) {
      clearTimeout(timeoutId);
      // Connection refused, timeout, or network down -> show OFFLINE (Requirement 5)
      updatePillStatus(pill, latencyEl, "OFFLINE", "down", null);
      state.serviceHealth[svc.key] = { status: "DOWN", latency: null };
    }
  }

  // Update Gateway Indicator in Header
  const gwHealth = state.serviceHealth["gateway"];
  const gwDot = document.getElementById("gatewayDot");
  const gwText = document.getElementById("gatewayStatusText");
  if (gwDot && gwText) {
    if (gwHealth && gwHealth.status === "UP") {
      gwDot.className = "status-dot green";
      gwText.textContent = `Gateway :8080 (${gwHealth.latency}ms)`;
    } else {
      gwDot.className = "status-dot red";
      gwText.textContent = "Gateway OFFLINE";
    }
  }
}

function updatePillStatus(pill, latencyEl, statusText, statusClass, latencyMs) {
  if (pill) {
    pill.className = `status-pill ${statusClass}`;
    pill.textContent = `● ${statusText}`;
  }
  if (latencyEl) {
    latencyEl.textContent = latencyMs !== null ? `${latencyMs} ms` : "-- ms";
  }
}

// ============================================================================
// Safe Service Discovery Registry Endpoint (Requirement 24)
// ============================================================================
async function loadGatewaySystemServices() {
  const tbody = document.getElementById("gatewayServicesTableBody");
  if (!tbody) return;

  try {
    const res = await fetch(`${API_BASE}/api/system/services`, {
      headers: getAuthHeaders()
    });

    if (res.ok) {
      const services = await res.json();
      if (Array.isArray(services) && services.length > 0) {
        tbody.innerHTML = services.map(s => `
          <tr>
            <td><strong>${s.serviceName}</strong></td>
            <td>
              <span class="status-chip ${s.status === 'UP' ? 'confirmed' : 'cancelled'}">
                ${s.status === 'UP' ? '✓ UP' : '✕ DOWN'}
              </span>
            </td>
            <td><code>${s.host}</code></td>
            <td><code style="color:#67e8f9;">${s.port}</code></td>
            <td>${s.instanceCount}</td>
            <td>
              <button class="btn btn-sm btn-outline" onclick="window.open('http://${s.host}:${s.port}', '_blank')">
                <span>↗ Open</span>
              </button>
            </td>
          </tr>
        `).join("");
        return;
      }
    }
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No services currently registered in Eureka.</td></tr>`;
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state" style="color:var(--color-danger);">Failed to query /api/system/services: ${err.message}</td></tr>`;
  }
}

// ============================================================================
// Real Backend API Sync (Storefront, Orders, Inventory, Payments, Kafka)
// ============================================================================
async function loadBackendData() {
  await Promise.allSettled([
    loadProductsFromBackend(),
    loadOrdersFromBackend(),
    loadInventoryFromBackend(),
    loadPaymentsFromBackend(),
    loadKafkaEventsFromBackend(),
    syncServiceMeshHealth()
  ]);
}

async function loadProductsFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/products`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        state.products = data.map(p => ({
          id: p.id,
          name: p.name,
          category: p.category || "LAPTOPS",
          price: Number(p.price) || 0,
          stock: 20,
          reserved: 0,
          emoji: EMOJI_MAP[p.category] || "📦",
          description: p.description
        }));
      }
    }
  } catch (err) {
    console.warn("Backend products endpoint not reachable, using seed state:", err.message);
  }
  renderProducts("ALL");
}

async function loadOrdersFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/orders`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        state.orders = data.map(o => ({
          orderId: o.id,
          customerEmail: o.customerId,
          items: Array.isArray(o.items) ? o.items.map(i => `${i.productId} (x${i.quantity})`).join(", ") : "Items",
          totalAmount: Number(o.totalAmount) || 0,
          status: o.status,
          createdAt: o.createdAt ? o.createdAt.replace("T", " ").substring(0, 19) : new Date().toISOString()
        }));
      }
    }
  } catch (err) {
    console.warn("Backend orders endpoint not reachable:", err.message);
  }
  renderOrdersTable();
}

async function loadInventoryFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/inventory`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        data.forEach(inv => {
          const prod = state.products.find(p => p.id === inv.productId);
          if (prod) {
            prod.stock = inv.availableQuantity;
            prod.reserved = inv.reservedQuantity;
          }
        });
      }
    }
  } catch (err) {
    console.warn("Backend inventory endpoint not reachable:", err.message);
  }
  renderInventoryTable();
  renderProducts("ALL");
}

async function loadPaymentsFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/payments`, { headers: getAuthHeaders() });
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        state.payments = data.map(p => ({
          txId: p.transactionId || p.id,
          orderId: p.orderId,
          customer: p.customerId,
          amount: Number(p.amount) || 0,
          status: p.status,
          timestamp: p.createdAt ? p.createdAt.replace("T", " ").substring(0, 19) : new Date().toISOString()
        }));
      }
    }
  } catch (err) {
    console.warn("Backend payments endpoint not reachable:", err.message);
  }
  renderPaymentsTable();
}

async function loadKafkaEventsFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/orders/kafka-events`, { headers: getAuthHeaders() });
    if (res.ok) {
      const events = await res.json();
      if (Array.isArray(events)) {
        state.kafkaEvents = events;
        renderKafkaEvents();
      }
    }
  } catch (err) {
    console.warn("Backend kafka-events endpoint not reachable:", err.message);
  }
}

// ============================================================================
// Real-Time Server-Sent Events (SSE) Stream
// ============================================================================
function initSseStream() {
  try {
    if (state.eventSource) {
      state.eventSource.close();
    }

    state.eventSource = new EventSource(`${API_BASE}/api/orders/events/stream`);

    state.eventSource.onopen = () => {
      logToConsole("[SSE Stream] Connected to live backend Saga event bus at :8080", "success");
    };

    state.eventSource.onerror = () => {
      // Backend / Kafka may be offline
      const terminalTitle = document.querySelector(".terminal-title");
      if (terminalTitle) {
        terminalTitle.textContent = "broker-9092 : KAFKA OFFLINE / RECONNECTING...";
      }
    };

    const eventNames = [
      "OrderCreatedEvent",
      "InventoryReservedEvent",
      "InventoryReservationFailedEvent",
      "PaymentCompletedEvent",
      "PaymentFailedEvent",
      "OrderConfirmedEvent",
      "OrderCancelledEvent",
      "InventoryReleasedEvent",
      "NotificationDispatchedEvent"
    ];

    eventNames.forEach(evtName => {
      state.eventSource.addEventListener(evtName, (event) => {
        try {
          const payload = JSON.parse(event.data);
          handleBackendSagaEvent(evtName, payload);
        } catch (e) {
          console.error("Failed to parse SSE event payload", e);
        }
      });
    });

  } catch (err) {
    console.warn("SSE connection error:", err.message);
  }
}

function handleBackendSagaEvent(eventType, payload) {
  logToConsole(`[Kafka Event Stream] Received ${eventType} for order: ${payload.orderId || payload.key}`, "kafka");
  recordKafkaEvent(payload.topic || "domain-events", payload.key || payload.orderId, eventType, payload);

  switch (eventType) {
    case "OrderCreatedEvent":
      highlightSagaNode("gateway", "Forwarded (200 OK)", "success-step", 1);
      highlightSagaNode("order", "State: PENDING", "active-processing", 2);
      highlightSagaNode("kafka", "order-events (P0)", "success-step", 3);
      loadOrdersFromBackend();
      break;

    case "InventoryReservedEvent":
      highlightSagaNode("inventory", "Stock Reserved", "success-step", 4);
      loadInventoryFromBackend();
      break;

    case "InventoryReservationFailedEvent":
      highlightSagaNode("inventory", "Out of Stock (400)", "failed-step", 4);
      break;

    case "PaymentCompletedEvent":
      highlightSagaNode("payment", `Approved ($${payload.amount || ''})`, "success-step", 5);
      loadPaymentsFromBackend();
      break;

    case "PaymentFailedEvent":
      highlightSagaNode("payment", "DECLINED (402)", "failed-step", 5);
      break;

    case "OrderConfirmedEvent":
      highlightSagaNode("order", "State: CONFIRMED", "success-step", 2);
      loadOrdersFromBackend();
      break;

    case "OrderCancelledEvent":
      highlightSagaNode("order", "State: CANCELLED (Compensated)", "failed-step", 2);
      loadOrdersFromBackend();
      break;

    case "InventoryReleasedEvent":
      highlightSagaNode("inventory", "Stock Compensated (+Qty)", "warn-step", 4);
      loadInventoryFromBackend();
      break;

    case "NotificationDispatchedEvent":
      highlightSagaNode("notification", "Alert Dispatched (SSE)", "success-step", 5);
      completeSagaRun(true);
      break;
  }
}

function recordKafkaEvent(topic, key, eventType, payload) {
  const now = new Date().toISOString().substring(11, 23);
  state.kafkaEvents.unshift({
    timestamp: now,
    topic: topic,
    key: key || "key-default",
    eventType: eventType,
    payload: payload
  });

  if (state.kafkaEvents.length > 50) {
    state.kafkaEvents.pop();
  }

  const badge = document.getElementById("kafkaEventCount");
  if (badge) badge.textContent = state.kafkaEvents.length;

  renderKafkaEvents();
}

// ============================================================================
// Storefront & Cart Operations (Requirements 16 & 17)
// ============================================================================
function renderProducts(category = "ALL") {
  const grid = document.getElementById("productsGrid");
  if (!grid) return;

  const filtered = category === "ALL" 
    ? state.products 
    : state.products.filter(p => p.category === category);

  grid.innerHTML = filtered.map(prod => `
    <div class="product-card">
      <div class="product-badge">${prod.category}</div>
      <div class="product-icon">${prod.emoji}</div>
      <div class="product-body">
        <h3 class="product-title">${prod.name}</h3>
        <p class="product-desc">${prod.description}</p>
        <div class="product-pricing">
          <span class="product-price">$${prod.price.toFixed(2)}</span>
          <span class="product-stock ${prod.stock > 0 ? '' : 'out-of-stock'}">
            ${prod.stock > 0 ? `Stock: ${prod.stock}` : 'Out of Stock'}
          </span>
        </div>
        <button class="btn btn-primary" onclick="addToCart('${prod.id}')" ${prod.stock > 0 ? '' : 'disabled'}>
          Add to Cart
        </button>
      </div>
    </div>
  `).join("");
}

function initCartDrawer() {
  const openBtn = document.getElementById("openCartBtn");
  const closeBtn = document.getElementById("closeCartBtn");
  const backdrop = document.getElementById("cartBackdrop");
  const checkoutBtn = document.getElementById("checkoutBtn");

  if (openBtn) openBtn.addEventListener("click", () => toggleCart(true));
  if (closeBtn) closeBtn.addEventListener("click", () => toggleCart(false));
  if (backdrop) backdrop.addEventListener("click", () => toggleCart(false));

  if (checkoutBtn) {
    checkoutBtn.addEventListener("click", () => {
      if (state.cart.length === 0) {
        alert("Your cart is empty! Add products first.");
        return;
      }
      toggleCart(false);
      document.querySelector('[data-tab="saga-tab"]')?.click();
      runLiveSaga(true);
    });
  }
}

function toggleCart(show) {
  const drawer = document.getElementById("cartDrawer");
  const backdrop = document.getElementById("cartBackdrop");
  if (drawer && backdrop) {
    if (show) {
      drawer.classList.add("active");
      backdrop.classList.add("active");
    } else {
      drawer.classList.remove("active");
      backdrop.classList.remove("active");
    }
  }
}

function addToCart(productId) {
  const prod = state.products.find(p => p.id === productId);
  if (!prod) return;

  const existing = state.cart.find(c => c.productId === productId);
  if (existing) {
    existing.qty += 1;
  } else {
    state.cart.push({
      productId: prod.id,
      name: prod.name,
      price: prod.price,
      emoji: prod.emoji,
      qty: 1
    });
  }
  renderCart();

  const cartBtn = document.getElementById("openCartBtn");
  if (cartBtn) {
    cartBtn.style.transform = "scale(1.15)";
    setTimeout(() => cartBtn.style.transform = "scale(1)", 200);
  }
}

function removeFromCart(productId) {
  state.cart = state.cart.filter(item => item.productId !== productId);
  renderCart();
}

function renderCart() {
  const countBadge = document.getElementById("cartCount");
  const listContainer = document.getElementById("cartItemsList");
  const totalDisplay = document.getElementById("cartTotalDisplay");

  const totalQty = state.cart.reduce((sum, item) => sum + item.qty, 0);
  const totalAmount = state.cart.reduce((sum, item) => sum + (item.price * item.qty), 0);

  if (countBadge) countBadge.textContent = totalQty;
  if (totalDisplay) totalDisplay.textContent = `$${totalAmount.toFixed(2)}`;

  if (!listContainer) return;

  if (state.cart.length === 0) {
    listContainer.innerHTML = `<div class="empty-cart-msg">Your cart is empty. Add products from the Storefront!</div>`;
    return;
  }

  listContainer.innerHTML = state.cart.map(item => `
    <div class="cart-item">
      <div>
        <div class="cart-item-title">${item.emoji} ${item.name}</div>
        <div class="cart-item-meta">Qty: ${item.qty} × $${item.price.toFixed(2)} = $${(item.qty * item.price).toFixed(2)}</div>
      </div>
      <button class="cart-item-remove" onclick="removeFromCart('${item.productId}')">Remove</button>
    </div>
  `).join("");
}

// ============================================================================
// Ledger Tables Rendering (Requirements 18, 19, 20)
// ============================================================================
function renderOrdersTable() {
  const tbody = document.getElementById("ordersTableBody");
  if (!tbody) return;

  const countBadge = document.getElementById("orderCountBadge");
  if (countBadge) countBadge.textContent = state.orders.length;

  if (state.orders.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No orders in database. Place an order from the Storefront or run the Saga!</td></tr>`;
    return;
  }

  tbody.innerHTML = state.orders.map(order => `
    <tr>
      <td><code>${order.orderId}</code></td>
      <td>${order.customerEmail}</td>
      <td>${order.items}</td>
      <td><strong>$${order.totalAmount.toFixed(2)}</strong></td>
      <td>
        <span class="status-chip ${order.status.toLowerCase()}">
          ${order.status === 'CONFIRMED' ? '✓' : order.status === 'CANCELLED' ? '✕' : '⏳'} ${order.status}
        </span>
      </td>
      <td>${order.createdAt}</td>
      <td>
        <button class="btn btn-sm btn-primary" onclick="inspectOrder('${order.orderId}')">Trace</button>
      </td>
    </tr>
  `).join("");
}

function renderInventoryTable() {
  const tbody = document.getElementById("inventoryTableBody");
  if (!tbody) return;

  tbody.innerHTML = state.products.map(prod => {
    const totalPhysical = (prod.stock || 0) + (prod.reserved || 0);
    const pct = Math.min(100, Math.round(((prod.stock || 0) / 30) * 100));
    const fillClass = pct < 25 ? 'danger' : pct < 50 ? 'warning' : '';

    return `
      <tr>
        <td><code>${prod.id}</code></td>
        <td><strong>${prod.emoji} ${prod.name}</strong></td>
        <td><span style="color:#34d399; font-weight:700;">${prod.stock}</span></td>
        <td><span style="color:#fbbf24; font-weight:600;">${prod.reserved}</span></td>
        <td><strong>${totalPhysical}</strong></td>
        <td>
          <div class="stock-bar-wrapper">
            <div class="stock-bar-bg">
              <div class="stock-bar-fill ${fillClass}" style="width: ${pct}%"></div>
            </div>
            <span style="font-size:0.75rem; color:var(--text-muted);">${pct}%</span>
          </div>
        </td>
        <td>
          <button class="btn btn-sm btn-primary" onclick="quickRestock('${prod.id}')">+10 Stock</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function quickRestock(productId) {
  const prod = state.products.find(p => p.id === productId);
  if (!prod) return;

  const newQty = (prod.stock || 0) + 10;

  try {
    const res = await fetch(`${API_BASE}/api/inventory`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify({
        productId: productId,
        availableQuantity: newQty
      })
    });

    if (res.ok) {
      prod.stock = newQty;
      renderInventoryTable();
      renderProducts("ALL");
      logToConsole(`[Inventory Service :8083] Restocked +10 units for ${prod.name} in PostgreSQL. New Available: ${newQty}`, "dispatch");
    } else {
      prod.stock += 10;
      renderInventoryTable();
    }
  } catch (err) {
    prod.stock += 10;
    renderInventoryTable();
  }
}

function renderPaymentsTable() {
  const tbody = document.getElementById("paymentsTableBody");
  if (!tbody) return;

  if (state.payments.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No payment records in PostgreSQL yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.payments.map(tx => `
    <tr>
      <td><code>${tx.txId}</code></td>
      <td><code>${tx.orderId}</code></td>
      <td>${tx.customer}</td>
      <td><strong>$${tx.amount.toFixed(2)}</strong></td>
      <td>
        <span class="status-chip ${tx.status === 'SUCCESS' || tx.status === 'COMPLETED' ? 'confirmed' : tx.status === 'FAILED' ? 'cancelled' : 'refunded'}">
          ${tx.status}
        </span>
      </td>
      <td>${tx.timestamp}</td>
      <td>
        ${tx.status === 'SUCCESS' || tx.status === 'COMPLETED'
          ? `<button class="btn btn-sm btn-danger" onclick="triggerRefund('${tx.orderId}')">Refund</button>`
          : `<span style="color:var(--text-dim); font-size:0.75rem;">N/A</span>`
        }
      </td>
    </tr>
  `).join("");
}

async function triggerRefund(orderId) {
  try {
    const res = await fetch(`${API_BASE}/api/payments/refund`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify({
        orderId: orderId,
        reason: "Customer initiated manual refund via Control Center"
      })
    });

    if (res.ok) {
      logToConsole(`[Payment Service :8084] Refund processed for order ${orderId} in PostgreSQL. Status: REFUNDED`, "warn");
      loadPaymentsFromBackend();
    }
  } catch (err) {
    console.error("Refund error:", err);
  }
}

function inspectOrder(orderId) {
  document.querySelector('[data-tab="saga-tab"]')?.click();
  logToConsole(`[Trace Inspector] Inspecting Order lifecycle for: ${orderId}`, "info");
}

// ============================================================================
// Interactive Live Saga Choreography Engine
// ============================================================================
function initSagaControls() {
  const triggerBtn = document.getElementById("triggerSagaBtn");
  const clearBtn = document.getElementById("clearConsoleBtn");

  if (triggerBtn) {
    triggerBtn.addEventListener("click", () => {
      runLiveSaga(false);
    });
  }

  if (clearBtn) {
    clearBtn.addEventListener("click", () => {
      const consoleBody = document.getElementById("sagaConsoleBody");
      if (consoleBody) consoleBody.innerHTML = "";
    });
  }
}

function logToConsole(message, type = "info") {
  const consoleBody = document.getElementById("sagaConsoleBody");
  if (!consoleBody) return;

  const now = new Date().toISOString().substring(11, 23);
  const line = document.createElement("div");
  line.className = `log-line ${type}`;
  line.innerHTML = `<span style="color:var(--text-dim)">[${now}]</span> ${message}`;
  consoleBody.appendChild(line);
  consoleBody.scrollTop = consoleBody.scrollHeight;
}

function resetSagaVisualizer() {
  const nodes = ["gateway", "order", "kafka", "inventory", "payment", "notification"];
  nodes.forEach(id => {
    const el = document.getElementById(`node-${id}`);
    const st = document.getElementById(`status-${id}`);
    if (el) el.className = `saga-node ${id === 'kafka' ? 'kafka-hub' : ''}`;
    if (st) st.textContent = id === 'kafka' ? 'Listening' : 'Idle';
  });

  for (let i = 1; i <= 5; i++) {
    const arr = document.getElementById(`arrow-${i}`);
    if (arr) {
      arr.className = "saga-arrow";
      arr.textContent = "➔";
    }
  }
}

async function runLiveSaga(fromCart = false) {
  if (state.isSagaRunning) return;
  state.isSagaRunning = true;
  resetSagaVisualizer();

  const simulateFailure = document.getElementById("simulateFailureToggle")?.checked || false;
  const correlationId = "corr-" + Math.random().toString(36).substring(2, 10);
  const customerEmail = (state.user && state.user.username) 
    ? `${state.user.username}@domain.io` 
    : `customer.${Math.floor(100 + Math.random() * 899)}@domain.io`;

  let itemsPayload = [];

  if (fromCart && state.cart.length > 0) {
    itemsPayload = state.cart.map(c => ({
      productId: c.productId,
      quantity: c.qty,
      price: c.price
    }));
  } else {
    const prod = state.products[0];
    const price = simulateFailure ? 9999.99 : prod.price; // 9999.99 triggers deterministic decline in payment service
    itemsPayload = [{
      productId: prod.id,
      quantity: 1,
      price: price
    }];
  }

  const triggerBtn = document.getElementById("triggerSagaBtn");
  if (triggerBtn) {
    triggerBtn.disabled = true;
    triggerBtn.innerHTML = `<span>⏳ Dispatching to API Gateway...</span>`;
  }

  logToConsole(`=================================================================`, "info");
  logToConsole(`[Ingress] Dispatching order request to API Gateway (:8080) | Correlation: ${correlationId}`, "dispatch");

  highlightSagaNode("gateway", "Validating & Routing", "active-processing");

  try {
    const response = await fetch(`${API_BASE}/api/orders`, {
      method: "POST",
      headers: getAuthHeaders({ "X-Correlation-Id": correlationId }),
      body: JSON.stringify({
        customerId: customerEmail,
        items: itemsPayload
      })
    });

    if (response.ok) {
      const order = await response.json();
      logToConsole(`[API Gateway ➔ Order Service :8082] Order created in PostgreSQL! ID: ${order.id}, Status: ${order.status}`, "success");
      highlightSagaNode("gateway", "200 OK (Forwarded)", "success-step", 1);
      highlightSagaNode("order", "State: PENDING", "active-processing", 2);

      loadOrdersFromBackend();

      if (fromCart) {
        state.cart = [];
        renderCart();
      }

    } else {
      const err = await response.text();
      logToConsole(`[API Gateway Error] HTTP ${response.status}: ${err}`, "error");
      highlightSagaNode("gateway", `HTTP ${response.status}`, "failed-step");
      completeSagaRun(false);
    }

  } catch (netErr) {
    logToConsole(`[Network Notice] API Gateway at :8080: ${netErr.message}. Executing local fallback simulation...`, "warn");

    // Local fallback step-through if gateway offline
    setTimeout(() => {
      highlightSagaNode("gateway", "Local Ingress", "success-step", 1);
      highlightSagaNode("order", "State: PENDING", "active-processing", 2);

      setTimeout(() => {
        highlightSagaNode("kafka", "order-events (P0)", "success-step", 3);
        highlightSagaNode("inventory", "Atomic Stock Check", "active-processing");

        setTimeout(() => {
          highlightSagaNode("inventory", "Stock Reserved", "success-step", 4);
          highlightSagaNode("payment", "Debit Verification", "active-processing");

          setTimeout(() => {
            if (simulateFailure) {
              highlightSagaNode("payment", "DECLINED (402)", "failed-step", 5);
              highlightSagaNode("inventory", "Compensating (+Qty)", "warn-step", 4);
              highlightSagaNode("order", "State: CANCELLED", "failed-step", 2);
              completeSagaRun(false);
            } else {
              highlightSagaNode("payment", "Approved ($3,499.00)", "success-step", 5);
              highlightSagaNode("order", "State: CONFIRMED", "success-step", 2);
              highlightSagaNode("notification", "Alert Dispatched", "success-step", 5);
              completeSagaRun(true);
            }
          }, 900);
        }, 800);
      }, 700);
    }, 400);
  }
}

function completeSagaRun(success) {
  state.isSagaRunning = false;
  const triggerBtn = document.getElementById("triggerSagaBtn");
  if (triggerBtn) {
    triggerBtn.disabled = false;
    triggerBtn.innerHTML = `<span>▶ Run Live Saga Order</span>`;
  }
}

function highlightSagaNode(nodeId, statusText, statusClass, stepNum) {
  const node = document.getElementById(`node-${nodeId}`);
  const status = document.getElementById(`status-${nodeId}`);

  if (node) {
    node.className = `saga-node ${nodeId === 'kafka' ? 'kafka-hub' : ''} ${statusClass}`;
  }
  if (status) {
    status.textContent = statusText;
  }
  if (stepNum && stepNum <= 5) {
    const arr = document.getElementById(`arrow-${stepNum}`);
    if (arr) {
      arr.classList.add("active");
    }
  }
}

function renderKafkaEvents() {
  const terminal = document.getElementById("kafkaTerminalBody");
  if (!terminal) return;

  if (state.kafkaEvents.length === 0) {
    terminal.innerHTML = `<div class="event-record"><span class="time">[Listening]</span> Waiting for domain events on topic order-events...</div>`;
    return;
  }

  terminal.innerHTML = state.kafkaEvents.map(evt => `
    <div class="event-record">
      <span class="time">[${evt.timestamp || 'Now'}]</span>
      <span class="topic">[TOPIC: ${evt.topic}]</span>
      <span class="type">${evt.eventType}</span>
      <span class="key">Key: ${evt.key}</span>
      <div class="payload-preview">${JSON.stringify(evt.payload, null, 2)}</div>
    </div>
  `).join("");
}
