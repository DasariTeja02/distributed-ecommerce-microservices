/**
 * Distributed E-Commerce Microservices — Interactive Control Center Application Logic
 * Supports: Storefront, Live Cart, Animated Saga Visualizer (with Failure Simulation),
 * Orders Ledger, Inventory Manager, Payments & Refunds, Kafka Event Bus Stream, and Mesh Health.
 */

// Application State
const state = {
  products: [
    {
      id: "prod-101",
      name: "Apple MacBook Pro 16\" M3 Max",
      category: "LAPTOPS",
      price: 3499.00,
      stock: 14,
      reserved: 2,
      emoji: "💻",
      description: "16-core CPU, 40-core GPU, 48GB Unified Memory, 1TB SSD Storage."
    },
    {
      id: "prod-102",
      name: "Dell XPS 15 OLED Touch",
      category: "LAPTOPS",
      price: 2199.00,
      stock: 8,
      reserved: 1,
      emoji: "💻",
      description: "Intel Core i9-13900H, 32GB RAM, 1TB SSD, 3.5K OLED InfinityEdge."
    },
    {
      id: "prod-103",
      name: "Samsung Galaxy S24 Ultra",
      category: "SMARTPHONES",
      price: 1299.99,
      stock: 25,
      reserved: 3,
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
      reserved: 2,
      emoji: "🎵",
      description: "Spatial audio with CustomTune sound calibration and active noise cancellation."
    }
  ],
  cart: [],
  orders: [
    {
      orderId: "ord-88310",
      customerEmail: "sarah.connor@cyberdyne.io",
      items: "Apple MacBook Pro 16\" (x1)",
      totalAmount: 3499.00,
      status: "CONFIRMED",
      createdAt: "2026-10-01 11:20:14"
    },
    {
      orderId: "ord-88309",
      customerEmail: "alex.murphy@omnicorp.org",
      items: "Sony WH-1000XM5 (x2)",
      totalAmount: 796.00,
      status: "CANCELLED",
      createdAt: "2026-10-01 11:15:32"
    }
  ],
  payments: [
    {
      txId: "tx-pay-9921",
      orderId: "ord-88310",
      customer: "sarah.connor@cyberdyne.io",
      amount: 3499.00,
      status: "COMPLETED",
      timestamp: "2026-10-01 11:20:16"
    },
    {
      txId: "tx-pay-9920",
      orderId: "ord-88309",
      customer: "alex.murphy@omnicorp.org",
      amount: 796.00,
      status: "FAILED",
      timestamp: "2026-10-01 11:15:34"
    }
  ],
  kafkaEvents: [],
  isSagaRunning: false
};

// ============================================================================
// Initialization & Tab Navigation
// ============================================================================
document.addEventListener("DOMContentLoaded", () => {
  initTabs();
  renderProducts("ALL");
  renderCart();
  renderOrdersTable();
  renderInventoryTable();
  renderPaymentsTable();
  initCartDrawer();
  initSagaControls();
  initKafkaTerminal();
});

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

// ============================================================================
// Storefront Rendering & Cart Logic
// ============================================================================
function renderProducts(category) {
  const container = document.getElementById("productsGrid");
  if (!container) return;

  const filtered = category === "ALL" 
    ? state.products 
    : state.products.filter(p => p.category === category);

  container.innerHTML = filtered.map(p => {
    const isLowStock = p.stock <= 10;
    return `
      <div class="product-card">
        <div class="product-image-container">${p.emoji}</div>
        <span class="product-category-tag">${p.category}</span>
        <h3 class="product-title">${p.name}</h3>
        <p class="product-desc">${p.description}</p>
        <div class="product-card-footer">
          <div>
            <div class="product-price">$${p.price.toFixed(2)}</div>
            <span class="product-stock-tag ${isLowStock ? 'low' : ''}">${p.stock} in stock</span>
          </div>
          <button class="add-cart-btn" onclick="addToCart('${p.id}')">
            Add to Cart
          </button>
        </div>
      </div>
    `;
  }).join("");

  const badge = document.getElementById("productCountBadge");
  if (badge) badge.textContent = state.products.length;
}

