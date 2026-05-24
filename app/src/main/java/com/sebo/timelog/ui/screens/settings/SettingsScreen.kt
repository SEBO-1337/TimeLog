package com.sebo.timelog.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.sebo.timelog.BuildConfig
import com.sebo.timelog.data.remote.SyncStatus
import com.sebo.timelog.ui.components.TimeLogTopAppBar
import com.sebo.timelog.ui.theme.ThemePreference
import com.sebo.timelog.utils.appContainer
import java.text.DateFormat

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val syncStatus by context.appContainer.syncStatus.collectAsState()
    val currentUser = context.appContainer.authService?.currentUser()
    val userLabel = when {
        currentUser?.email?.isNullOrBlank() == false -> currentUser.email!!
        currentUser != null -> "UID: ${currentUser.uid.take(8)}..."
        else -> "Nicht angemeldet"
    }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val themePreference by context.appContainer.themeStore.themePreference.collectAsState(
        initial = ThemePreference.SYSTEM
    )
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            current = themePreference,
            onSelect = { selected ->
                coroutineScope.launch {
                    context.appContainer.themeStore.setThemePreference(selected)
                }
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.state.collectAsState()

    // Update-Dialog
    when (val state = updateState) {
        is UpdateUiState.UpdateAvailable -> {
            UpdateAvailableDialog(
                release = state.release,
                onDownload = {
                    val url = state.release.apkDownloadUrl ?: state.release.releasePage
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    updateViewModel.dismissResult()
                },
                onOpenReleasePage = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.release.releasePage)))
                    updateViewModel.dismissResult()
                },
                onDismiss = { updateViewModel.dismissResult() }
            )
        }
        is UpdateUiState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { updateViewModel.dismissResult() },
                icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                title = { Text("Kein Update verfügbar") },
                text = { Text("Du verwendest bereits die neueste Version (${BuildConfig.VERSION_NAME}).") },
                confirmButton = {
                    TextButton(onClick = { updateViewModel.dismissResult() }) {
                        Text("OK")
                    }
                }
            )
        }
        is UpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = { updateViewModel.dismissResult() },
                title = { Text("Fehler") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { updateViewModel.dismissResult() }) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }

    Scaffold(
        topBar = {
            TimeLogTopAppBar(title = "Einstellungen")
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Darstellung
            SettingsSection(title = "Darstellung")
            SettingsItem(
                icon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                title = "Design",
                subtitle = themePreference.label,
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider()

            // Benachrichtigungen
            SettingsSection(title = "Benachrichtigungen")
            SettingsItem(
                icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                title = "Benachrichtigungen",
                subtitle = "Timer- und Erinnerungsbenachrichtigungen",
                onClick = { /* TODO */ }
            )

            HorizontalDivider()

            // Daten
            SettingsSection(title = "Daten")
            SettingsItem(
                icon = {
                    val icon = when {
                        !syncStatus.configured -> Icons.Default.CloudOff
                        syncStatus.isSyncing -> Icons.Default.Sync
                        syncStatus.lastErrorAt != null -> Icons.Default.CloudOff
                        else -> Icons.Default.CloudDone
                    }
                    Icon(icon, contentDescription = null)
                },
                title = "Cloud-Sync (Firebase)",
                subtitle = syncStatusSubtitle(syncStatus, dateFormat),
                onClick = {
                    if (!syncStatus.configured) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Firebase ist nicht konfiguriert")
                        }
                    } else if (syncStatus.isSyncing) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Synchronisierung läuft bereits...")
                        }
                    } else {
                        context.appContainer.triggerManualSync()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cloud-Sync gestartet ☁️")
                        }
                    }
                }
            )

            SettingsItem(
                icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                title = "Datenmanagement",
                subtitle = "Backup & Export",
                onClick = { /* TODO: Export */ }
            )

            HorizontalDivider()

            // Über
            SettingsSection(title = "Über")
            SettingsItem(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = "TimeLog",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                onClick = { }
            )
            SettingsItem(
                icon = {
                    if (updateState is UpdateUiState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(4.dp)
                                .then(Modifier.height(24.dp)),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    }
                },
                title = "Nach Updates suchen",
                subtitle = when (updateState) {
                    is UpdateUiState.Checking -> "Wird geprüft..."
                    is UpdateUiState.UpdateAvailable -> "Update verfügbar!"
                    else -> "Auf Updates von GitHub prüfen"
                },
                onClick = { updateViewModel.checkForUpdates() }
            )

            // Banner wenn Update verfügbar
            if (updateState is UpdateUiState.UpdateAvailable) {
                val rel = (updateState as UpdateUiState.UpdateAvailable).release
                UpdateBanner(
                    newVersion = rel.version,
                    onDownload = {
                        val url = rel.apkDownloadUrl ?: rel.releasePage
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }

            HorizontalDivider()

            // Account
            SettingsSection(title = "Account")
            SettingsItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                title = "Angemeldet als",
                subtitle = userLabel,
                onClick = { }
            )

            SettingsItem(
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = "Abmelden",
                subtitle = "Aus deinem Konto abmelden",
                onClick = onLogout,
                titleColor = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UpdateBanner(newVersion: String, onDownload: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎉 Update verfügbar: v$newVersion",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Eine neue Version ist auf GitHub verfügbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDownload) {
                Text("Jetzt herunterladen")
            }
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    release: com.sebo.timelog.utils.ReleaseInfo,
    onDownload: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text("Update verfügbar: v${release.version}") },
        text = {
            Column {
                Text(
                    text = "Release-Notes:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = release.releaseNotes.take(400).let {
                        if (release.releaseNotes.length > 400) "$it…" else it
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Text(if (release.apkDownloadUrl != null) "APK herunterladen" else "Release öffnen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onOpenReleasePage) {
                Text("Release-Seite")
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    current: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
        title = { Text("Design") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemePreference.entries.forEach { pref ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = pref == current,
                                onClick = { onSelect(pref) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = pref == current,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text(
                            text = pref.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = { Text(title, color = titleColor) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

private fun syncStatusSubtitle(status: SyncStatus, dateFormat: DateFormat): String {
    if (!status.configured) {
        return "Firebase nicht konfiguriert"
    }
    if (status.isSyncing) {
        return "Synchronisiert gerade..."
    }
    if (status.lastErrorAt != null) {
        val time = dateFormat.format(status.lastErrorAt)
        val reason = status.lastErrorMessage ?: "Unbekannter Fehler"
        return "Fehler am $time ($reason)"
    }
    if (status.lastSuccessAt != null) {
        return "Zuletzt synchronisiert: ${dateFormat.format(status.lastSuccessAt)}"
    }
    return "Noch keine Synchronisierung"
}
