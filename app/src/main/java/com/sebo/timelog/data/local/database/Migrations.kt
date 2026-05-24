package com.sebo.timelog.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration von Version 1 auf 2:
 * Fügt die Spalte `pausedAt` zur Tabelle `active_timers` hinzu.
 * Damit kann der exakte Elapsed-Wert beim Pausieren eingefroren werden,
 * sodass er nach App-Neustart korrekt wiederhergestellt wird.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE active_timers ADD COLUMN pausedAt INTEGER")
    }
}

/**
 * Migration von Version 2 auf 3:
 * Fügt die Spalte `createdBy` zur Tabelle `projects` hinzu (Firebase UID des Erstellers).
 * Dies ist notwendig für Rollen-basierte Filterung (Monteure sehen nur ihre Projects).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN createdBy TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_createdBy` ON `projects` (`createdBy`)")
    }
}

/**
 * Migration von Version 3 auf 4:
 * Fuegt `cloudId` als UUID-basierte Projekt-ID fuer den Cloud-Sync hinzu.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE projects SET cloudId = 'legacy-' || id WHERE cloudId = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_projects_cloudId` ON `projects` (`cloudId`)")
    }
}

/**
 * Migration von Version 4 auf 5:
 * Fuegt `cloudId` als UUID-basierte WorkLog-ID fuer den Cloud-Sync hinzu.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE work_logs ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE work_logs SET cloudId = 'legacy-' || id WHERE cloudId = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_work_logs_cloudId` ON `work_logs` (`cloudId`)")
    }
}

