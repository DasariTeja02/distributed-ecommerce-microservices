const http = require('http');

http.get('http://localhost:9090/api/v1/targets', res => {
  let d = '';
  res.on('data', c => d += c);
  res.on('end', () => {
    const json = JSON.parse(d);
    json.data.activeTargets.forEach(t => {
      console.log(`Pool: ${t.scrapePool.padEnd(22)} | Health: ${t.health.padEnd(6)} | LastError: '${t.lastError}'`);
    });
  });
}).on('error', e => console.error(e));
