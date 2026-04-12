package com.sebo.timelog.ui.screens.timer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.timelog.ui.screens.timer.TimerUiState

@Composable
fun TimerControls(
    uiState: TimerUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (uiState) {
            is TimerUiState.Idle, is TimerUiState.Error -> {
                // Start-Button
                FloatingActionButton(
                    onClick = onStart,
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Timer starten",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            is TimerUiState.Running -> {
                // Verwerfen-Button
                FilledIconButton(
                    onClick = onDiscard,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Verwerfen")
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Pause-Button
                FloatingActionButton(
                    onClick = onPause,
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "Pausieren",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stop-Button
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stoppen")
                }
            }

            is TimerUiState.Paused -> {
                // Verwerfen-Button
                FilledIconButton(
                    onClick = onDiscard,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Verwerfen")
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Resume-Button
                FloatingActionButton(
                    onClick = onResume,
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Fortsetzen",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stop-Button
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stoppen & Speichern")
                }
            }
        }
    }
}

