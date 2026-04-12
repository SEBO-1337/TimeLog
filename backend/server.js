const express = require('express');
const cors    = require('cors');
const path    = require('path');
const os      = require('os');

const app  = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Statische Web-Dateien (Dashboard)
app.use(express.static(path.join(__dirname, '../web')));

// API-Routen
app.use('/api/projects', require('./routes/projects'));
app.use('/api/worklogs', require('./routes/worklogs'));
app.use('/api/timer',    require('./routes/timer'));

// Health-Check
app.get('/api/health', (_req, res) => res.json({ status: 'ok', time: Date.now() }));

// Alle anderen Routen → Web-App (SPA)
app.get('*', (_req, res) =>
  res.sendFile(path.join(__dirname, '../web/index.html'))
);

// Lokale IP-Adressen ermitteln
function getLocalIPs() {
  return Object.values(os.networkInterfaces())
    .flat()
    .filter(i => i.family === 'IPv4' && !i.internal)
    .map(i => i.address);
}

app.listen(PORT, '0.0.0.0', () => {
  const ips = getLocalIPs();
  console.log('\n✅  TimeLog Server gestartet!\n');
  console.log(`   Lokal:        http://localhost:${PORT}`);
  ips.forEach(ip => console.log(`   Im Netzwerk:  http://${ip}:${PORT}  ← Diese URL für Freddy & Android`));
  console.log('\n   Trage die Netzwerk-URL in local.properties ein:');
  console.log(`   timelog.server.url=http://${ips[0] ?? 'DEINE_IP'}:${PORT}\n`);
});

