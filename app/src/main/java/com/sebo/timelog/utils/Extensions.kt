package com.sebo.timelog.utils

import android.app.Activity
import android.content.Context
import androidx.compose.ui.graphics.Color
import com.sebo.timelog.TimeLogApplication
import com.sebo.timelog.di.AppContainer

/**
 * Extension für einfachen Zugriff auf den AppContainer
 */
val Context.appContainer: AppContainer
    get() = (applicationContext as TimeLogApplication).container

/**
 * Konvertiert einen Hex-Farbstring in eine Compose Color
 */
fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF2196F3) // Default Blau
    }
}

/**
 * Formatiert einen Long-Timestamp in lesbare Zeitanzeige
 */
fun Long.toFormattedTime(): String = TimeFormatter.formatDuration(this)

/**
 * Formatiert einen Long-Timestamp in lesbares Datum
 */
fun Long.toFormattedDate(): String = DateUtils.formatDate(this)

/**
 * Formatiert Dezimalstunden
 */
fun Double.toFormattedHours(): String = TimeFormatter.formatHoursDecimal(this)

/**
 * Formatiert einen Geldbetrag
 */
fun Double.toCurrencyString(): String = String.format("%.2f €", this)

