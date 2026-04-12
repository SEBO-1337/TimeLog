package com.sebo.timelog.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.timelog.ui.components.TimeLogTopAppBar

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TimeLogTopAppBar(title = "Einstellungen")
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Darstellung
            SettingsSection(title = "Darstellung")
            SettingsItem(
                icon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                title = "Design",
                subtitle = "Systemstandard",
                onClick = { /* TODO: Dark Mode */ }
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
                subtitle = "Version 1.0",
                onClick = { }
            )
        }
    }
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
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

