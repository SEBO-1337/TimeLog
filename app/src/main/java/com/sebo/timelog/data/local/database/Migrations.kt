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

