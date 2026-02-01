package com.hazron.sequencetimer.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.domain.RunningTimerState
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.domain.model.Timer
import com.hazron.sequencetimer.ui.theme.TimerGreen
import com.hazron.sequencetimer.ui.theme.TimerOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTimerClick: (Long) -> Unit,
    onAddTimer: () -> Unit,
    onEditTimer: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sequence Timer") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTimer) {
                Icon(Icons.Default.Add, contentDescription = "Add Timer")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.timers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No timers yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to create your first timer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.timers, key = { it.id }) { timer ->
                    TimerCard(
                        timer = timer,
                        runningState = uiState.runningStates[timer.id],
                        onClick = { onTimerClick(timer.id) },
                        onEdit = { onEditTimer(timer.id) },
                        onDelete = { viewModel.deleteTimer(timer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: Timer,
    runningState: RunningTimerState?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isRunning = runningState?.isRunning == true && runningState.isPaused == false
    val isPaused = runningState?.isPaused == true
    val displayTime = runningState?.remainingSeconds ?: timer.durationSeconds

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isRunning) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Running indicator
            if (isRunning || isPaused) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Running" else "Paused",
                    tint = if (isRunning) TimerGreen else TimerOrange,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDuration(displayTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunning) {
                            TimerGreen
                        } else if (isPaused) {
                            TimerOrange
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (isRunning) {
                        Text(
                            text = "Running",
                            style = MaterialTheme.typography.labelSmall,
                            color = TimerGreen
                        )
                    } else if (isPaused) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = TimerOrange
                        )
                    }

                    Icon(
                        imageVector = when (timer.notificationType) {
                            NotificationType.SILENT -> Icons.Default.NotificationsOff
                            NotificationType.SOUND -> Icons.Default.Notifications
                            NotificationType.ALARM -> Icons.Default.NotificationsActive
                        },
                        contentDescription = timer.notificationType.name,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Timer") },
            text = { Text("Are you sure you want to delete \"${timer.label}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%d:%02d", minutes, secs)
    }
}
