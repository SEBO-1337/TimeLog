# Firebase Setup (TimeLog)

## 1) `google-services.json` ablegen

1. Firebase Console -> Projekt -> Android-App (`com.sebo.timelog`) waehlen.
2. `google-services.json` herunterladen.
3. Datei hier ablegen:
   - `app/google-services.json`

Hinweis: Die Datei ist in `.gitignore` eingetragen und wird nicht mitcommittet.

## 2) Firestore aktivieren

1. In Firebase Console: Firestore Database erstellen (Native mode).
2. Region waehlen.

## 3) Firestore Rules (Dev)

Diese Regeln sind lokal in `firestore.rules` enthalten und erlauben den aktuellen Sync-Pfad (`shared/default/...`) fuer Entwicklung:

```txt
match /shared/default/{document=**} {
  allow read, write: if true;
}
```

Fuer Produktion sollten die Regeln auf Auth/UID eingeschraenkt werden.

## 4) App verifizieren

- App starten.
- In `Einstellungen` den Eintrag `Cloud-Sync (Firebase)` pruefen.
- Projekt/Worklog/Timer erstellen oder bearbeiten.
- In Firestore pruefen:
  - `shared/default/projects/*`
  - `shared/default/workLogs/*`
  - `shared/default/meta/activeTimer`

## 5) Optional: Regeln deployen (Firebase CLI)

```bash
firebase login
firebase use <dein-projekt-id>
firebase deploy --only firestore:rules
```

## 6) Firebase Auth aktivieren (Login/Registrierung)

1. Firebase Console -> Build -> Authentication -> "Get started".
2. Unter "Sign-in method" den Provider "E-Mail/Passwort" aktivieren.
3. App starten: Bei ausgeloggtem Zustand erscheinen Login/Registrierung automatisch.

Hinweis: Die App hat jetzt eigene Screens fuer Login und Registrierung integriert.

