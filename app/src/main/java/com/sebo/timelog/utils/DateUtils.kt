package com.sebo.timelog.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.GERMANY)

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))

    fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun todayEndMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun weekStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun monthStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun isToday(millis: Long): Boolean {
        val today = todayStartMillis()
        val tomorrow = today + 24 * 60 * 60 * 1000
        return millis in today until tomorrow
    }

    fun isThisWeek(millis: Long): Boolean {
        return millis >= weekStartMillis()
    }
}

