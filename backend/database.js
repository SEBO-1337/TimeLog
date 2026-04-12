/**
 * Einfacher JSON-basierter Datenspeicher – kein native Build nötig.
 * Daten werden in timelog-data.json gespeichert.
 */
const fs   = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, 'timelog-data.json');

let db = { projects: [], work_logs: [], active_timer: null };

// ── Laden ────────────────────────────────────────────────────────
try {
  if (fs.existsSync(DB_PATH)) {
    db = JSON.parse(fs.readFileSync(DB_PATH, 'utf8'));
    // Sicherstellen, dass alle Schlüssel vorhanden sind
    db.projects     = db.projects     ?? [];
    db.work_logs    = db.work_logs    ?? [];
    db.active_timer = db.active_timer ?? null;
    console.log(`💾  Daten geladen: ${db.projects.length} Projekte, ${db.work_logs.length} WorkLogs`);
  }
} catch (e) {
  console.warn('⚠️  DB-Datei konnte nicht gelesen werden, starte leer:', e.message);
}

// ── Speichern (synchron, nach jeder Änderung) ────────────────────
function save() {
  try {
    fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2), 'utf8');
  } catch (e) {
    console.error('❌  Speichern fehlgeschlagen:', e.message);
  }
}

module.exports = { db, save };
