package com.sebo.timelog.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import com.sebo.timelog.TimeLogApplication
import com.sebo.timelog.di.AppContainer
import androidx.core.graphics.toColorInt

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
        Color(this.toColorInt())
    } catch (_: Exception) {
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
@SuppressLint("DefaultLocale")
fun Double.toCurrencyString(): String = String.format("%.2f €", this)

