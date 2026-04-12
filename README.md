# TimeLog

TimeLog ist eine Zeiterfassungs-Loesung mit drei Teilen:
- **Android-App** (`app/`) fuer Projekte, Timer, WorkLogs und Abrechnung
- **Backend** (`backend/`) als lokale REST-API fuer Sync und Dashboard-Zugriff
- **Web/PWA** (`web/`) fuer Browserzugriff, Hosting ueber Firebase

## Architektur (kurz)

- Android speichert lokal (Room) und synchronisiert Daten.
- Backend stellt API-Endpunkte bereit (Express).
- Web liest Daten aus Firebase (Auth + Firestore) und kann ueber Firebase Hosting ausgeliefert werden.

Weiterfuehrende Doku:
- Firebase Setup: `FIREBASE_SETUP.md`
- Backend Details: `backend/README.md`

## Voraussetzungen

- **Android Studio** (aktuelle Version)
- **JDK 11**
- **Android SDK**: `compileSdk 36`, `targetSdk 36`, `minSdk 26`
- **Node.js 18+** und npm (fuer `backend/`)
- Optional: **Firebase CLI** (`npm install -g firebase-tools`)

## Projektstruktur

```text
TimeLog/
|- app/                 Android App (Jetpack Compose, Room, WorkManager, Firebase)
|- backend/             Express Server + lokale Datenhaltung fuer Dashboard/API
|- web/                 Web-Frontend/PWA
|- FIREBASE_SETUP.md    Firebase/Auth/Rules Setup
|- firebase.json        Firebase Hosting + Firestore Konfiguration
|- firestore.rules      Firestore Security Rules
```

## Schnellstart (Windows / PowerShell)

### 1) Backend starten

```powershell
Set-Location "C:\Users\sdend\Android\TimeLog\backend"
npm install
npm start
```

### 2) Android-App konfigurieren

1. Stelle sicher, dass `app/google-services.json` vorhanden ist.
2. Trage in `local.properties` bei Bedarf die Server-URL ein:

```properties
timelog.server.url=http://<deine-ip>:3000
```

3. Projekt in Android Studio oeffnen und App bauen/starten.

### 3) Web/Firebase (optional)

Wenn du Firebase Hosting oder Rules deployen willst:

```powershell
Set-Location "C:\Users\sdend\Android\TimeLog"
firebase login
firebase use <dein-projekt-id>
firebase deploy --only firestore:rules
firebase deploy --only hosting
```

## Wichtige Konfigurationsdateien

- `app/google-services.json`: Firebase Android Config (sensibel, nicht committen)
- `local.properties`: lokale Pfade/URLs (nicht committen)
- `firestore.rules`: Firestore Zugriffsregeln
- `firebase.json`: Hosting/Deployment Konfiguration

## Entwicklungshinweise

- Android Build/Dependencies: `app/build.gradle.kts`
- Backend Scripts: `backend/package.json`
- Web Assets fuer Hosting: `web/`

## Troubleshooting (kurz)

- **Backend nicht erreichbar:** IP/Port pruefen, Firewall pruefen, beide Geraete im gleichen Netzwerk.
- **Android sync nicht:** `timelog.server.url` in `local.properties` pruefen, App neu bauen.
- **Keine Web-Daten sichtbar:** Firebase Auth/Rules und Firestore Pfade gemaess `FIREBASE_SETUP.md` pruefen.

