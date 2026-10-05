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
      let b = '';
      res.on('data', c => b += c);
      res.on('end', () => resolve(JSON.parse(b).token));
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
      let b = '';
      res.on('data', c => b += c);
      res.on('end', () => resolve(JSON.parse(b)));
    });
    req.on('error', reject);
    req.end();
  });
}

async function listOrders(token) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: 'localhost',
      port: 8080,
      path: `/api/orders`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    }, res => {
      let b = '';
      res.on('data', c => b += c);
      res.on('end', () => resolve(JSON.parse(b)));
    });
    req.on('error', reject);
    req.end();
  });
}

async function main() {
  const token = await login();
  console.log('Login OK');

  const orders = await listOrders(token);
  console.log(`Total orders in PostgreSQL database: ${orders.length}`);
  for (const o of orders) {
    console.log(`Order ${o.id}: Status=${o.status}, Amount=₹${o.totalAmount}, Customer=${o.customerId}`);
  }
}

main().catch(console.error);
