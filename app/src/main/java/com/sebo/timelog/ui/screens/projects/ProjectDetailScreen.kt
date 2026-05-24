package com.sebo.timelog.ui.screens.projects

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.timelog.ui.components.BillingAmountDialog
import com.sebo.timelog.ui.components.ConfirmDialog
import com.sebo.timelog.ui.components.LoadingIndicator
import com.sebo.timelog.ui.components.TimeLogDetailTopAppBar
import com.sebo.timelog.utils.TimeFormatter
import com.sebo.timelog.utils.toCurrencyString

@SuppressLint("DefaultLocale")
@Composable
fun ProjectDetailScreen(
    viewModel: ProjectsViewModel,
    projectId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val billingSummaries by viewModel.projectBillingSummaries.collectAsState()
    val project = projects.find { it.id == projectId }
    val billingSummary = billingSummaries[projectId]

    var showHoursBillingConfirm by remember { mutableStateOf(false) }
    var showAmountBillingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TimeLogDetailTopAppBar(
                title = project?.name ?: "Projekt",
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (project == null) {
                LoadingIndicator()
            } else {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (!project.description.isNullOrEmpty()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = "Status: ${project.status.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (project.hourlyRate > 0) {
                    Text(
                        text = String.format("Stundensatz: %.2f €", project.hourlyRate),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Abrechnung",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Einträge: ${billingSummary?.workLogCount ?: 0}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Gesamtstunden: ${TimeFormatter.formatHoursDecimal(billingSummary?.totalHours ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Abgerechnet: ${TimeFormatter.formatHoursDecimal(billingSummary?.billedHours ?: 0.0)}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        val pendingHours = billingSummary?.pendingHours ?: 0.0
                        val pendingColor = if (pendingHours < -0.0001)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary

                        Text(
                            text = if (pendingHours < -0.0001)
                                "Minusstunden: ${TimeFormatter.formatHoursDecimal(pendingHours)}"
                            else
                                "Offen: ${TimeFormatter.formatHoursDecimal(pendingHours)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = pendingColor
                        )

                        if (project.hourlyRate > 0) {
                            val openAmount = billingSummary?.openAmount ?: 0.0
                            Text(
                                text = if (openAmount < -0.0001)
                                    "Guthaben: ${(-openAmount).toCurrencyString()}"
                                else
                                    "Offener Betrag: ${openAmount.toCurrencyString()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = pendingColor
                            )
                        }
                    }
                }

                // Stunden-basiertes Abrechnen (nur wenn offene Stunden vorhanden)
                if (pendingHoursValue(billingSummary) > 0.0001) {
                    Button(
                        onClick = { showHoursBillingConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Offene Stunden abrechnen")
                    }
                }

                // Geld-basiertes Abrechnen (wenn Stundensatz hinterlegt und Einträge vorhanden)
                if (project.hourlyRate > 0 && (billingSummary?.totalHours ?: 0.0) > 0.0) {
                    Button(
                        onClick = { showAmountBillingDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (pendingHoursValue(billingSummary) > 0.0001) 8.dp else 16.dp
                            )
                    ) {
                        Text("Geldbetrag abrechnen")
                    }
                }
            }
        }
    }

    // Dialog: klassisches Stunden-Abrechnen
    if (showHoursBillingConfirm && project != null) {
        ConfirmDialog(
            title = "Projekt abrechnen",
            message = "Möchtest du alle offenen ${TimeFormatter.formatHoursDecimal(billingSummary?.pendingHours ?: 0.0)} für \"${project.name}\" als abgerechnet markieren?",
            confirmText = "Abrechnen",
            onConfirm = {
                viewModel.markProjectPendingAsBilled(project.id)
                showHoursBillingConfirm = false
            },
            onDismiss = { showHoursBillingConfirm = false }
        )
    }

    // Dialog: Geldbetrag abrechnen (erlaubt Minusstunden)
    if (showAmountBillingDialog && project != null) {
        BillingAmountDialog(
            projectName = project.name,
            hourlyRate = project.hourlyRate,
            pendingHours = billingSummary?.pendingHours ?: 0.0,
            onConfirm = { amount ->
                viewModel.billProjectByAmount(project.id, amount, project.hourlyRate)
                showAmountBillingDialog = false
            },
            onDismiss = { showAmountBillingDialog = false }
        )
    }
}

private fun pendingHoursValue(summary: ProjectBillingSummary?): Double =
    summary?.pendingHours ?: 0.0
