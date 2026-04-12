# TimeLog - Bauplan für die Zeiterfassungs-App

**Datum:** April 2026  
**Projekt:** TimeLog - Professionelle Zeiterfassung für Projekte  
**Package:** `com.sebo.timelog`  
**Zielversion:** 1.0  

---

## 📋 Inhaltsverzeichnis

1. [Projekt-Übersicht](#projekt-übersicht)
2. [App-Struktur](#app-struktur)
3. [Feature-List](#feature-list)
4. [Datenbankschema](#datenbankschema)
5. [Technischer Stack](#technischer-stack)
6. [Architektur-Pattern](#architektur-pattern)
7. [Implementierungs-Reihenfolge](#implementierungs-reihenfolge)
8. [Schnittstellen & Integrationen](#schnittstellen--integrationen)

---

## 🎯 Projekt-Übersicht

### Zweck
TimeLog ist eine spezialisierte Android-App für professionelle Zeiterfassung von Projektarbeit. Sie soll als eigenständige App neben der Notes-App existieren und ermöglicht es Nutzern, ihre Arbeitszeiten zu tracken, zu verwalten und später abzurechnen.

### Zielgruppe
- Freiberufler
- Projektmanager
- Agenturen
- Teams mit Stundenabrechnung

### Kern-Funktionalität
- ⏱️ Zeitmessung per Timer/Stopwatch
- 📊 Protokollierung von Arbeitszeiten
- 💼 Projekt-Management
- 📈 Reports und Statistiken
- 💰 Abrechnungs-Vorbereitung

---

## 📁 App-Struktur

```
TimeLog/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sebo/timelog/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── database/
│   │   │   │   │   │   │   ├── TimeLogDatabase.kt
│   │   │   │   │   │   │   └── Migrations.kt
│   │   │   │   │   │   ├── entities/
│   │   │   │   │   │   │   ├── Project.kt
│   │   │   │   │   │   │   ├── WorkLog.kt
│   │   │   │   │   │   │   ├── Timer.kt
│   │   │   │   │   │   │   └── ProjectStats.kt (DTO)
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── ProjectDao.kt
│   │   │   │   │   │   │   ├── WorkLogDao.kt
│   │   │   │   │   │   │   └── TimerDao.kt
│   │   │   │   │   │   └── relations/
│   │   │   │   │   │       ├── ProjectWithWorkLogs.kt
│   │   │   │   │   │       └── ProjectWithStats.kt
│   │   │   │   │   ├── repositories/
│   │   │   │   │   │   ├── ProjectRepository.kt
│   │   │   │   │   │   ├── WorkLogRepository.kt
│   │   │   │   │   │   └── TimerRepository.kt
│   │   │   │   │   └── preferences/
│   │   │   │   │       └── UserPreferences.kt (DataStore)
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   │   └── Screens.kt
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── timer/
│   │   │   │   │   │   │   ├── TimerScreen.kt
│   │   │   │   │   │   │   ├── TimerViewModel.kt
│   │   │   │   │   │   │   ├── TimerViewModelFactory.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   │       ├── StopWatch.kt
│   │   │   │   │   │   │       ├── ProjectSelector.kt
│   │   │   │   │   │   │       └── TimerControls.kt
│   │   │   │   │   │   ├── history/
│   │   │   │   │   │   │   ├── HistoryScreen.kt
│   │   │   │   │   │   │   ├── HistoryViewModel.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   │       ├── WorkLogItem.kt
│   │   │   │   │   │   │       └── WorkLogFilters.kt
│   │   │   │   │   │   ├── projects/
│   │   │   │   │   │   │   ├── ProjectsScreen.kt
│   │   │   │   │   │   │   ├── ProjectsViewModel.kt
│   │   │   │   │   │   │   ├── ProjectDetailScreen.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   │       ├── ProjectList.kt
│   │   │   │   │   │   │       ├── ProjectCard.kt
│   │   │   │   │   │   │       └── ProjectForm.kt
│   │   │   │   │   │   ├── statistics/
│   │   │   │   │   │   │   ├── StatisticsScreen.kt
│   │   │   │   │   │   │   ├── StatisticsViewModel.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   │       ├── Charts.kt
│   │   │   │   │   │   │       └── StatsSummary.kt
│   │   │   │   │   │   └── settings/
│   │   │   │   │   │       ├── SettingsScreen.kt
│   │   │   │   │   │       └── SettingsViewModel.kt
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Type.kt
│   │   │   │   │   │   └── Theme.kt
│   │   │   │   │   └── components/
│   │   │   │   │       ├── TopAppBars.kt
│   │   │   │   │       ├── BottomNavigation.kt
│   │   │   │   │       ├── Dialogs.kt
│   │   │   │   │       └── Common.kt
│   │   │   │   │
│   │   │   │   ├── services/
│   │   │   │   │   ├── TimerService.kt (Foreground Service)
│   │   │   │   │   ├── NotificationManager.kt
│   │   │   │   │   └── WorkerTasks.kt (Worker für Background)
│   │   │   │   │
│   │   │   │   ├── utils/
│   │   │   │   │   ├── TimeFormatter.kt
│   │   │   │   │   ├── DateUtils.kt
│   │   │   │   │   ├── Constants.kt
│   │   │   │   │   └── Extensions.kt
│   │   │   │   │
│   │   │   │   ├── di/
│   │   │   │   │   ├── AppContainer.kt
│   │   │   │   │   └── RepositoryModule.kt
│   │   │   │   │
│   │   │   │   └── MainActivity.kt
│   │   │   │
│   │   │   ├── AndroidManifest.xml
│   │   │   └── res/
│   │   │       ├── values/
│   │   │       │   ├── strings.xml
│   │   │       │   ├── colors.xml
│   │   │       │   └── themes.xml
│   │   │       ├── layout/
│   │   │       ├── drawable/
│   │   │       └── menu/
│   │   │
│   │   ├── test/
│   │   │   └── java/com/sebo/timelog/
│   │   │       ├── utils/
│   │   │       ├── viewmodels/
│   │   │       └── repositories/
│   │   │
│   │   └── androidTest/
│   │       └── java/com/sebo/timelog/
│   │           ├── ui/
│   │           └── dao/
│   │
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── ...
│
├── gradle/
│   └── libs.versions.toml
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md
└── ARCHITECTURE.md
```

---

## ✨ Feature-List

### Phase 1: MVP (Minimum Viable Product)
- [ ] Projekte erstellen/bearbeiten/löschen
- [ ] Timer starten/pausieren/stoppen
- [ ] WorkLog automatisch speichern
- [ ] Beschreibung zu WorkLog hinzufügen
- [ ] WorkLog-Verlauf anzeigen (History)
- [ ] Basis-Statistiken (Stunden pro Projekt)
- [ ] Foreground Service für Timer
- [ ] Material 3 Design

### Phase 2: Standard
- [ ] Abrechnung markieren (billable/non-billable)
- [ ] Filter & Suchfunktion für WorkLogs
- [ ] Export zu CSV/PDF
- [ ] Benachrichtigungen bei langen Sessions
- [ ] Dark Mode
- [ ] App-Widget für Quick-Start
- [ ] Lokale Datenbank-Backups

### Phase 3: Premium
- [ ] Cloud-Sync (Firebase)
- [ ] Team-Funktionalität
- [ ] REST-API Integration
- [ ] Rechnungsgenerierung
- [ ] Zeiterfassungs-Vorlagen
- [ ] Kategorisierung von Tasks
- [ ] Erweiterte Berichte

---

## 🗄️ Datenbankschema

### Entity: Project
```sql
CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    color TEXT DEFAULT "#2196F3",  -- Hex-Farbe für UI
    hourlyRate REAL DEFAULT 0.0,   -- Stundensatz für Abrechnung
    status TEXT DEFAULT "ACTIVE",  -- ACTIVE, PAUSED, ARCHIVED
    createdAt LONG NOT NULL,
    updatedAt LONG NOT NULL
);
```

### Entity: WorkLog
```sql
CREATE TABLE work_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    projectId INTEGER NOT NULL,
    description TEXT DEFAULT "",
    hoursWorked REAL NOT NULL,        -- z.B. 1.5 (1h 30min)
    hoursBilled REAL DEFAULT 0.0,     -- Abgerechnete Stunden
    date LONG NOT NULL,                -- Datum der Arbeitszeit
    startTime LONG,                    -- Unix-Timestamp des Starts
    endTime LONG,                      -- Unix-Timestamp des Endes
    billableStatus TEXT DEFAULT "UNBILLED",  -- UNBILLED, BILLED, PARTIAL
    notes TEXT,
    tags TEXT,                         -- JSON Array oder komma-separiert
    createdAt LONG NOT NULL,
    updatedAt LONG NOT NULL,
    FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE CASCADE
);
```

### Entity: Timer (für laufende Timer)
```sql
CREATE TABLE active_timers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    projectId INTEGER NOT NULL,
    isRunning BOOLEAN DEFAULT 1,
    startTime LONG NOT NULL,
    pausedDuration LONG DEFAULT 0,     -- Nur Pausierungszeiten
    description TEXT,
    FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE CASCADE
);
```

### Relation: ProjectWithStats (DTO)
```kotlin
data class ProjectWithStats(
    val project: Project,
    val totalHours: Double,
    val billedHours: Double,
    val pendingHours: Double,
    val estimatedRevenue: Double,
    val workLogCount: Int,
    val lastActivityDate: Long?
)
```

### Indizes
```sql
CREATE INDEX idx_work_logs_project ON work_logs(projectId);
CREATE INDEX idx_work_logs_date ON work_logs(date);
CREATE INDEX idx_work_logs_billable ON work_logs(billableStatus);
CREATE INDEX idx_active_timers_project ON active_timers(projectId);
```

---

## 🛠️ Technischer Stack

### Android SDK
```gradle
compileSdk = 36
minSdk = 24
targetSdk = 36
```

### Kernel-Dependencies
```gradle
// Kotlin
kotlin = "2.1.10"

// Android
androidx.core.ktx = "1.15.0"
androidx.lifecycle = "2.8.7"
androidx.activity.compose = "1.9.3"

// Compose
androidx.compose.bom = "2024.12.01"
androidx.compose.ui = "1.8.0"
androidx.compose.material3 = "1.3.1"
androidx.compose.material.icons = "1.8.0"

// Room & Data
androidx.room = "2.6.1"
androidx.datastore = "1.1.2"

// Navigation
androidx.navigation.compose = "2.8.7"

// Coroutines
kotlinx.coroutines = "1.9.1"

// WorkManager für Background Tasks
androidx.work = "2.10.1"

// Testing
junit = "4.13.2"
androidx.test.junit = "1.2.1"
androidx.test.espresso = "3.6.1"
```

### Build-Features
```gradle
buildFeatures {
    compose = true
    buildConfig = true
}

composeOptions {
    kotlinCompilerExtensionVersion = "2.1.10"
}
```

---

## 🏗️ Architektur-Pattern

### MVVM (Model-View-ViewModel)
Jeder Screen hat:
1. **Screen Composable** - UI-Darstellung
2. **ViewModel** - State Management & Business Logic
3. **Repository** - Datenzugriff
4. **Entity/DAO** - Datenbankmodelle

### Dependency Injection
```kotlin
// app/NotesApplication.kt
class TimeLogApplication : Application() {
    lateinit var container: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

// di/AppContainer.kt
class AppContainer(private val context: Context) {
    private val database = TimeLogDatabase.getInstance(context)
    
    val projectRepository = ProjectRepository(database.projectDao())
    val workLogRepository = WorkLogRepository(database.workLogDao())
    val timerRepository = TimerRepository(database.timerDao())
}
```

### Navigation
```kotlin
// Sealed Classes für typsichere Navigation
sealed class Screens(val route: String) {
    data object Timer : Screens("timer")
    data object Projects : Screens("projects")
    data object History : Screens("history")
    data object Statistics : Screens("statistics")
    data object Settings : Screens("settings")
    
    data class ProjectDetail(val projectId: Long) : 
        Screens("project_detail/{projectId}") {
        fun createRoute(id: Long) = "project_detail/$id"
    }
}
```

### State Management
```kotlin
// Beispiel ViewModel
class TimerViewModel(
    private val timerRepository: TimerRepository,
    private val workLogRepository: WorkLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TimerUiState>(TimerUiState.Idle)
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    
    // StateFlow für Echtzeit-Updates
    val currentTimer: StateFlow<Timer?> = timerRepository
        .getActiveTimer()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
}

sealed class TimerUiState {
    data object Idle : TimerUiState()
    data object Running : TimerUiState()
    data object Paused : TimerUiState()
    data class Error(val message: String) : TimerUiState()
}
```

---

## 📋 Implementierungs-Reihenfolge

### Sprint 1: Foundation (Woche 1-2)
1. **Projekt-Setup**
   - New Android App in Android Studio
   - Gradle-Dependencies einrichten
   - App-Struktur erstellen

2. **Datenbank**
   - `Project.kt` Entity
   - `WorkLog.kt` Entity
   - `Timer.kt` Entity
   - DAOs implementieren
   - `TimeLogDatabase.kt`

3. **Repositories**
   - `ProjectRepository`
   - `WorkLogRepository`
   - `TimerRepository`
   - Dependency Injection Setup

### Sprint 2: UI Grundlagen (Woche 3-4)
1. **Theme & Design**
   - Material 3 Colors
   - Typographie
   - Icons

2. **Navigation**
   - BottomNavigation aufbauen
   - Screen-Struktur

3. **ProjectsScreen**
   - Liste alle Projekte
   - Projekt erstellen
   - Projekt bearbeiten
   - Projekt löschen

### Sprint 3: Timer-Funktion (Woche 5-6)
1. **TimerScreen**
   - Stopwatch-UI
   - Start/Pause/Stop Buttons
   - Projekt-Auswahl
   - Beschreibungs-Feld

2. **TimerService**
   - Foreground Service
   - Timer-Logik
   - Benachrichtigungen
   - Persistenz bei App-Neustart

3. **WorkLog speichern**
   - Dialog nach Stopp
   - Daten in DB schreiben

### Sprint 4: History & Analytics (Woche 7-8)
1. **HistoryScreen**
   - WorkLog-Liste
   - Filter (Projekt, Datum)
   - WorkLog bearbeiten/löschen

2. **StatisticsScreen**
   - Stunden pro Projekt
   - Einnahmen-Übersicht
   - Grafiken (Charts)
   - Zeitraum-Filter

### Sprint 5: Polish & Testing (Woche 9-10)
1. **Settings**
   - Sprache
   - Benachrichtigungseinstellungen
   - Datenmanagement

2. **Testing**
   - Unit Tests für ViewModels
   - UI Tests für Screens
   - Integration Tests für Repositories

3. **Bug Fixes & Optimierung**

---

## 🔗 Schnittstellen & Integrationen

### 1. Interner Service (TimerService)
```kotlin
// Timer starten
WidgetTimerService.startService(context, projectId)

// Timer beenden
WidgetTimerService.stopService(context, projectId)

// Status abfragen
val elapsedSeconds = WidgetTimerService.getElapsedSeconds(context)
```

### 2. Intents (für Widget/Shortcuts)
```kotlin
// TimeLog-App vom Home-Screen starten mit Projekt
Intent("com.sebo.timelog.START_TIMER").apply {
    putExtra("projectId", projectId)
    startActivity(this)
}

// WorkLog speichern & History anzeigen
Intent("com.sebo.timelog.SHOW_HISTORY").apply {
    startActivity(this)
}
```

### 3. Content Provider (für Notes-App)
```kotlin
// Notes-App kann WorkLogs auslesen
val uri = Uri.parse("content://com.sebo.timelog/worklogs")
val cursor = contentResolver.query(uri, null, "projectId=?", arrayOf(projectId), null)
```

### 4. Notification Channels
```kotlin
// Timer läuft im Hintergrund - Benachrichtigung
NotificationManager.IMPORTANCE_LOW
Channel ID: "timer_channel"

// Erinnerungen für lange Sessions
NotificationManager.IMPORTANCE_DEFAULT  
Channel ID: "reminders_channel"
```

### 5. SharedPreferences / DataStore
```kotlin
// Für schnelle Einstellungen
dataStore.edit { preferences ->
    preferences[LAST_SELECTED_PROJECT] = projectId
    preferences[NOTIFICATION_ENABLED] = true
}
```

---

## 🚀 Best Practices

### Code-Stil
- Kotlin Coroutines für Async-Operationen
- Flow für reaktive Datenströme
- Sealed Classes für typsichere Zustände
- No-Arg Konstruktoren für Entities (Room-Anforderung)

### Performance
- Pagination für große WorkLog-Listen
- Lazy Loading der Projekte
- Index auf häufigen Query-Spalten
- WorkManager für Background Tasks

### Sicherheit
- Keine sensitiven Daten in Logs
- Validierung aller User-Inputs
- Sichere lokale Speicherung
- Permissions-Handling

### Testing
- Unit Tests für ViewModels (90% Coverage)
- Integration Tests für Repositories
- UI Tests für kritische Flows
- Mocking von Repositories in Tests

---

## 📱 Berechtigungen (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

---

## 🔄 Migration aus Notes-App

Falls bereits WorkLog-Daten in der Notes-App existieren:

1. **Export aus Notes-DB**
   ```kotlin
   val oldWorkLogs = notesDatabase.workLogDao().getAllWorkLogs()
   ```

2. **Import in TimeLog-DB**
   ```kotlin
   timeLogDatabase.workLogDao().insertAll(oldWorkLogs)
   ```

3. **Daten-Validation**
   - Alle Projekte importieren
   - Alle WorkLogs mit Fremdschlüsseln verlinken
   - Duplikate prüfen

---

## 📊 Metriken & Success-Kriterien

### Performance
- App-Start < 2 Sekunden
- Timer-Genauigkeit: ±0,5 Sekunden
- Datenbank-Queries < 200ms

### Usability
- Timer-Start mit 1 Tap
- WorkLog in 10 Sekunden erstellbar
- Alle Features in 3-4 Screen erreichbar

### Qualität
- 0 Critical Bugs bei Release
- 95%+ erfolgreiche Timer-Speicherungen
- Keine Crashes in Production

---

## 📞 Kontakt & Support

Bei Fragen zur Architektur oder Implementierung:
- Siehe `ARCHITECTURE.md` für tiefere Details
- Siehe `README.md` für Entwickler-Setup
- Code-Kommentare für komplexe Logik

**Viel Erfolg beim Entwickeln!** 🎉


