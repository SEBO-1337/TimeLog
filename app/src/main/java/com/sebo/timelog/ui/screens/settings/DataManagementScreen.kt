package com.sebo.timelog.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackupTable
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebo.timelog.ui.components.TimeLogDetailTopAppBar
import com.sebo.timelog.utils.appContainer
import kotlinx.coroutines.launch

@Composable
fun DataManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context       = LocalContext.current
    val viewModel: DataManagementViewModel = viewModel(
        factory = DataManagementViewModel.factory(
            context.appContainer.projectRepository,
            context.appContainer.workLogRepository
        )
    )

    val state          by viewModel.state.collectAsState()
    val snackbarHost   = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Pending content to write after the file-picker returns a URI
    var pendingContent by remember { mutableStateOf<String?>(null) }

    // ── File chooser launchers ─────────────────────────────────────────────────

    val createJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingContent != null) {
            viewModel.writeContentToUri(context, uri, pendingContent!!)
            pendingContent = null
        } else {
            pendingContent = null
            viewModel.dismissState()
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null && pendingContent != null) {
            viewModel.writeContentToUri(context, uri, pendingContent!!)
            pendingContent = null
        } else {
            pendingContent = null
            viewModel.dismissState()
        }
    }

    val openJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.loadRestoreFile(context, uri)
        else             viewModel.dismissState()
    }

    // ── React to state changes ─────────────────────────────────────────────────

    LaunchedEffect(state) {
        when (val s = state) {
            is DataManagementState.BackupReady -> {
                pendingContent = s.json
                createJsonLauncher.launch(s.fileName)
            }
            is DataManagementState.CsvReady -> {
                pendingContent = s.csv
                createCsvLauncher.launch(s.fileName)
            }
            is DataManagementState.Success -> {
                coroutineScope.launch { snackbarHost.showSnackbar(s.message) }
                viewModel.dismissState()
            }
            is DataManagementState.Error -> {
                coroutineScope.launch { snackbarHost.showSnackbar(s.message) }
                viewModel.dismissState()
            }
            else -> {}
        }
    }

    // ── Restore confirm dialog ─────────────────────────────────────────────────

    if (state is DataManagementState.RestoreConfirm) {
        val s = state as DataManagementState.RestoreConfirm
        AlertDialog(
            onDismissRequest = { viewModel.dismissState() },
            icon    = { Icon(Icons.Default.FileUpload, contentDescription = null) },
            title   = { Text("Backup wiederherstellen?") },
            text    = {
                Column {
                    Text(
                        "Das Backup enthält:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("• ${s.projectCount} Projekte")
                    Text("• ${s.workLogCount} Arbeitslogs")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Die Daten werden zu den bestehenden Einträgen hinzugefügt. " +
                        "Bereits vorhandene Einträge werden nicht gelöscht.",
                        style = MaterialTheme.typography.bodySmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.performRestore(s.json) }) {
                    Text("Wiederherstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissState() }) { Text("Abbrechen") }
            }
        )
    }

    // ── Main UI ────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TimeLogDetailTopAppBar(
                title           = "Datenmanagement",
                onNavigateBack  = onNavigateBack
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHost) { data -> Snackbar(snackbarData = data) }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val isLoading = state is DataManagementState.Loading

            // ── Info Banner ────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = "Hier kannst du deine Daten als JSON-Backup sichern, " +
                                "als CSV-Tabelle exportieren oder ein bestehendes Backup wiederherstellen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // ── Backup (JSON) ──────────────────────────────────────────────────
            SectionHeader(title = "Backup")
            DataCard(
                icon     = { Icon(Icons.Default.BackupTable, contentDescription = null) },
                title    = "JSON-Backup erstellen",
                subtitle = "Alle Projekte und Arbeitslogs in einer Datei sichern",
                actionLabel = "Backup erstellen",
                isLoading   = isLoading,
                onClick     = { viewModel.prepareBackup() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── CSV Export ─────────────────────────────────────────────────────
            SectionHeader(title = "Export")
            DataCard(
                icon     = { Icon(Icons.Default.TableChart, contentDescription = null) },
                title    = "CSV-Tabelle exportieren",
                subtitle = "Arbeitslogs als Tabelle für Excel oder Calc exportieren",
                actionLabel = "CSV exportieren",
                isLoading   = isLoading,
                onClick     = { viewModel.prepareCsvExport() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Restore ────────────────────────────────────────────────────────
            SectionHeader(title = "Wiederherstellen")
            DataCard(
                icon     = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                title    = "Backup importieren",
                subtitle = "JSON-Backup laden und Daten wiederherstellen",
                actionLabel  = "Datei auswählen",
                isLoading    = isLoading,
                onClick      = { openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                actionColors = ButtonDefaults.outlinedButtonColors()
            )

            // ── Download own backup for reference ────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader(title = "Teilen")
            DataCard(
                icon        = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                title       = "Backup teilen",
                subtitle    = "JSON-Backup über die Teilen-Funktion weitergeben",
                actionLabel = "Teilen",
                isLoading   = isLoading,
                onClick     = { viewModel.prepareBackup() }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Private helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun DataCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actionLabel: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    actionColors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            if (actionColors == ButtonDefaults.buttonColors()) {
                Button(
                    onClick  = onClick,
                    enabled  = !isLoading,
                    colors   = actionColors
                ) { Text(actionLabel) }
            } else {
                OutlinedButton(
                    onClick  = onClick,
                    enabled  = !isLoading
                ) { Text(actionLabel) }
            }
        }
    }
}

