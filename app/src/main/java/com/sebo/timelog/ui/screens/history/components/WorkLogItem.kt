package com.sebo.timelog.ui.screens.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sebo.timelog.data.local.entities.BillableStatus
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.WorkLog
import com.sebo.timelog.utils.DateUtils
import com.sebo.timelog.utils.TimeFormatter
import com.sebo.timelog.utils.effectiveBilledHours
import com.sebo.timelog.utils.pendingHours
import com.sebo.timelog.utils.resolvedBillableStatus
import com.sebo.timelog.utils.toComposeColor

@Composable
fun WorkLogItem(
    workLog: WorkLog,
    project: Project?,
    onPartialBilling: () -> Unit,
    onMarkAsBilled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val billedHours = workLog.effectiveBilledHours()
    val pendingHours = workLog.pendingHours()
    val status = workLog.resolvedBillableStatus()
    val statusText = when (status) {
        BillableStatus.UNBILLED -> "Offen"
        BillableStatus.PARTIAL -> "Teilweise abgerechnet"
        BillableStatus.BILLED -> "Abgerechnet"
    }
    val statusColor = when (status) {
        BillableStatus.UNBILLED -> MaterialTheme.colorScheme.tertiary
        BillableStatus.PARTIAL -> MaterialTheme.colorScheme.primary
        BillableStatus.BILLED -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Projekt-Farbe
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(project?.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project?.name ?: "Unbekanntes Projekt",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (workLog.description.isNotEmpty()) {
                        Text(
                            text = workLog.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                    Row {
                        Text(
                            text = DateUtils.formatDate(workLog.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (workLog.startTime != null && workLog.endTime != null) {
                            Text(
                                text = " • ${DateUtils.formatTime(workLog.startTime)} - ${DateUtils.formatTime(workLog.endTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Stunden-Anzeige
                Text(
                    text = TimeFormatter.formatHoursShort(workLog.hoursWorked),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Abgerechnet: ${TimeFormatter.formatHoursShort(billedHours)} • Offen: ${TimeFormatter.formatHoursShort(pendingHours)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pendingHours > 0.0001) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPartialBilling,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (billedHours > 0.0) "Weitere Stunden" else "Teilabrechnung")
                    }
                    Button(
                        onClick = onMarkAsBilled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Komplett abrechnen")
                    }
                }
            }
        }
    }
}

