package com.hazron.sequencetimer.ui.screens.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.ui.theme.TimerGreen
import com.hazron.sequencetimer.ui.theme.TimerOrange
import com.hazron.sequencetimer.ui.theme.TimerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.timer?.label ?: "Timer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
        } else if (uiState.timer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Timer not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular progress indicator with time
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    CircularProgressIndicator(
                        progress = uiState.progress,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 12.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = when {
                            uiState.isComplete -> TimerRed
                            uiState.progress < 0.25f -> TimerOrange
                            else -> TimerGreen
                        }
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(uiState.remainingSeconds),
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.isComplete) {
                            Text(
                                text = "Complete!",
                                style = MaterialTheme.typography.titleMedium,
                                color = TimerRed
                            )
                        } else if (uiState.isPaused) {
                            Text(
                                text = "Paused",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    FilledTonalIconButton(
                        onClick = viewModel::resetTimer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause button
                    FilledIconButton(
                        onClick = {
                            when {
                                uiState.isComplete -> viewModel.resetTimer()
                                !uiState.isRunning -> viewModel.startTimer()
                                uiState.isPaused -> viewModel.resumeTimer()
                                else -> viewModel.pauseTimer()
                            }
                        },
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                uiState.isComplete -> Icons.Default.Refresh
                                !uiState.isRunning || uiState.isPaused -> Icons.Default.PlayArrow
                                else -> Icons.Default.Pause
                            },
                            contentDescription = when {
                                uiState.isComplete -> "Restart"
                                !uiState.isRunning || uiState.isPaused -> "Start"
                                else -> "Pause"
                            },
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Spacer to balance layout
                    Spacer(modifier = Modifier.size(64.dp))
                }
            }
        }
    }
}

private fun formatTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}
