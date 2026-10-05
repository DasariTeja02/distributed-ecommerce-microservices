const http = require('http');

async function login() {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify({ username: 'admin', password: 'admin123' });
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/auth/login',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data)
      }
    }, res => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const json = JSON.parse(body);
          resolve(json.token);
        } else {
          reject(new Error(`Login failed (${res.statusCode}): ${body}`));
        }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

function listenSSE(onEvent) {
  const req = http.request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/orders/events/stream',
    method: 'GET',
    headers: { 'Accept': 'text/event-stream' }
  }, res => {
    let buffer = '';
    res.on('data', chunk => {
      buffer += chunk.toString();
      const lines = buffer.split('\n\n');
      buffer = lines.pop();
      for (const line of lines) {
        if (line.trim()) {
          onEvent(line);
        }
      }
    });
  });
  req.on('error', err => console.error('[SSE Error]', err.message));
  req.end();
  return req;
}

async function postOrder(token, payload) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(payload);
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/orders',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data),
        'Authorization': `Bearer ${token}`
      }
    }, res => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(JSON.parse(body));
        } else {
          reject(new Error(`Post order failed (${res.statusCode}): ${body}`));
        }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function getOrder(token, orderId) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: `/api/orders/${orderId}`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    }, res => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(JSON.parse(body));
        } else {
          reject(new Error(`Get order failed (${res.statusCode}): ${body}`));
        }
      });
    });
    req.on('error', reject);
    req.end();
  });
}

async function main() {
  console.log('--- 1. AUTHENTICATING ---');
  const token = await login();
  console.log('Login successful. Token acquired.');

  console.log('\n--- 2. CONNECTING TO SSE EVENT STREAM (:8080) ---');
  const capturedEvents = [];
  const sseReq = listenSSE(eventStr => {
    console.log('[SSE EVENT RECEIVED]:', eventStr.replace(/\n/g, ' '));
    capturedEvents.push(eventStr);
  });

  // Give SSE a moment to establish handshake
  await new Promise(r => setTimeout(r, 1000));

  console.log('\n--- 3. TESTING HAPPY PATH SAGA ---');
  const happyOrder = await postOrder(token, {
    customerId: 'admin@domain.io',
    items: [{ productId: 'prod-102', quantity: 1, price: 99.99 }]
  });
  console.log(`Happy order created: ID=${happyOrder.id}, Initial Status=${happyOrder.status}`);

  console.log('Waiting for Saga choreography completion (3 seconds)...');
  await new Promise(r => setTimeout(r, 3500));

  const happyOrderFinal = await getOrder(token, happyOrder.id);
  console.log(`>>> HAPPY ORDER FINAL STATUS IN DATABASE: ${happyOrderFinal.status} <<<`);

  console.log('\n--- 4. TESTING PAYMENT FAILURE COMPENSATION SAGA ---');
  const failOrder = await postOrder(token, {
    customerId: 'admin@domain.io',
    items: [{ productId: 'prod-102', quantity: 1, price: 9999.99 }]
  });
  console.log(`Payment-fail order created: ID=${failOrder.id}, Initial Status=${failOrder.status}`);

  console.log('Waiting for Compensation flow completion (3 seconds)...');
  await new Promise(r => setTimeout(r, 3500));

  const failOrderFinal = await getOrder(token, failOrder.id);
  console.log(`>>> FAILED ORDER FINAL STATUS IN DATABASE: ${failOrderFinal.status} (Reason: ${failOrderFinal.failureReason}) <<<`);

  console.log('\n--- 5. SUMMARY OF CAPTURED EVENTS ---');
  console.log(`Total events captured: ${capturedEvents.length}`);

  sseReq.destroy();
  process.exit(0);
}

main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
