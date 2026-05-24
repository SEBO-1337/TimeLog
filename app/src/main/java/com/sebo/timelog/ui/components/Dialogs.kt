package com.sebo.timelog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Löschen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String = "Abbrechen",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun BillingHoursDialog(
    title: String,
    message: String,
    initialHours: String = "",
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var hoursInput by remember(initialHours) { mutableStateOf(initialHours) }
    val parsedHours = hoursInput.replace(',', '.').toDoubleOrNull()
    val isValid = parsedHours != null && parsedHours > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(message)
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("Stunden") },
                    singleLine = true,
                    supportingText = {
                        Text("Beispiel: 2,5")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedHours?.let(onConfirm) },
                enabled = isValid
            ) {
                Text("Abrechnen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}


data class ManualWorkLogInput(
    val projectId: Long,
    val hoursWorked: Double,
    val date: Long,
    val description: String,
    val notes: String
)

/**
 * Dialog zum Abrechnen per Geldbetrag.
 * Der Benutzer gibt den ausgezahlten Betrag ein, die entsprechenden Stunden werden berechnet.
 * Wenn mehr Geld eingegeben wird als offene Stunden vorhanden, gehen die Stunden ins Minus.
 *
 * @param hourlyRate Stundensatz in €
 * @param pendingHours Noch offene Stunden (kann negativ sein)
 * @param onConfirm Callback mit dem eingegebenen Betrag in €
 */
@Composable
fun BillingAmountDialog(
    projectName: String,
    hourlyRate: Double,
    pendingHours: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    val parsedAmount = amountInput.replace(',', '.').toDoubleOrNull()
    val isValid = parsedAmount != null && parsedAmount > 0.0

    val hoursEquivalent = if (parsedAmount != null && hourlyRate > 0.0) parsedAmount / hourlyRate else null
    val openAmount = pendingHours * hourlyRate
    val isOverpayment = hoursEquivalent != null && hoursEquivalent > pendingHours + 0.0001

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abrechnen: $projectName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Offener Betrag: ${String.format("%.2f", openAmount)} €" +
                            " (${String.format("%.2f", pendingHours)} Std.)",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Ausgezahlter Betrag (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Beispiel: 150,00") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (hoursEquivalent != null) {
                    Text(
                        text = "= ${String.format("%.2f", hoursEquivalent)} Std. (bei ${String.format("%.2f", hourlyRate)} €/Std.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isOverpayment && hoursEquivalent != null) {
                    val excessHours = hoursEquivalent - pendingHours
                    Text(
                        text = "⚠ Betrag übersteigt die offenen Stunden um ${String.format("%.2f", excessHours)} Std. – " +
                                "die Stunden gehen ins Minus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedAmount?.let(onConfirm) },
                enabled = isValid
            ) {
                Text("Abrechnen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkLogDialog(
    projects: List<Project>,
    onConfirm: (ManualWorkLogInput) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var hoursInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(DateUtils.formatDate(System.currentTimeMillis())) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var notes by remember { mutableStateOf("") }

    val parsedHours = hoursInput.replace(',', '.').toDoubleOrNull()
    val parsedDate = runCatching {
        SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).parse(dateInput.trim())?.time
    }.getOrNull()
    val isValid = selectedProject != null && parsedHours != null && parsedHours > 0.0 && parsedDate != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag manuell hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = projectDropdownExpanded,
                    onExpandedChange = { projectDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "Projekt wählen",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Projekt") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = projectDropdownExpanded,
                        onDismissRequest = { projectDropdownExpanded = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = {
                                    selectedProject = project
                                    projectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Datum") },
                    singleLine = true,
                    supportingText = { Text("Format: TT.MM.JJJJ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("Stunden") },
                    singleLine = true,
                    supportingText = { Text("Beispiel: 2,5 · wird auf 0,25 h aufgerundet") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { newValue ->
                        val oldText = description.text
                        val newText = newValue.text
                        // Prüfen ob ein Zeilenumbruch hinzugefügt wurde
                        if (newText.length > oldText.length) {
                            val insertPos = newValue.selection.start
                            val inserted = newText.substring(oldText.length.coerceAtMost(insertPos - 1), insertPos)
                            if (inserted.contains("\n")) {
                                val withPrefix = newText.substring(0, insertPos) + "- " + newText.substring(insertPos)
                                val newCursor = insertPos + 2
                                description = TextFieldValue(withPrefix, TextRange(newCursor))
                                return@OutlinedTextField
                            }
                        }
                        description = newValue
                    },
                    label = { Text("Beschreibung (optional)") },
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            ManualWorkLogInput(
                                projectId = selectedProject!!.id,
                                hoursWorked = parsedHours!!,
                                date = parsedDate!!,
                                description = description.text,
                                notes = notes
                            )
                        )
                    }
                },
                enabled = isValid
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
