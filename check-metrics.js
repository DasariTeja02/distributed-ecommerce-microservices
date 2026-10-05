const http = require('http');

[8080, 8081, 8082, 8083, 8084, 8085].forEach(port => {
  http.get(`http://localhost:${port}/actuator/prometheus`, res => {
    let len = 0;
    res.on('data', chunk => len += chunk.length);
    res.on('end', () => {
      console.log(`Port ${port} /actuator/prometheus -> ${res.statusCode} (${len} bytes)`);
    });
  }).on('error', e => console.log(`Port ${port} error: ${e.message}`));
});
