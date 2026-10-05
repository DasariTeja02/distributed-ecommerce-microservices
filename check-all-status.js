const http = require('http');

const endpoints = [
  { name: 'Frontend', url: 'http://localhost:3000/' },
  { name: 'Obs Health API', url: 'http://localhost:3000/api/obs-health' },
  { name: 'Prometheus', url: 'http://localhost:9090/-/healthy' },
  { name: 'Grafana', url: 'http://localhost:3001/api/health' },
  { name: 'Zipkin', url: 'http://localhost:9411/zipkin/' },
  { name: 'Eureka', url: 'http://localhost:8761/' },
  { name: 'Config Server', url: 'http://localhost:8888/actuator/health' },
  { name: 'API Gateway', url: 'http://localhost:8080/actuator/health' },
  { name: 'Product Service', url: 'http://localhost:8081/actuator/health' },
  { name: 'Order Service', url: 'http://localhost:8082/actuator/health' },
  { name: 'Inventory Service', url: 'http://localhost:8083/actuator/health' },
  { name: 'Payment Service', url: 'http://localhost:8084/actuator/health' },
  { name: 'Notification Service', url: 'http://localhost:8085/actuator/health' }
];

async function check() {
  for (const ep of endpoints) {
    await new Promise(resolve => {
      const u = new URL(ep.url);
      const req = http.request({
        hostname: u.hostname,
        port: u.port,
        path: u.pathname + (u.search || ''),
        method: 'GET',
        timeout: 2000
      }, res => {
        res.resume();
        console.log(`[${res.statusCode}] ${ep.name} -> ${ep.url}`);
        resolve();
      });
      req.on('error', err => {
        console.log(`[ERR: ${err.code}] ${ep.name} -> ${ep.url}`);
        resolve();
      });
      req.on('timeout', () => {
        req.destroy();
        console.log(`[TIMEOUT] ${ep.name} -> ${ep.url}`);
        resolve();
      });
      req.end();
    });
  }
}

check();
