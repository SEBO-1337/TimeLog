package com.sebo.timelog.utils

import com.sebo.timelog.data.local.entities.BillableStatus
import com.sebo.timelog.data.local.entities.WorkLog

fun WorkLog.effectiveBilledHours(): Double {
    val workedHours = hoursWorked.coerceAtLeast(0.0)
    // Keine Obergrenze – Überzahlung (Minusstunden) ist erlaubt
    val explicitBilledHours = hoursBilled.coerceAtLeast(0.0)

    return if (billableStatus == BillableStatus.BILLED) {
        if (explicitBilledHours > 0.0) explicitBilledHours else workedHours
    } else {
        explicitBilledHours
    }
}

fun WorkLog.pendingHours(): Double {
    // Kann negativ werden, wenn mehr abgerechnet wurde als gearbeitet
    return hoursWorked.coerceAtLeast(0.0) - effectiveBilledHours()
}

fun WorkLog.resolvedBillableStatus(): BillableStatus {
    val billedHours = effectiveBilledHours()
    return when {
        billedHours <= 0.0 -> BillableStatus.UNBILLED
        pendingHours() <= 0.0001 -> BillableStatus.BILLED
        else -> BillableStatus.PARTIAL
    }
}

fun WorkLog.withBilledHours(
    billedHours: Double,
    updatedAt: Long = System.currentTimeMillis()
): WorkLog {
    val normalizedWorkedHours = hoursWorked.coerceAtLeast(0.0)
    // Keine Obergrenze – Überzahlung erlaubt
    val normalizedBilledHours = billedHours.coerceAtLeast(0.0)
    val status = when {
        normalizedBilledHours <= 0.0 -> BillableStatus.UNBILLED
        normalizedWorkedHours <= 0.0 || normalizedBilledHours >= normalizedWorkedHours -> BillableStatus.BILLED
        else -> BillableStatus.PARTIAL
    }

    return copy(
        hoursBilled = normalizedBilledHours,
        billableStatus = status,
        updatedAt = updatedAt
    )
}