function initCartDrawer() {
  const openBtn = document.getElementById("openCartBtn");
  const closeBtn = document.getElementById("closeCartBtn");
  const backdrop = document.getElementById("cartBackdrop");
  const drawer = document.getElementById("cartDrawer");
  const checkoutBtn = document.getElementById("checkoutBtn");

  const toggleCart = (open) => {
    if (drawer && backdrop) {
      drawer.classList.toggle("active", open);
      backdrop.classList.toggle("active", open);
    }
  };

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
      // Switch to Saga tab and trigger
      document.querySelector('[data-tab="saga-tab"]')?.click();
      runLiveSaga(true);
    });
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

  // Subtle button bounce effect
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
// Tables Rendering
// ============================================================================
function renderOrdersTable() {
  const tbody = document.getElementById("ordersTableBody");
  if (!tbody) return;

  const countBadge = document.getElementById("orderCountBadge");
  if (countBadge) countBadge.textContent = state.orders.length;

  if (state.orders.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No orders placed yet. Place an order from the Storefront!</td></tr>`;
    return;
  }

  tbody.innerHTML = state.orders.map(order => `
    <tr>
      <td><strong>${order.orderId}</strong></td>
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
    const totalPhysical = prod.stock + prod.reserved;
    const pct = Math.min(100, Math.round((prod.stock / 30) * 100));
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

function quickRestock(productId) {
  const prod = state.products.find(p => p.id === productId);
  if (!prod) return;
  prod.stock += 10;
  renderInventoryTable();
  renderProducts("ALL");
  logToConsole(`[Inventory Service] Restocked +10 units for ${prod.name}. Available: ${prod.stock}`, "dispatch");
}

function renderPaymentsTable() {
  const tbody = document.getElementById("paymentsTableBody");
  if (!tbody) return;

  if (state.payments.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No payment transactions recorded.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.payments.map(tx => `
    <tr>
      <td><code>${tx.txId}</code></td>
      <td><strong>${tx.orderId}</strong></td>
      <td>${tx.customer}</td>
      <td><strong>$${tx.amount.toFixed(2)}</strong></td>
      <td>
        <span class="status-chip ${tx.status === 'COMPLETED' ? 'confirmed' : tx.status === 'FAILED' ? 'cancelled' : 'refunded'}">
          ${tx.status}
        </span>
      </td>
      <td>${tx.timestamp}</td>
      <td>
        ${tx.status === 'COMPLETED' 
          ? `<button class="btn btn-sm btn-danger" onclick="triggerRefund('${tx.txId}')">Refund</button>`
          : `<span style="color:var(--text-dim); font-size:0.75rem;">N/A</span>`
        }
      </td>
    </tr>
  `).join("");
}

function triggerRefund(txId) {
  const tx = state.payments.find(t => t.txId === txId);
  if (!tx || tx.status !== "COMPLETED") return;
  tx.status = "REFUNDED";
  renderPaymentsTable();
  logToConsole(`[Payment Service] Refund processed for ${txId} on Order ${tx.orderId} ($${tx.amount}). Status: REFUNDED`, "warn");
  recordKafkaEvent("payment-events", tx.orderId, "PaymentRefundedEvent", {
    transactionId: tx.txId,
    orderId: tx.orderId,
    amount: tx.amount,
    reason: "MANUAL_CUSTOMER_SERVICE_REFUND"
  });
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
  const orderId = "ord-" + Math.floor(10000 + Math.random() * 90000);
  const customerEmail = "customer." + Math.floor(100 + Math.random() * 899) + "@domain.io";

  // Pick order item
  let itemName = "Apple MacBook Pro 16\"";
  let itemPrice = 3499.00;
  let targetProduct = state.products[0];

  if (fromCart && state.cart.length > 0) {
    itemName = state.cart.map(c => `${c.name} (x${c.qty})`).join(", ");
    itemPrice = state.cart.reduce((s, c) => s + (c.price * c.qty), 0);
    targetProduct = state.products.find(p => p.id === state.cart[0].productId) || state.products[0];
  }

  const triggerBtn = document.getElementById("triggerSagaBtn");
  if (triggerBtn) {
    triggerBtn.disabled = true;
    triggerBtn.innerHTML = `<span>⏳ Running Saga (${orderId})...</span>`;
  }

  logToConsole(`=================================================================`, "info");
  logToConsole(`[Saga Initiated] Starting Choreography for ${orderId} | Correlation: ${correlationId}`, "info");

  const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

  try {
    // -------------------------------------------------------------
    // Step 1: API Gateway (Port 8080)
    // -------------------------------------------------------------
    const nodeGateway = document.getElementById("node-gateway");
    const statusGateway = document.getElementById("status-gateway");
    const arrow1 = document.getElementById("arrow-1");

    nodeGateway.classList.add("active-processing");
    statusGateway.textContent = "Validating Token & Routing";
    logToConsole(`[API Gateway :8080] POST /api/orders received. Correlation ID header injected. Routing to order-service...`, "dispatch");
    await sleep(650);

    nodeGateway.classList.remove("active-processing");
    nodeGateway.classList.add("success-step");
    statusGateway.textContent = "Forwarded (200 OK)";
    arrow1.classList.add("active");

    // -------------------------------------------------------------
    // Step 2: Order Service (Port 8082, PostgreSQL)
    // -------------------------------------------------------------
    const nodeOrder = document.getElementById("node-order");
    const statusOrder = document.getElementById("status-order");
    const arrow2 = document.getElementById("arrow-2");

    nodeOrder.classList.add("active-processing");
    statusOrder.textContent = "State: PENDING";
    logToConsole(`[Order Service :8082] Created Order [${orderId}] with status PENDING in PostgreSQL. Emitting OrderCreatedEvent to Kafka.`, "dispatch");
    await sleep(700);

    nodeOrder.classList.remove("active-processing");
    nodeOrder.classList.add("success-step");
    arrow2.classList.add("active");

    // Record Event & Add to orders list
    const newOrder = {
      orderId: orderId,
      customerEmail: customerEmail,
      items: itemName,
      totalAmount: itemPrice,
      status: "PENDING",
      createdAt: new Date().toISOString().replace("T", " ").substring(0, 19)
    };
    state.orders.unshift(newOrder);
    renderOrdersTable();

    // -------------------------------------------------------------
    // Step 3: Kafka Broker (:9092)
    // -------------------------------------------------------------
    const nodeKafka = document.getElementById("node-kafka");
    const statusKafka = document.getElementById("status-kafka");
    const arrow3 = document.getElementById("arrow-3");

    nodeKafka.classList.add("active-processing");
    statusKafka.textContent = "order-events (P0)";
    logToConsole(`[Apache Kafka :9092] Message published to 'order-events' [Key: ${orderId}]. Broadcast to consumer groups.`, "kafka");
    recordKafkaEvent("order-events", orderId, "OrderCreatedEvent", {
      orderId, customerEmail, totalAmount: itemPrice, correlationId
    });
    await sleep(600);

    nodeKafka.classList.remove("active-processing");
    statusKafka.textContent = "Forwarded";
    arrow3.classList.add("active");

    // -------------------------------------------------------------
    // Step 4: Inventory Service (:8083, PostgreSQL)
    // -------------------------------------------------------------
    const nodeInventory = document.getElementById("node-inventory");
    const statusInventory = document.getElementById("status-inventory");
    const arrow4 = document.getElementById("arrow-4");

    nodeInventory.classList.add("active-processing");
    statusInventory.textContent = "Reserving Stock...";
    logToConsole(`[Inventory Service :8083] Consumed OrderCreatedEvent. Executing atomic stock check and reservation...`, "dispatch");
    await sleep(750);

    // Reserve stock in state
    if (targetProduct && targetProduct.stock > 0) {
      targetProduct.stock -= 1;
      targetProduct.reserved += 1;
      renderInventoryTable();
      renderProducts("ALL");
    }

    nodeInventory.classList.remove("active-processing");
    nodeInventory.classList.add("success-step");
    statusInventory.textContent = "Stock Reserved (-1)";
    logToConsole(`[Inventory Service :8083] Stock reserved successfully. Emitted InventoryReservedEvent to 'inventory-events'.`, "success");
    recordKafkaEvent("inventory-events", orderId, "InventoryReservedEvent", {
      orderId, productId: targetProduct.id, quantity: 1, correlationId
    });
    arrow4.classList.add("active");

    // -------------------------------------------------------------
    // Step 5: Payment Service (:8084, PostgreSQL)
    // -------------------------------------------------------------
    const nodePayment = document.getElementById("node-payment");
    const statusPayment = document.getElementById("status-payment");

    nodePayment.classList.add("active-processing");
    statusPayment.textContent = "Processing Card...";
    logToConsole(`[Payment Service :8084] Consumed InventoryReservedEvent. Contacting Payment Gateway simulation...`, "dispatch");
    await sleep(900);

    if (simulateFailure) {
      // ===========================================================
      // FAILURE & COMPENSATION FLOW (SAGA ROLLBACK)
      // ===========================================================
      nodePayment.classList.remove("active-processing");
      nodePayment.classList.add("failed-step");
      statusPayment.textContent = "DECLINED (402)";
      logToConsole(`[Payment Service :8084] ❌ Payment Failed! Insufficient funds simulation. Emitting PaymentFailedEvent.`, "error");

      const txRecord = {
        txId: "tx-pay-" + Math.floor(1000 + Math.random() * 9000),
        orderId: orderId,
        customer: customerEmail,
        amount: itemPrice,
        status: "FAILED",
        timestamp: new Date().toISOString().replace("T", " ").substring(0, 19)
      };
      state.payments.unshift(txRecord);
      renderPaymentsTable();

      recordKafkaEvent("payment-events", orderId, "PaymentFailedEvent", {
        orderId, failureReason: "INSUFFICIENT_FUNDS_SIMULATION", correlationId
      });

      // Compensation 1: Inventory Service rollback (Release Reserved Stock)
      await sleep(600);
      nodeInventory.classList.remove("success-step");
      nodeInventory.classList.add("compensated-step");
      statusInventory.textContent = "Stock Released (+1)";
      arrow4.classList.remove("active");
      arrow4.classList.add("rollback");
      arrow4.textContent = "⮌";

      if (targetProduct) {
        targetProduct.stock += 1;
        targetProduct.reserved -= 1;
        renderInventoryTable();
        renderProducts("ALL");
      }
      logToConsole(`[Inventory Service :8083] ⚠️ Compensating Transaction Triggered! Released reserved stock. Emitted InventoryReleasedEvent.`, "warn");

      // Compensation 2: Order Service rollback (Mark CANCELLED)
      await sleep(600);
      nodeOrder.classList.remove("success-step");
      nodeOrder.classList.add("failed-step");
      statusOrder.textContent = "State: CANCELLED";
      arrow3.classList.remove("active");
      arrow3.classList.add("rollback");
      arrow3.textContent = "⮌";

      newOrder.status = "CANCELLED";
      renderOrdersTable();
      logToConsole(`[Order Service :8082] ❌ Saga Rollback Complete. Order ${orderId} transitioned to CANCELLED.`, "error");
      logToConsole(`=================================================================`, "info");

    } else {
      // ===========================================================
      // SUCCESS FLOW (CONFIRMATION)
      // ===========================================================
      nodePayment.classList.remove("active-processing");
      nodePayment.classList.add("success-step");
      statusPayment.textContent = "Approved ($" + itemPrice.toFixed(0) + ")";

      const txRecord = {
        txId: "tx-pay-" + Math.floor(1000 + Math.random() * 9000),
        orderId: orderId,
        customer: customerEmail,
        amount: itemPrice,
        status: "COMPLETED",
        timestamp: new Date().toISOString().replace("T", " ").substring(0, 19)
      };
      state.payments.unshift(txRecord);
      renderPaymentsTable();

      recordKafkaEvent("payment-events", orderId, "PaymentCompletedEvent", {
        transactionId: txRecord.txId, orderId, amount: itemPrice, correlationId
      });
      logToConsole(`[Payment Service :8084] ✓ Payment Captured! Emitted PaymentCompletedEvent.`, "success");

      const arrow5 = document.getElementById("arrow-5");
      arrow5.classList.add("active");

      // Step 6: Notification Service (:8085)
      const nodeNotification = document.getElementById("node-notification");
      const statusNotification = document.getElementById("status-notification");

      nodeNotification.classList.add("active-processing");
      statusNotification.textContent = "Dispatching...";
      await sleep(650);

      nodeNotification.classList.remove("active-processing");
      nodeNotification.classList.add("success-step");
      statusNotification.textContent = "Dispatched (SMS/Email)";
      logToConsole(`[Notification Service :8085] Consumed PaymentCompletedEvent. Order confirmation email dispatched to ${customerEmail}.`, "success");

      recordKafkaEvent("notification-events", orderId, "NotificationDispatchedEvent", {
        orderId, recipient: customerEmail, channel: "EMAIL", correlationId
      });

      // Update Order Status to CONFIRMED
      newOrder.status = "CONFIRMED";
      statusOrder.textContent = "State: CONFIRMED";
      renderOrdersTable();

      logToConsole(`[Saga Complete] Order ${orderId} successfully completed and committed across all distributed services!`, "success");
      logToConsole(`=================================================================`, "info");

      if (fromCart) {
        state.cart = [];
        renderCart();
      }
    }
  } finally {
    state.isSagaRunning = false;
    if (triggerBtn) {
      triggerBtn.disabled = false;
      triggerBtn.innerHTML = `<span>▶ Run Live Saga Order</span>`;
    }
  }
}

// ============================================================================
// Kafka Real-time Stream Terminal
// ============================================================================
function initKafkaTerminal() {
  const countBadge = document.getElementById("kafkaEventCount");
  if (countBadge) countBadge.textContent = state.kafkaEvents.length;
}

function recordKafkaEvent(topic, key, eventType, payload) {
  const now = new Date().toISOString().substring(11, 23);
  const eventRecord = {
    timestamp: now,
    topic: topic,
    eventType: eventType,
    key: key,
    payload: payload
  };
  state.kafkaEvents.unshift(eventRecord);

  const countBadge = document.getElementById("kafkaEventCount");
  if (countBadge) countBadge.textContent = state.kafkaEvents.length;

  const terminalBody = document.getElementById("kafkaTerminalBody");
  if (!terminalBody) return;

  const row = document.createElement("div");
  row.className = "event-record";
  row.innerHTML = `
    <span class="time">[${now}]</span>
    <span class="topic">[TOPIC: ${topic}]</span>
    <span class="type">${eventType}</span>
    <span class="key">Key: ${key}</span>
    <div class="payload-preview">${JSON.stringify(payload, null, 2)}</div>
  `;
  terminalBody.insertBefore(row, terminalBody.firstChild);
}
