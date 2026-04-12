# TimeLog – Web-Dashboard Backend

## Was macht das hier?

Dieser Server läuft auf deinem **PC** und stellt:
1. Eine **REST-API** bereit, die Daten von der Android-App empfängt
2. Ein **Web-Dashboard** für Freddy's iPhone aus

---

## ⚡ Schnellstart (3 Schritte)

### Schritt 1 – Einmalig: Abhängigkeiten installieren

```bash
cd backend
npm install
```

### Schritt 2 – Server starten

```bash
npm start
```

Du siehst dann so etwas:

```
✅  TimeLog Server gestartet!

   Lokal:        http://localhost:3000
   Im Netzwerk:  http://192.168.1.42:3000  ← Diese URL für Freddy & Android
```

> **Tipp:** Nutze `npm run dev` statt `npm start` für Auto-Neustart bei Änderungen.

---

### Schritt 3 – URL eintragen

**In `../local.properties`** (einmal ändern, dann Android-App neu bauen):

```properties
timelog.server.url=http://192.168.1.42:3000
```

Danach in Android Studio: **Build → Rebuild Project**

**Für Freddy's iPhone:** Safari öffnen → `http://192.168.1.42:3000`  
Dann: **Teilen → Zum Home-Bildschirm** → sieht aus wie eine native App ✅

---

## 📱 Dashboard-Features

| Feature | Beschreibung |
|---|---|
| **Aktiver Timer** | Live-Anzeige des laufenden Timers (ticking) |
| **Übersicht** | Stunden heute / diese Woche / diesen Monat |
| **Projekte** | Alle Projekte mit Gesamtstunden & Wert |
| **Verlauf** | Alle WorkLogs, filterbar nach Projekt & Datum |
| **Auto-Refresh** | Alle 30 Sek. werden Daten automatisch aktualisiert |
| **PWA** | Auf dem iPhone installierbar ("Add to Home Screen") |

---

## 🌐 API-Endpunkte

```
GET  /api/health                → Server-Status
GET  /api/projects              → Alle Projekte
POST /api/projects/sync         → Projekt von Android hinzufügen/aktualisieren
DEL  /api/projects/:id          → Projekt löschen

GET  /api/worklogs              → WorkLogs (Query: projectId, from, to, limit)
GET  /api/worklogs/stats/summary→ Schnell-Statistiken
POST /api/worklogs/sync         → WorkLog von Android hinzufügen/aktualisieren
DEL  /api/worklogs/:id          → WorkLog löschen

GET  /api/timer                 → Aktiver Timer
POST /api/timer/sync            → Timer-Status von Android speichern
DEL  /api/timer                 → Timer löschen (gestoppt)
```

---

## 🔧 Konfiguration

| Variable | Default | Beschreibung |
|---|---|---|
| `PORT` | `3000` | Server-Port |

Beispiel mit anderem Port:
```bash
PORT=8080 npm start
```

---

## 💾 Datenbank

Die Daten werden lokal in `backend/timelog.db` (SQLite) gespeichert.  
Die Android-App ist die **einzige Quelle** – der Server speichert nur eine Kopie für das Web-Dashboard.

---

## 🔄 Wie funktioniert der Sync?

```
Android-App ──(HTTP POST)──► Server (PC) ──(HTTP GET)──► Freddy's iPhone
     │                           │
     │  bei jedem:               │  SQLite-Datenbank
     │  - Projekt erstellen      │  (timelog.db)
     │  - WorkLog speichern      │
     │  - Timer start/stop       │
     └───────────────────────────┘
```

Die Android-App sendet Daten automatisch nach jeder Aktion.  
Beim App-Start werden **alle vorhandenen Daten** einmalig synchronisiert.

---

## ❓ Probleme?

| Problem | Lösung |
|---|---|
| iPhone kann Dashboard nicht öffnen | Beide im gleichen WLAN? Firewall auf PC prüfen |
| Android synchronisiert nicht | IP in `local.properties` korrekt? App neu gebaut? |
| Daten fehlen auf dem Dashboard | Android-App einmal starten → initialer Sync läuft |
| Server startet nicht | `node --version` prüfen (braucht Node.js 18+) |

