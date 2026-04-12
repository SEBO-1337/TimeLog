package com.sebo.timelog.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormatter {

    /**
     * Formatiert Millisekunden in HH:MM:SS Format
     */
    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Formatiert Millisekunden in HH:MM Format (ohne Sekunden)
     */
    fun formatDurationShort(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    /**
     * Formatiert Dezimalstunden in lesbaren String
     * z.B. 1.5 -> "1h 30min"
     */
    fun formatHoursDecimal(hours: Double): String {
        val h = hours.toInt()
        val min = ((hours - h) * 60).toInt()
        return when {
            h > 0 && min > 0 -> "${h}h ${min}min"
            h > 0 -> "${h}h"
            min > 0 -> "${min}min"
            else -> "0min"
        }
    }

    /**
     * Formatiert Dezimalstunden in "X,X h" Format
     */
    fun formatHoursShort(hours: Double): String {
        return String.format(Locale.getDefault(), "%.1f h", hours)
    }

    /**
     * Konvertiert Millisekunden in Dezimalstunden
     */
    fun millisToHours(millis: Long): Double {
        return millis.toDouble() / (1000 * 60 * 60)
    }

    /**
     * Konvertiert Dezimalstunden in Millisekunden
     */
    fun hoursToMillis(hours: Double): Long {
        return (hours * 1000 * 60 * 60).toLong()
    }
}

