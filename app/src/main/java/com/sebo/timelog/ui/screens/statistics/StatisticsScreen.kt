package com.sebo.timelog.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sebo.timelog.ui.components.EmptyState
import com.sebo.timelog.ui.components.LoadingIndicator
import com.sebo.timelog.ui.components.TimeLogTopAppBar
import com.sebo.timelog.utils.TimeFormatter
import com.sebo.timelog.utils.toComposeColor
import com.sebo.timelog.utils.toCurrencyString

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TimeLogTopAppBar(title = "Statistiken")
        },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else if (uiState.projectStats.isEmpty()) {
            EmptyState(
                message = "Noch keine Statistiken.\nStarte den Timer um Daten zu sammeln!",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gesamt-Übersicht
                item {
                    SummaryCard(
                        totalHours = uiState.totalHours,
                        totalEntries = uiState.totalEntries,
                        totalRevenue = uiState.projectStats.sumOf { it.estimatedRevenue }
                    )
                }

                item {
                    Text(
                        text = "Pro Projekt",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Projekt-Stats
                items(uiState.projectStats) { stat ->
                    ProjectStatCard(
                        stat = stat,
                        maxHours = uiState.projectStats.maxOf { it.totalHours }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalHours: Double,
    totalEntries: Int,
    totalRevenue: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Gesamt-Übersicht",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatValue(label = "Stunden", value = TimeFormatter.formatHoursShort(totalHours))
                StatValue(label = "Einträge", value = totalEntries.toString())
                StatValue(label = "Umsatz", value = totalRevenue.toCurrencyString())
            }
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProjectStatCard(
    stat: ProjectStatItem,
    maxHours: Double
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(stat.project.color.toComposeColor())
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stat.project.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = TimeFormatter.formatHoursShort(stat.totalHours),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fortschrittsbalken
            if (maxHours > 0) {
                LinearProgressIndicator(
                    progress = { (stat.totalHours / maxHours).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = stat.project.color.toComposeColor()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stat.entryCount} Einträge",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (stat.estimatedRevenue > 0) {
                    Text(
                        text = stat.estimatedRevenue.toCurrencyString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

