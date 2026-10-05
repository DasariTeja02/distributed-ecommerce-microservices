const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = __dirname;

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

const server = http.createServer(async (req, res) => {
  // Normalize URL path
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost:3000'}`);
  let reqPath = parsedUrl.pathname;

  // Real Observability Health Endpoint (immune to browser CORS)
  if (reqPath === '/api/obs-health') {
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'no-cache'
    });

    const checkService = (host, port, pathStr) => {
      return new Promise(resolve => {
        const start = Date.now();
        const clientReq = http.request({
          hostname: host,
          port: port,
          path: pathStr,
          method: 'GET',
          timeout: 2000
        }, clientRes => {
          clientRes.resume();
          const latency = Date.now() - start;
          const isUp = clientRes.statusCode >= 200 && clientRes.statusCode < 400;
          resolve({
            status: isUp ? 'UP' : 'DEGRADED',
            statusCode: clientRes.statusCode,
            latency: latency,
            checkedAt: new Date().toLocaleTimeString('en-GB')
          });
        });

        clientReq.on('timeout', () => {
          clientReq.destroy();
          resolve({ status: 'DOWN', statusCode: 0, latency: null, error: 'TIMEOUT' });
        });

        clientReq.on('error', err => {
          resolve({ status: 'DOWN', statusCode: 0, latency: null, error: err.code || err.message });
        });

        clientReq.end();
      });
    };

    const [prom, graf, zip] = await Promise.all([
      checkService('localhost', 9090, '/-/healthy'),
      checkService('localhost', 3001, '/api/health'),
      checkService('localhost', 9411, '/zipkin/')
    ]);

    const results = {
      prometheus: prom,
      grafana: graf,
      zipkin: zip
    };

    const onlineCount = Object.values(results).filter(r => r.status === 'UP').length;
    const totalCount = Object.keys(results).length;

    let stackState = 'UNAVAILABLE';
    let stackMessage = 'Observability stack unavailable';
    if (onlineCount === totalCount) {
      stackState = 'OPERATIONAL';
      stackMessage = 'Observability stack operational';
    } else if (onlineCount > 0) {
      stackState = 'PARTIAL';
      stackMessage = 'Partial observability availability';
    }

    res.end(JSON.stringify({
      timestamp: new Date().toLocaleTimeString('en-GB'),
      summary: {
        total: totalCount,
        online: onlineCount,
        state: stackState,
        message: stackMessage
      },
      services: results
    }));
    return;
  }

  if (reqPath === '/' || reqPath === '') {
    reqPath = '/index.html';
  }

  const filePath = path.join(PUBLIC_DIR, reqPath);
  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      if (err.code === 'ENOENT') {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
      } else {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end(`Server Error: ${err.code}`);
      }
    } else {
      res.writeHead(200, {
        'Content-Type': contentType,
        'Access-Control-Allow-Origin': '*'
      });
      res.end(content);
    }
  });
});

server.listen(PORT, () => {
  console.log(`Frontend Control Center running at http://localhost:${PORT}/`);
});
